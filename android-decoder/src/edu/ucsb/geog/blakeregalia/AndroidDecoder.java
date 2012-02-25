package edu.ucsb.geog.blakeregalia;

import java.io.*;
import java.util.Date;

/**
 * S: Start time
 * W: # of WAPS
 * T: Time
 * A: Latitude
 * L: Longitude
 * M: MAC address
 * r: rssi signal strength
 * 
 * SSSSSSSSWTTAAAALLLLMMMMMMrMMMMMMr
 * --long--b-s-int-int-
 * 
 * @author Blake
 *
 */

public class AndroidDecoder {

	private final static long FILE_HEADER_LENGTH = 8;
	private final static long DATA_HEADER_LENGTH = 11;
	private final static long DATA_ENTRY_LENGTH = 7;
	private final static long MINIMUM_FILE_SIZE = FILE_HEADER_LENGTH + DATA_HEADER_LENGTH + DATA_ENTRY_LENGTH;

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

		/* read start time */
		long start_time = fr.read_long();

		long bytes_read = FILE_HEADER_LENGTH;

		char mode = 0;
		int b = 0;
		while((b=fr.read()) != -1) {
			char wap_length = (char) (b & 0xff);

			int event_length = (int) (DATA_HEADER_LENGTH + wap_length*DATA_ENTRY_LENGTH); 
			if(bytes_read + event_length > file_size) {
				System.err.println("The trace file is incomplete, expecting more data. "+bytes_read+" bytes read, "+event_length+" more bytes expected");
				return;
			}

			/* read time offset */
			char c = fr.read_char();
			long timestamp = start_time + c;
			System.out.println((new Date(timestamp*100)).toString()+": "+((int)wap_length)+" WAP(s); ");

			/* read latitude */
			fr.read_int();

			/* read longitude */
			fr.read_int();

			/* read wap entries */
			char n = wap_length;
			while(n-- != 0) {
				byte[] byte_hw_addr = new byte[6];
				fr.read(byte_hw_addr);
				System.out.print("\t");
				int rssi = fr.read() & 0xff;
				int signal = (int) (((float) rssi) / 2.55f);
				System.out.print(signal+"%  ");
				System.out.print(decode_hw_addr(byte_hw_addr));
				System.out.print("\n");
			}
			
			bytes_read += event_length;
		}

		fr.close();
	}
	

    public String decode_hw_addr(byte[] b) {
    	return Integer.toHexString(b[0] & 0xff)  + ":"
    			+ Integer.toHexString(b[1] & 0xff) + ":"
    			+ Integer.toHexString(b[2] & 0xff) + ":"
    			+ Integer.toHexString(b[3] & 0xff) + ":"
    			+ Integer.toHexString(b[4] & 0xff) + ":"
    			+ Integer.toHexString(b[5] & 0xff);
    }
}
