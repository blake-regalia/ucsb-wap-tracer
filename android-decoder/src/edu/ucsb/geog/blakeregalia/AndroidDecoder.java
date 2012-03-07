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
	
	private final static double COORDINATE_FACTOR = 0.001; 
	private final static double COORDINATE_PRECISION = 1000000; 
	private final static double COORDINATE_PRECISION_INV = 1 / COORDINATE_PRECISION;

	/* precision of the recorded time-stamp values in milliseconds, value of 100 yields 0.1 second resolution */
	private static final int TIMESTAMP_PRECISION_MS = 100;
	private static final double TIMESTAMP_REDUCTION_FACTOR = 1.0 / TIMESTAMP_PRECISION_MS;

	public static void main(String[] args) {		
		new AndroidDecoder(args);
	}

	public AndroidDecoder(String[] args) {

		int i = args.length;
		String file_name = args[--i];

		File dir = new File("./data");
		String file_path = "";

		try {
			file_path = dir.getCanonicalPath()+File.separatorChar+file_name;
			System.out.println("Looking in path "+dir.getCanonicalPath()+" for "+file_name);
		} catch (IOException e) {
			System.out.println("Could not find directory \"data\" located at "+System.getProperty("user.dir"));
		}

		_FileReader fr = new _FileReader(file_path);

		long file_size = fr.size();
		System.out.println("Sucessfully opened \""+file_name+"\" for reading. File is "+(file_size)+"b");

		if(file_size < MINIMUM_FILE_SIZE) {
			System.err.println("The trace file is incomplete, it is not large enough to contain useable information.");
			return;
		}
		
		System.out.println("----------------------------------------------");

		/* read start time - SSSSSSSS [8 bytes] */
		long start_time = fr.read_long();

		/* read start latitude/longitude - LLLLLLLLLLLLLLLL [16 bytes] */
		double start_latitude = ((double) fr.read_long()) * COORDINATE_PRECISION_INV;
		double start_longitude = ((double) fr.read_long()) * COORDINATE_PRECISION_INV;
		
		System.out.println("Start Time: "+(new Date(start_time)).toString());
		System.out.println("South West Corner: "+start_latitude+", "+start_longitude);

		int b = 0;
		while((b=fr.read()) != -1) {
			System.out.println("");
			
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
			double latitude = start_latitude + (((double) fr.read_int()) * COORDINATE_PRECISION_INV);

			/* read longitude */
			double longitude = start_longitude + (((double) fr.read_int()) * COORDINATE_PRECISION_INV);
			
			/* read location accuracy */
			int accuracy = fr.read();

			System.out.println(""+(new Date(timestamp)).toString()+": "+((int)wap_length)+" WAP(s); "
					+ latitude+", "+longitude+" :"+accuracy+"m");

			/* read wap entries */
			byte n = wap_length;
			while(n-- != 0) {
				
				offset = fr.bytes_read;
				
				/* read hw addr - MMMMMM [6 bytes] */
				byte[] byte_hw_addr = new byte[6];
				fr.read(byte_hw_addr);

				/* read rssi - R [1 byte] */
				int rssi = ((int) fr.read_byte()) & 0xff;
				int signal = (int) (((float) rssi) / 2.55f);

				/* read String ID - I [1 byte] */
				int ssid = fr.read_byte();

				System.out.print("\t");
				System.out.print(signal+"%: ");
				System.out.print(decode_hw_addr(byte_hw_addr));
				System.out.print("\t"+ssid);
				System.out.println("");
			}
		}

		System.out.println("=========================");
		
		int ssid_key = fr.read_int();
		while(true) {
			String ssid = fr.read_string();
			System.out.println(ssid_key+": "+ssid);
			if(fr.bytes_read == file_size) {
				break;
			}
			ssid_key = fr.read_int();
		}

		fr.close();
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
