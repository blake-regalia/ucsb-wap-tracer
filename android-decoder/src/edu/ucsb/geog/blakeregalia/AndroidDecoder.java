package edu.ucsb.geog.blakeregalia;

import java.io.*;
import java.util.Date;

/**
 * S: Start time
 * L: Start latitude / longitude
 * N: # of WAPS
 * T: Time offset
 * A: Latitude
 * G: Longitude
 * X: Location Accuracy in Meters
 * M: MAC address
 * R: RSSI signal strength
 * I: SSID name of network key
 * k: key of SSID
 * s: length of SSID string
 * D: SSID string
 * 
 * file header
 * SSSSSSSSLLLLLLLLLLLLLLLL
 * --long----long----long--
 * 
 * line header
 * NTTAAAAGGGGX
 * b-s-int-intb
 * 
 * 		line data
 * 		MMMMMMRI
 * 		--mac-bb
 * 
 * 0
 * -
 * 
 * ksD+
 * bb-S
 * 
 * @author Blake
 *
 */

public class AndroidDecoder {

	private final static long FILE_HEADER_LENGTH = 24;
	private final static long DATA_HEADER_LENGTH = 12;
	private final static long DATA_ENTRY_LENGTH = 8;
	private final static long TERMINATING_FIELD_LENGTH = 1;
	private final static long STRING_MAPPING_LENGTH = 2;
	private final static long MINIMUM_FILE_SIZE = FILE_HEADER_LENGTH + DATA_HEADER_LENGTH + DATA_ENTRY_LENGTH;
	
	private final static float COORDINATE_FACTOR = 0.001f; 
	private final static float COORDINATE_PRECISION = 1000000; 
	private final static float COORDINATE_PRECISION_INV = 1 / COORDINATE_PRECISION;

	/* precision of the recorded time-stamp values in milliseconds, value of 100 yields 0.1 second resolution */
	private static final int TIMESTAMP_PRECISION_MS = 100;
	private static final double TIMESTAMP_REDUCTION_FACTOR = 1.0 / TIMESTAMP_PRECISION_MS;

	public static void main(String[] args) {		
		new AndroidDecoder(args);
	}

	public AndroidDecoder(String[] args) {
		
		if(args.length == 0) {
			System.err.println("Usage: android-decoder [-json] [-pretty] FILE");
			System.exit(1);
		}

		int i = args.length;
		String file_name = args[--i];
		
		boolean output_json = false;
		boolean pretty_print = false;
		while(i-- != 0) {
			switch(args[i].toLowerCase()) {
			case "-json":
				output_json = true;
				break;
			case "-pretty":
				pretty_print = true;
				break;
			}
		}

		File dir = new File("./data");
		String file_path = "";

		try {
			file_path = dir.getCanonicalPath()+File.separatorChar+file_name;
		} catch (IOException e) {
			System.out.println("Could not find directory \"data\" located at "+System.getProperty("user.dir"));
		}
		
		/* open the file reader for reading and decoding bytes from the binary file */
		_FileReader fr = new _FileReader(file_path);

		/* get the file size */
		long file_size = fr.size();

		/* check that it meets the minimum file size requirement */
		if(file_size < MINIMUM_FILE_SIZE) {
			System.err.println("The trace file is incomplete, it is not large enough to contain useable information.");
			return;
		}

		/* read start time - SSSSSSSS [8 bytes] */
		long start_time = fr.read_long();

		/* read start latitude/longitude - LLLLLLLLLLLLLLLL [16 bytes] */
		float start_latitude = ((float) fr.read_long()) * COORDINATE_PRECISION_INV;
		float start_longitude = ((float) fr.read_long()) * COORDINATE_PRECISION_INV;
		
		/* prepare the output format */
		DefaultOutput output;
		if(output_json) {
			output = new JSON_Output(pretty_print);
		}
		else {
			output = new DefaultOutput();
		}
		
		/* initialize the outputter */
		output.initialize(file_size, start_time, start_latitude, start_longitude);

		int b = 0;
		while((b=fr.read()) != -1) {
			
			int offset = fr.bytes_read;
			
			//byte wap_length = fr.read_byte();
			byte wap_length = (byte) (b & 0xff);

			if(wap_length == 0) break;

			int event_length = (int) (DATA_HEADER_LENGTH + wap_length*DATA_ENTRY_LENGTH) - 1;
			if(fr.bytes_read + event_length > file_size) {
				System.err.println("The trace file is incomplete, expecting more data. "+fr.bytes_read+" bytes read, "+event_length+" more bytes expected");
				return;
			}

			/* read time offset */
			char c = fr.read_char();
			long timestamp = start_time + c*TIMESTAMP_PRECISION_MS;
			
			/* read latitude */
			float latitude = start_latitude + (((float) fr.read_int()) * COORDINATE_PRECISION_INV);

			/* read longitude */
			float longitude = start_longitude + (((float) fr.read_int()) * COORDINATE_PRECISION_INV);
			
			/* read location accuracy */
			int accuracy = fr.read();

			WAP_Event event = new WAP_Event(timestamp, latitude, longitude, accuracy);

			/* read wap entries */
			byte n = wap_length;
			while(n-- != 0) {
				
				offset = fr.bytes_read;
				
				/* read hw addr - MMMMMM [6 bytes] */
				byte[] byte_hw_addr = new byte[6];
				fr.read(byte_hw_addr);
				String mac_addr = decode_hw_addr(byte_hw_addr);

				/* read rssi - R [1 byte] */
				int rssi = ((int) fr.read_byte()) & 0xff;
				int signal = (int) (((float) rssi) / 2.55f);

				/* read String ID - I [1 byte] */
				int ssid = fr.read_byte();
				
				event.add_WAP(new WAP(mac_addr, rssi, ssid));
			}

			output.addEvent(event);
		}
		
		int ssid_key = fr.read_int();
		while(true) {
			String ssid = fr.read_string();
			
			output.setSSID(ssid_key, ssid);
			
			if(fr.bytes_read == file_size) {
				break;
			}
			ssid_key = fr.read_int();
		}

		fr.close();
		
		System.out.println(output.dump());
	}
	

    public String decode_hw_addr(byte[] b) {
    	StringBuilder addr = new StringBuilder();
    	String hex;
    	
    	hex = Integer.toHexString(b[0] & 0xff);
    	if(hex.length() == 1) {
    		addr.append('0');
    	}
    	addr.append(hex);
    	
    	addr.append(':');
    	
    	hex = Integer.toHexString(b[1] & 0xff);
    	if(hex.length() == 1) {
    		addr.append('0');
    	}
    	addr.append(hex);

    	addr.append(':');
    	
    	hex = Integer.toHexString(b[2] & 0xff);
    	if(hex.length() == 1) {
    		addr.append('0');
    	}
    	addr.append(hex);

    	addr.append(':');
    	
    	hex = Integer.toHexString(b[3] & 0xff);
    	if(hex.length() == 1) {
    		addr.append('0');
    	}
    	addr.append(hex);

    	addr.append(':');
    	
    	hex = Integer.toHexString(b[4] & 0xff);
    	if(hex.length() == 1) {
    		addr.append('0');
    	}
    	addr.append(hex);

    	addr.append(':');
    	
    	hex = Integer.toHexString(b[5] & 0xff);
    	if(hex.length() == 1) {
    		addr.append('0');
    	}
    	addr.append(hex);

    	return addr.toString();
    }
}