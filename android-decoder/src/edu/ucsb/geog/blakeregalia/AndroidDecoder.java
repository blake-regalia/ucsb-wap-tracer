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
	
	private static final String BINARY_FILE_EXTENSION = ".bin";
	private static final int LENGTH_STRING_BINARY_FILE_EXTENSION = BINARY_FILE_EXTENSION.length();

	private static final String DEFAULT_OUTPUT_TYPE = "text";
	private static final String DEFAULT_SQL_TABLE = "traces";

	public static void main(String[] args) {		
		new AndroidDecoder(args);
	}

	public AndroidDecoder(String[] args) {
		
		// no-args print out
		if(args.length == 0) {
			System.err.println("Usage: android-decoder [--output=text|sql|json] [--sql-table=TABLE_NAME] [--pretty] [--debug] FILE_PREFIX");
			System.exit(1);
		}

		// last argument = file name
		int i = args.length;
		String fileName = args[--i];
		
		// parameters
		String outputType = DEFAULT_OUTPUT_TYPE;
		String sqlTable = DEFAULT_SQL_TABLE;
		boolean prettyPrint = false;
		
		// parse input arguments
		while(i-- != 0) {
			int assignCharIndex = args[i].indexOf('=');
			String argNameStr; String argValue = null;
			if(assignCharIndex < 0) {
				argNameStr = args[i].substring(0, assignCharIndex).toLowerCase();
				argValue = args[i].substring(assignCharIndex+1);
			}
			else {
				argNameStr = args[i].toLowerCase();
			}
			
			switch(argNameStr) {
			case "--output":
				outputType = argValue;
				break;
				
			case "--sql-table":
				sqlTable = argValue;
				break;
				
			case "--pretty":
				prettyPrint = true;
				break;
				
			case "--debug":
				break;
			}
		}
		

		// assure trace file exists
		String filePath = fileName;

		/*
		try {
			filePath = (new File("./")).getCanonicalPath()+File.separatorChar+fileName;
		} catch (IOException e) {
			System.err.println("Could not find file \""+fileName+"\" in "+System.getProperty("user.dir"));
			System.exit(1);
		}*/
		
		File traceFile = new File(filePath);
		if(!traceFile.isFile()) {
			System.err.println(fileName+" is not a file");
			System.exit(1);
		}
		
		
		// find the location file, make sure it exists and decode it
		String dateString = fileName.substring(0, fileName.indexOf('.'));
		File locationFile = new File(dateString+".location.bin");
		if(!locationFile.isFile()) {
			System.err.println("Location file was not found: "+locationFile.getName());
			System.exit(1);
		}
		
		LocationDecoder locationDecoder = new LocationDecoder(locationFile);
		

		// setup an output renderer
		OutputRenderer outputRenderer = null;
		switch(outputType) {
		case "text":
			outputRenderer = new OutputRenderer_Text();
			break;
			
		default:
			System.err.println("Output type \""+outputType+"\" is undefined");
			System.exit(1);
			break;
		}
		
		
		// determine the type of trace and prepare the decoder
		String traceType = fileName.substring(
				fileName.indexOf('.')+1,
				fileName.length()-LENGTH_STRING_BINARY_FILE_EXTENSION
				);
		
		SensorDecoder traceDecoder = null;		
		switch(traceType) {
		case "trace-wap":
			traceDecoder = new Decoder_WAP();
			break;
			
		default:
			System.err.println("Trace type \""+traceType+"\" is undefined");
			System.exit(1);
			break;
		}

		// decode the trace file
		traceDecoder.decode(traceFile);
		
		// generate the output
		String dump = traceDecoder.output(outputRenderer, locationDecoder.getTrace());
		
		// dump to stdout
		System.out.println(dump);
		
	}
	
}
