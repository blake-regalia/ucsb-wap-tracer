package edu.ucsb.geog.blakeregalia;

import java.io.*;
import java.util.Date;

/**
 * V: Version
 * Z: Android Id -string
 * S: Start time
 * L: Reference latitude / longitude
 * 
 * N: # of WAPS
 * T: Time offset
 * P: GPS Stale time
 * A: Latitude
 * G: Longitude
 * W: Altitude in meters
 * X: Location Accuracy in Meters
 * 
 * M: MAC address
 * F: Frequency of the wap
 * R: RSSI signal strength
 * I: SSID name of network key
 * 
 * k: key of SSID
 * s: length of SSID string
 * D: SSID string
 * 
 * file header
 * VVVVZz*SSSSSSSSLLLLLLLLLLLLLLLL
 * -int-s-long----long----long--
 * 
 * line header
 * NTT PAAAAGGGGWX
 * b-s b-int-intb
 * 
 * 		line data
 * 		MMMMMMFFRI
 * 		--mac-bbbb
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
	private static long DATA_HEADER_LENGTH = 12;
	private static long DATA_ENTRY_LENGTH = 8;
	
	private final static long TERMINATING_FIELD_LENGTH = 1;
	private final static long STRING_MAPPING_LENGTH = 2;
	private final static long MINIMUM_FILE_SIZE = FILE_HEADER_LENGTH + DATA_HEADER_LENGTH + DATA_ENTRY_LENGTH;
	
	private final static float COORDINATE_FACTOR = 0.001f; 
	private final static float COORDINATE_PRECISION = 1000000; 
	private final static float COORDINATE_PRECISION_INV = 1 / COORDINATE_PRECISION;
	
	private final static int WIFI_NUM_LEVELS = 45;
	private final static double WIFI_SIGNAL = 255.0 / WIFI_NUM_LEVELS;
	private final static double WIFI_SIGNAL_INVERT = WIFI_NUM_LEVELS / 255.0;

	/* precision of the recorded time-stamp values in milliseconds, value of 100 yields 0.1 second resolution */
	private static final int TIMESTAMP_PRECISION_MS = 100;
	private static final double TIMESTAMP_REDUCTION_FACTOR = 1.0 / TIMESTAMP_PRECISION_MS;
	
	private static final String DEFAULT_SQL_TABLE = "wireless_access_point_events";

	public static void main(String[] args) {		
		new AndroidDecoder(args);
	}

	public AndroidDecoder(String[] args) {
		
		if(args.length == 0) {
			System.err.println("Usage: android-decoder [-json|-sql [table=TABLE_NAME]]  [-pretty|-debug] FILE");
			System.exit(1);
		}

		int i = args.length;
		String file_name = args[--i];
		
		String output_type = "";
		String sql_table = DEFAULT_SQL_TABLE;
		boolean pretty_print = false;
		boolean debug = false;
		while(i-- != 0) {
			String arg_str = args[i].toLowerCase(); 
			switch(arg_str) {
			case "-json":
				output_type = "json";
				break;
			case "-sql":
				output_type = "sql";
				break;
			case "-pretty":
				pretty_print = true;
				break;
			case "-debug":
				debug = true;
				break;
			default:
				int delim_equals = arg_str.indexOf('=');
				if(delim_equals != -1) {
					String value = arg_str.substring(delim_equals+1);
					switch(arg_str.substring(0, delim_equals)) {
					case "table":
						sql_table = value;
						break;
					}
				}
				break;
			}
		}

		File dir = new File("./");
		String file_path = "";

		try {
			file_path = dir.getCanonicalPath()+File.separatorChar+file_name;
		} catch (IOException e) {
			System.err.println("Could not find file \""+file_name+"\" in "+System.getProperty("user.dir"));
		}
		
		file_path = file_name;
		
		/* open the file reader for reading and decoding bytes from the binary file */
		_FileReader fr = new _FileReader(file_path);

		/* get the file size */
		long file_size = fr.size();

		/* check that it meets the minimum file size requirement */
		if(file_size < MINIMUM_FILE_SIZE) {
			System.err.println("The trace file is incomplete, it is not large enough to contain useable information.");
			return;
		}
		
		/* read version # */
		int version = fr.read_int();
		
		// changelog
		// v3: +1 byte in data header (gps stale time)
		if(version >= 3) {
			DATA_HEADER_LENGTH += 1;
		}
		// v4: +1 byte in data header (gps altitude)
		if(version >= 4) {
			DATA_HEADER_LENGTH += 1;
		}
		// v4: +2 bytes in data entry (frequency)
		if(version >= 6) {
			DATA_ENTRY_LENGTH += 2;
		}
		
		String uid = "";
		if(version < 6) {
			/* fetch unique id */
			long user = fr.read_long();
			uid = user+"";
		}
		else {
			uid = fr.read_string();
		}

		/* read start time - SSSSSSSS [8 bytes] */
		long start_time = fr.read_long();

		/* read start latitude/longitude - LLLLLLLLLLLLLLLL [16 bytes] */
		float start_latitude = ((float) fr.read_long()) * COORDINATE_PRECISION_INV;
		float start_longitude = ((float) fr.read_long()) * COORDINATE_PRECISION_INV;
		
		/* prepare the output format */
		DefaultOutput output;
		if(output_type.equals("json")) {
			output = new JSON_Output(pretty_print);
		}
		else if(output_type.equals("sql")) {
			output = new SQL_Output(pretty_print, sql_table);
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
			
			if(debug) System.out.println("@"+fr.bytes_read+"  ------- "+wap_length+" waps....");

			if(wap_length == 0) break;

			int event_length = (int) (DATA_HEADER_LENGTH + wap_length*DATA_ENTRY_LENGTH) - 1;
			if(fr.bytes_read + event_length > file_size) {
				System.err.println("The trace file is incomplete, expecting more data. "+fr.bytes_read+" bytes read, "+event_length+" more bytes expected");
				return;
			}

			/* read time offset */
			char c = fr.read_char();
			long timestamp = start_time + c*TIMESTAMP_PRECISION_MS;
			
			/* V3+: read gps stale time */
			float stale_time = 0.f;
			if(version >= 3) {
				int stale_time_int = (int) (fr.read_byte() & 0xff);
				stale_time = (stale_time_int) / 10.f;
			}
			
			/* read latitude */
			float latitude = start_latitude + (((float) fr.read_int()) * COORDINATE_PRECISION_INV);

			/* read longitude */
			float longitude = start_longitude + (((float) fr.read_int()) * COORDINATE_PRECISION_INV);
			
			/* V4+: read gps altitude */
			int altitude = 0;
			if(version >= 4) {
				altitude = (int) (fr.read_byte() & 0xff);
			}
			
			/* read location accuracy */
			int accuracy = fr.read();

			/* setup this event */
			WAP_Event event = new WAP_Event(timestamp, latitude, longitude, altitude, accuracy, stale_time);

			/* read wap entries */
			byte n = wap_length;
			
			while(n-- != 0) {
				
				offset = fr.bytes_read;
				
				/* read hw addr - MMMMMM [6 bytes] */
				byte[] byte_hw_addr = new byte[6];
				fr.read(byte_hw_addr);
				String mac_addr = decode_hw_addr(byte_hw_addr);
				
				if(debug) System.out.println("@"+fr.bytes_read+"  "+mac_addr);
				
				/* v6: read freqeuncy */
				int frequency = 0;
				if(version >= 6) {
					frequency = ((int) fr.read_char()) & 0xffff;
				}

				/* read rssi - R [1 byte] */
				int rssi = ((int) fr.read_byte()) & 0xff;
//				int signal = (int) (((float) rssi) / 2.55f);
				
				if(version < 2) {
					rssi = (int) Math.round((Math.round(rssi*WIFI_SIGNAL_INVERT)+1)*WIFI_SIGNAL);
				}

				/* read String ID - I [1 byte] */
				int ssid = fr.read_byte();
				
				event.add_WAP(new WAP(mac_addr, rssi, ssid, frequency));
			}

			output.addEvent(event);
		}
		
		
		int ssid_key = fr.read_int();
		while(true) {
			if(debug) System.out.print("@"+fr.bytes_read+"  "+ssid_key+":");
			String ssid = fr.read_string();
			if(debug) System.out.println(ssid);
			
			output.setSSID(ssid_key, ssid);
			
			if(fr.bytes_read == file_size) {
				break;
			}
			ssid_key = fr.read_int();
		}

		fr.close();
		
		if(!debug) System.out.print(output.dump());
		else System.out.println("program finished");
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
