package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;

public class StationaryMonitor {
	
	private static final String DATA_FILENAME = "monitor.bin";

	private static final String DEFAULT_SD_DIRECTORY = "./ucsb-wap-traces/";
	
	//                                        BSSID   rssi
	private static final int WIFI_DATA_LENGTH = 6   +   1;
	
	public static final int WIFI_SIGNAL_NUM_PRECISION_LEVELS 	= 45;
	
	public static final int WIFI_SIGNAL_MAX_DATA_LEVEL 		= 255;
	public static final double WIFI_SIGNAL_MAX_DATA_LEVEL_INV 	= 1.0 / WIFI_SIGNAL_MAX_DATA_LEVEL;
	public static final double WIFI_SIGNAL_NUM_LEVELS_FACTOR 	= ((double) WIFI_SIGNAL_MAX_DATA_LEVEL) / WIFI_SIGNAL_NUM_PRECISION_LEVELS;

	/** precision of the recorded time-stamp values in milliseconds, value of 10 yields 0.1 second resolution **/
	private static final int TIMESTAMP_PRECISION_MS = 10;
	private static final double TIMESTAMP_REDUCTION_FACTOR = 0.1 / TIMESTAMP_PRECISION_MS;

	private static final char TIMESTAMP_NEAR_FULL = 0xff00;
	
	private Context mContext;
	private File files_dir = null;
	private File monitor_file = null;
	private FileOutputStream data = null;
	
	private boolean timestamp_values_full = false;
	
	private long startTime = 0;
	private boolean isMonitorInitialized = false;
	
	public StationaryMonitor(Context context) {
		mContext = context;
	}
	
	public void startNewMonitor() {
		
		startTime = System.currentTimeMillis();

		files_dir = mContext.getFilesDir();
		try {
			monitor_file = new File(files_dir, DATA_FILENAME);
			data = mContext.openFileOutput(DATA_FILENAME, Context.MODE_PRIVATE);
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		}

		/* write the file header */
		try {
			data.write(Encoder.encode_int(Registration.VERSION));
			data.write(Encoder.encode_long(startTime));
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

		isMonitorInitialized = true;
	}
	

	private byte[] encode_time_ds(long time) {
		char ts = (char) ((time) * TIMESTAMP_REDUCTION_FACTOR);
		if(ts >= TIMESTAMP_NEAR_FULL) {
			timestamp_values_full = true;
		}
		return Encoder.encode_char(ts);
	}
	
	public int recordEvent(List<ScanResult> scan) {
		long time = System.currentTimeMillis();
		int size = scan.size();
		ByteBuilder bytes = new ByteBuilder(4 + size*WIFI_DATA_LENGTH);
		
		bytes.append_4(encode_time_ds(time - startTime));

		int i = size;
		while(i-- !=0) {
			ScanResult wap = scan.get(i);
			
			// encode BSSID
			bytes.append_6(
					WifiController.encodeBSSID(wap.BSSID)
					);
		
			// encode signal strength
			int signal_level = (int) Math.round((WifiManager.calculateSignalLevel(wap.level, WIFI_SIGNAL_NUM_PRECISION_LEVELS) * WIFI_SIGNAL_NUM_LEVELS_FACTOR))+1;
			bytes.append(
					(byte) ((signal_level < 0)?  0
						: ((signal_level > 255)? 255
						: signal_level & 0xff))
					);
			
		}
		
		if(timestamp_values_full) {
			return -1;
		}
		
		return 0;
	}
	
	public void close() {
		try {
			data.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public long save() {
		if(monitor_file == null) return 0;
		long fileSize = monitor_file.length();
		String file_size_str = fileSize+"";

        Calendar now = Calendar.getInstance();
        String fname = ""+(now.get(Calendar.YEAR)+"")+"."
        		+String.format("%02d", now.get(Calendar.MONTH)+1)+"."
        		+String.format("%02d", now.get(Calendar.DAY_OF_MONTH))+"-"
        		+String.format("%02d", now.get(Calendar.HOUR_OF_DAY))+"."
        		+String.format("%02d", now.get(Calendar.MINUTE))+"."
        		+String.format("%02d", now.get(Calendar.SECOND))+"_"
        		+"v"+Registration.VERSION+".bin";
		
		File sd_file = save_to_SD(monitor_file, fname);

		HttpRequest request = new HttpRequest();
		request.addPair("data",
				new String(
						Base64.encode(
								Encoder.getBytesFromFile(monitor_file)
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
			monitor_file.delete();
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
}
