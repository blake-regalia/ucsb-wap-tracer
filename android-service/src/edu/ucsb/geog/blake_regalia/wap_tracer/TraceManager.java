package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import android.content.Context;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;

public abstract class TraceManager {
	
	private static final String TAG = "TraceManager";

	protected static final int REASON_NONE           = 0xA000;
	protected static final int REASON_TIME_EXCEEDING = 0xA001;
	protected static final int REASON_IO_ERROR     = 0xA002;

	protected static final double CONVERT_MILLISECONDS_DECISECONDS = 0.01;
	protected static final double CONVERT_MILLISECONDS_CENTISECONDS = 0.1;
	protected static final int    CONVERT_COORDINATE_PRECISION_LONG = (int) 10e6;
	
	private static final char DECISECONDS_AS_CHAR_NEAR_FULL = 0xff00; // 25.5 seconds left until unable to encode
	
	
	private static final String PATH_DEFAULT_SD_DIRECTORY = "./ucsb-wap-traces/";

 
	protected long locationSensorProviderTimeStart;
	protected int shutdownReason;

	protected File traceFile;
	protected FileOutputStream traceFileData;
	protected File filesDir;
	protected File traceDir;
	protected Context mContext;
	
	/**
	 * @param context
	 * @param timeStart
	 */
	public TraceManager(Context context, long timeStart) {
		mContext = context;
		locationSensorProviderTimeStart = timeStart;
		shutdownReason = REASON_NONE;

        File sdCard = Environment.getExternalStorageDirectory();
		traceDir = new File(sdCard, PATH_DEFAULT_SD_DIRECTORY);
        traceDir.mkdir();
	}
	
	
	public abstract boolean openFile();
	public abstract boolean closeFile();
	
	protected void notifyShutdown(int reason) {
		shutdownReason = reason;
	}
	
	
	protected byte[] encodeDeciseconds(long millis) {
		char deciseconds = (char) ((millis) * CONVERT_MILLISECONDS_DECISECONDS);
		if(deciseconds >= DECISECONDS_AS_CHAR_NEAR_FULL) {
			notifyShutdown(REASON_TIME_EXCEEDING);
		}
		return Encoder.encode_char(deciseconds);
	}
	
	
	
	/**
	 * @return		the filename structure for the start time provided when the extending subclass was instantiated
	 */
	protected String getDateString() {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(locationSensorProviderTimeStart);
        
        return ""+(start.get(Calendar.YEAR)+"")+"_"
        		+zeroPad(start.get(Calendar.MONTH)+1)+"_"
        		+zeroPad(start.get(Calendar.DAY_OF_MONTH))+"-"
        		+zeroPad(start.get(Calendar.HOUR_OF_DAY))+"_"
        		+zeroPad(start.get(Calendar.MINUTE))+"_"
        		+zeroPad(start.get(Calendar.SECOND))+"."
        		+"v"+Registration.VERSION;
	}
	
	/**
	 * Creates a new trace file in the current application's file directory
	 * @param fileName
	 */
	protected boolean openTraceFile(String fileName) {
		filesDir = mContext.getFilesDir();
		try {
			traceFile = new File(filesDir, fileName);
			traceFileData = mContext.openFileOutput(fileName, Context.MODE_PRIVATE);
			return true;
		} catch (FileNotFoundException e) {
			Log.e(TAG, e.getMessage());
			notifyShutdown(REASON_IO_ERROR);
		}
		return false;
	}

	/**
	 * Writes data to the open trace file
	 * @param bytes
	 */
	protected boolean writeToTraceFile(byte[] bytes) {
		try {
			traceFileData.write(bytes);
			return true;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			notifyShutdown(REASON_IO_ERROR);
		}
		return false;
	}
	

	/**
	 * Closes the trace file and moves it to the SD card
	 * @param bytes
	 */
	protected boolean closeTraceFile() {
		if(traceFileData != null) {
			try {
				traceFileData.close();
				traceFileData = null;
				moveTraceFileToSdCard();
				return true;
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
				notifyShutdown(REASON_IO_ERROR);
			}
			return false;
		}
		else {
			Log.w(TAG, "no trace file");
			return true;
		}
	}
	
	private void moveTraceFileToSdCard() {
        File sdCardPath = new File(traceDir, traceFile.getName());
		Log.d(TAG, "renaming trace file to: "+sdCardPath.getAbsolutePath());
        traceFile.renameTo(sdCardPath);
	}
	
	
	private String zeroPad(int d) {
		if(d < 10) {
			return "0"+d;
		}
		return ""+d;
	}
	
	
	
	
	
	/*
	
	
	private static final String DATA_FILENAME = "trace.bin";
	
	private static final int MAX_NUM_WAPS = 255;

	// precision of the recorded time-stamp values in milliseconds, value of 10 yields 0.1 second resolution
	private static final int TIMESTAMP_PRECISION_MS = 10;
	private static final double TIMESTAMP_REDUCTION_FACTOR = 0.1 / TIMESTAMP_PRECISION_MS;

	// 10^6
	public final static int COORDINATE_PRECISION = 1000000;

	private static final char TIMESTAMP_NEAR_FULL = 0xff00;

	private static final int INITIAL_HASHTABLE_SIZE 			= 64;
	public static final int WIFI_SIGNAL_NUM_PRECISION_LEVELS 	= 45;
	
	public static final int WIFI_SIGNAL_MAX_DATA_LEVEL 			= 255;
	public static final double WIFI_SIGNAL_MAX_DATA_LEVEL_INV 	= 1.0 / WIFI_SIGNAL_MAX_DATA_LEVEL;
	public static final double WIFI_SIGNAL_NUM_LEVELS_FACTOR 	= ((double) WIFI_SIGNAL_MAX_DATA_LEVEL) / WIFI_SIGNAL_NUM_PRECISION_LEVELS;

	public static final double MINIMUM_TRACE_DISTANCE_M = 10.0;

	protected Hashtable<String, Integer> ssid_names;
	private Context context;

	private File files_dir = null;
	private File trace_file = null;
	private FileOutputStream data = null;
	
	private boolean isTraceInitialized = false;
	private long startTime;
	
	private Location originLocation = null;
	
	private boolean moved_min_distance = false;
	private boolean ssid_hashtable_full = false;
	private boolean timestamp_values_full = false;

	public TraceManager(Context _context) {
		context = _context;
	}
	
	public void startNewTrace() {
		ssid_names = new Hashtable<String, Integer>(INITIAL_HASHTABLE_SIZE);
		startTime = System.currentTimeMillis();
		
		moved_min_distance = false;
		ssid_hashtable_full = false;
		timestamp_values_full = false;
		
		originLocation = null;

		files_dir = context.getFilesDir();
		try {
			trace_file = new File(files_dir, DATA_FILENAME);
			data = context.openFileOutput(DATA_FILENAME, Context.MODE_PRIVATE);
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		}

		// write the file header
		try {
			String androidId = Registration.getAndroidId();
			
			data.write(Encoder.encode_int(Registration.VERSION));
			data.write(Encoder.encode_byte(androidId.length()));
			data.write(androidId.getBytes());
			data.write(Encoder.encode_long(startTime));
			data.write(Encoder.encode_long((long) (Boundary.UCSB_CAMPUS_LAT_MIN * COORDINATE_PRECISION)));
			data.write(Encoder.encode_long((long) (Boundary.UCSB_CAMPUS_LON_MIN * COORDINATE_PRECISION)));
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		 
		isTraceInitialized = true;
	}
	
	private byte[] encode_gps(Location location, long time) {
		ByteBuilder bytes = new ByteBuilder(Encoder.DATA_HEADER_GPS_LENGTH);
		
		double latitude = location.getLatitude() - Boundary.UCSB_CAMPUS_LAT_MIN;
		double longitude = location.getLongitude() - Boundary.UCSB_CAMPUS_LON_MIN;
		int accuracy = Math.round(location.getAccuracy());
		if(accuracy > 255) {
			accuracy = 255;
		}
		
		// v3
		int time_difference = (int) ((time-location.getTime())/100);
		if(time_difference < 0) time_difference = 0;
		else if(time_difference > 255) time_difference = 255;
		bytes.append(Encoder.encode_byte(time_difference));
		
		bytes.append_4(Encoder.encode_double_to_4_bytes(latitude, COORDINATE_PRECISION));
		bytes.append_4(Encoder.encode_double_to_4_bytes(longitude, COORDINATE_PRECISION));
		
		//v4
		int altitude = (int) Math.round(location.getAltitude());
		bytes.append(Encoder.encode_byte(altitude));
		
		bytes.append(Encoder.encode_byte(accuracy));
		
		return bytes.getBytes();		
	}



	private byte encode_ssid_name(String SSID) {
		int key = 0;
		Integer map = ssid_names.get(SSID);
		if(map == null) {
			key = ssid_names.size();
			if(key == 254) {
				ssid_hashtable_full = true;
			}
			ssid_names.put(SSID, new Integer(key));
		}
		else {
			key = map.intValue();
		}
		return (byte) (key & 0xff);
	}
	
	public byte[] encodeSSIDs() {
		StringBuilder sb = new StringBuilder();
		ByteBuilder zero = new ByteBuilder(1);
		zero.append((byte) (0 & 0xff));
		sb.append(new String(zero.getBytes()));
// sb.append(0);

		Iterator<Entry<String, Integer>> set = ssid_names.entrySet().iterator();
		while(set.hasNext()) {
			Entry<String, Integer> e = set.next();

			int v = ((Integer) e.getValue()).intValue();
			String ssid = (String) e.getKey();
			
			ByteBuilder bytes = new ByteBuilder(5);
			
			bytes.append_4(Encoder.encode_int( v ));
			bytes.append(Encoder.encode_byte(ssid.length()));
			
			// don't allow extended ascii codes to screw up the encoding
			sb.append(new String(bytes.getBytes()));
			int c = ssid.length();
			while(c-- != 0) {
				if(ssid.charAt(c) > 255) {
					ssid = ssid.replace(ssid.charAt(c), '?');
				}
			}
			sb.append(ssid);
		}

		return sb.toString().getBytes();
	}

	public int recordEvent(List<ScanResult> list, Location location) {
		long time = System.currentTimeMillis();
		
		int size = list.size();

		ByteBuilder bytes = new ByteBuilder(Encoder.DATA_HEADER_LENGTH + size*Encoder.DATA_ENTRY_LENGTH);

		// # of WAPS: 1 byte
		size = Math.min(size, MAX_NUM_WAPS);
		bytes.append(Encoder.encode_byte(size));
		
		// time offset (deci-seconds) of event relative to start time: 2 bytes  
		bytes.append(encode_time_ds(time - startTime));
		
		// gps header information: 11 bytes
		bytes.append(encode_gps(location, time));
		
		int i = size;
		while(i-- != 0) {
			ScanResult wap = (ScanResult) list.get(i);

			// encode BSSID
			bytes.append_6(
					WifiController.encodeBSSID(wap.BSSID)
					);
			
			// encode frequency
			bytes.append_2(
					Encoder.encode_char((char) wap.frequency)
					);	
			
			// encode signal strength
			int signal_level = (int) Math.round((WifiManager.calculateSignalLevel(wap.level, WIFI_SIGNAL_NUM_PRECISION_LEVELS) * WIFI_SIGNAL_NUM_LEVELS_FACTOR))+1;
			bytes.append(
					(byte) ((signal_level < 0)?  0
						: ((signal_level > 255)? 255
						: signal_level & 0xff))
					);
			
			// encode SSID name
			bytes.append(
					encode_ssid_name(wap.SSID)
				);
			
			if(ssid_hashtable_full) {
				break;
			}
		}
		
		try {
			data.write(bytes.getBytes());
		} catch (IOException e) {
			System.err.println("recordEvent();  "+e.getMessage());
			e.printStackTrace();
		}
		
		System.out.println("## wrote "+size+" waps to log");
		
		if(!moved_min_distance) {
			if(originLocation == null) {
				originLocation = location;
			}
			else if(location.distanceTo(originLocation) > MINIMUM_TRACE_DISTANCE_M) {
				moved_min_distance = true;
			}
		}
		
		if(ssid_hashtable_full || timestamp_values_full) {
			return -1;
		}
		
		return 0;
	}
	
	
	public void close() {
		if(!isTraceInitialized) return;
		try {
			data.write(encodeSSIDs());
			data.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("## closed log file");
		
		isTraceInitialized = false;
	}
	

	*/
	
	
	/**
	public long save() {
		if(trace_file == null) return 0;
		if(!moved_min_distance) {
			trace_file.delete();
			return -2;
		}
		long fileSize = trace_file.length();
		String file_size_str = fileSize+"";

        Calendar now = Calendar.getInstance();
        String fname = ""+(now.get(Calendar.YEAR)+"")+"."
        		+zero_pad(now.get(Calendar.MONTH)+1)+"."
        		+zero_pad(now.get(Calendar.DAY_OF_MONTH))+"-"
        		+zero_pad(now.get(Calendar.HOUR_OF_DAY))+"."
        		+zero_pad(now.get(Calendar.MINUTE))+"."
        		+zero_pad(now.get(Calendar.SECOND))+"_"
        		+"v"+Registration.VERSION+".bin";
		
		File sd_file = save_to_SD(trace_file, fname);

		HttpRequest request = new HttpRequest();
		request.addPair("data",
				new String(
						Base64.encode(
								Encoder.getBytesFromFile(trace_file)
								)
						)
				);
		request.addPair("name", fname);
		request.addPair("android-id", Registration.getAndroidId());
		request.addPair("phone-number", Registration.getPhoneNumber());
		request.addPair("version", Registration.VERSION+"");
		
		String response = request.submit(HttpRequest.POST);

		if(response == null) {
			System.out.println("no internet connection");
			return -1;
		}
		else if(!response.equals(file_size_str)) {
			System.out.println("server said: "+response);
			System.out.println("failed to upload file.");
			return -1;
		}
		else {
			System.out.println("uploaded "+fname+" to server; "+file_size_str+" bytes");
			sd_file.delete();
			trace_file.delete();
			return fileSize;
		}
	}

	public File save_to_SD(File trace_file, String file_name) {
        File root = Environment.getExternalStorageDirectory();

        File dir = new File(root, DEFAULT_SD_DIRECTORY);
        dir.mkdir();
        
        File output = new File(dir, file_name);
        
        FileOutputStream f = null;
        InputStream in = null;
		try {
			f = new FileOutputStream(output);
			in = new FileInputStream(trace_file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

        byte[] buffer = new byte[1024];
        int len = 0;
        try {
			while ((len = in.read(buffer)) > 0) {
			    f.write(buffer, 0, len);
			}
			f.close();
	        in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        try {
			System.out.println("saved to: "+output.getCanonicalPath()+"\n"+output.length()+"b");
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        return output;
	}
	**/
}
