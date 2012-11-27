package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.util.Log;

public class TraceManager_WAP extends TraceManager {

	private static final String TAG = "TraceManager(WAP)";

	private static final int BYTES_HEADER = 1 + 2;
	private static final int BYTES_ENTRY  = 6 + 2 + 1 + 1;

	private static final int SIZE_MAX_WAPS = (1 << 8) - 1;
	private static final int SIZE_INITIAL_HASHTABLE = (1 << 6);

	private static final int REASON_SSID_TABLE_FULL = 0xB001;


	private Context mContext;
	private Hashtable<String, Integer> ssidNames;
	private String traceType;

	public TraceManager_WAP(Context context, long timeStart) {
		super(context, timeStart);
		mContext = context;
		ssidNames = new Hashtable<String, Integer>(SIZE_INITIAL_HASHTABLE);
		traceType = "trace";
	}

	public TraceManager_WAP(Context context, long timeStart, String _traceType) {
		super(context, timeStart);
		mContext = context;
		ssidNames = new Hashtable<String, Integer>(SIZE_INITIAL_HASHTABLE);
		traceType = _traceType;
	}

	@Override
	public boolean openFile() {

		/* generate the file header */
		ByteBuilder byteBuilder = new ByteBuilder(0);


		/* create file & write file header */
		String traceFileName = getDateString()+"."+traceType+"-wap.bin";
		if(openTraceFile(traceFileName, ID_TYPE_TRACE_WAP, sensorLocationProviderTimeStart)) {
			return true;
			//return writeToTraceFile(byteBuilder.getBytes());
		}

		return false;
	}
	
	public boolean closeFile() {
		Log.d(TAG, "closing trace file");
		
		byte[] zero = {0};
		return
				// signify no more wap entries
				writeToTraceFile(zero)
				
				// lookup table for ssid names
				&& writeToTraceFile(encodeSsidMap())

				// and close the file
				&& closeTraceFile();
	}


	public int recordEvent(List<ScanResult> scanResults, int scanTimeOffsetMillis) {
		
		int listSize = Math.min(scanResults.size(), SIZE_MAX_WAPS);
		
		Log.d(TAG, "=> recordEvent(); "+listSize+" wap(s)");

		/* generate the data segment header & entry */
		ByteBuilder bytes = new ByteBuilder(BYTES_HEADER + listSize*BYTES_ENTRY);

		// number of entries [0-255]: 1 byte
		bytes.append(Encoder.encode_byte(listSize));

		// time offset (deci-seconds) of event relative to start time: 2 bytes  
		bytes.append_2(encodeDeciseconds(scanTimeOffsetMillis));


		/* iterate through all scan results */
		for(ScanResult wap : scanResults) {

			// encode BSSID: 6 bytes
			bytes.append_6(
					encodeBssid(wap.BSSID)
					);

			// encode SSID name: 1 byte identifier
			bytes.append(
					encodeSsidName(wap.SSID)
					);
			
			// encode frequency: 2 bytes
			bytes.append_2(
					Encoder.encode_char((char) wap.frequency)
					);	

			// encode signal strength [-128,127]: 1 byte
			bytes.append(
					Encoder.encode_byte(wap.level)
					);


			// stop encoding if no more space in SSID hash table
			if(shutdownReason == REASON_SSID_TABLE_FULL) {
				break;
			}
		}

		// check if the shutdown operation was flagged
		if(shutdownReason != REASON_NONE) {
			switch(shutdownReason) {
			case REASON_SSID_TABLE_FULL:
			case REASON_TIME_EXCEEDING:
				return SensorLooper.REASON_DATA_FULL;
				
			case REASON_IO_ERROR:
				return SensorLooper.REASON_IO_ERROR;
				
			default:
				return SensorLooper.REASON_UNKNOWN;
			}
		}
		else {
			// write the encoded bytes to a file
			if(writeToTraceFile(bytes.getBytes()) == false) {
				return SensorLooper.REASON_IO_ERROR;
			}

			return SensorLooper.REASON_NONE;	
		}
	}



	public static byte[] encodeBssid(String bssid) {
		String s = bssid.replaceAll(":", "");
		int len = s.length();
		byte[] data = new byte[len / 2];
		for(int i=0; i<len; i+=2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	private byte encodeSsidName(String ssid) {
		int key = 0;
		Integer map = ssidNames.get(ssid);
		if(map == null) {
			key = ssidNames.size();
			if(key == 255) {
				notifyShutdown(REASON_SSID_TABLE_FULL);
			}
			ssidNames.put(ssid, new Integer(key));
		}
		else {
			key = map.intValue();
		}
		return (byte) (key & 0xff);
	}


	public byte[] encodeSsidMap() {
		StringBuilder stringBuilder = new StringBuilder();

		Iterator<Entry<String, Integer>> set = ssidNames.entrySet().iterator();
		while(set.hasNext()) {
			Entry<String, Integer> e = set.next();

			int v = ((Integer) e.getValue()).intValue();
			String ssid = (String) e.getKey();
			
			/* generate string metadata */
			ByteBuilder bytes = new ByteBuilder(4 + 1);
			
			// ssid identifier: 1 byte
			bytes.append_4(
					Encoder.encode_int(v)
					);
			
			// ssid string length: 1 byte
			bytes.append(
					Encoder.encode_byte(ssid.length())
					);

			// append the string metadata to the byte stream 
			stringBuilder.append(new String(bytes.getBytes()));
			
			
			/* don't allow extended ascii codes to screw up the encoding */
			int c = ssid.length();
			while(c-- != 0) {
				if(ssid.charAt(c) > 255) {
					ssid = ssid.replace(ssid.charAt(c), '?');
				}
			}
			
			// append the string to the byte stream
			stringBuilder.append(ssid);
		}

		// build & return the final byte array 
		return stringBuilder.toString().getBytes();
	}
	
	/*

	public boolean hasAnySSID(String[] ssids) {
		boolean test_result = false;
		
		List<ScanResult> wap_list = getResults();
		int i = wap_list.size();
		while(i-- != 0) {
			ScanResult wap = wap_list.get(i);
			int x = ssids.length;
			while(x-- != 0) {
				if(wap.SSID.equals(ssids[x])) {
					test_result = true;
					break;
				}
			}
			if(test_result) break;
		}
		
		return test_result;
	}
	
	
	public static String wifiStateToString(int wifiState) {
		String state = "?";
		switch(wifiState) {
		case WifiManager.WIFI_STATE_ENABLED:
			state = "ENABLED"; break;
		case WifiManager.WIFI_STATE_ENABLING:
			state = "ENABLING"; break;
		case WifiManager.WIFI_STATE_DISABLED:
			state = "DISABLED"; break;
		case WifiManager.WIFI_STATE_DISABLING:
			state = "DISABLING"; break;
		case WifiManager.WIFI_STATE_UNKNOWN:
			state = "UNKNOWN"; break;
		}
		return state;
	}
	*/
}
