package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Looper;
import android.util.Log;


public class WapTracer extends LocationMonitor {

	private static final String TAG = "WapTracer";

	// the maximum age of a gps location fix, this is limited by how many bytes are used to encode gps age
	protected static final long GPS_AGE_MAX_MS = 25500; // 25.5 seconds

	// how long to wait between [after a scan completes, is encoded, and saved] and [starting a new scan]
	private static final long WIFI_SCAN_INTERVAL_MS = 1000; // 1 second

	/// how often to perform a wifi scan to check if the device is within boundaries
	protected long SSID_CHECK_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes

	// how often to use the GPS receiver to check if the device is within boundaries within the precision of the ssid check interval
	protected long LOCATION_CHECK_INTERVAL_MS = 3 * 60 * 60 * 1000; // 3 hours
	
	// array of Strings containing possible SSID triggers
	protected final String[] WIFI_SSID_TRIGGERS = {
			"UCSB Wireless Web",
			"UCSBIOGatICESS",
			"nanonet107",
			"nanonet100.1",
			"nanonet105",
			"PSetherwire",
			"nanonet_Eng1"
	};

	private enum Status {
		INIT,		// object is initializing
		WAIT_GPS,	// waiting for gps to enable
		WAIT_WIFI,	// waiting for wifi to enable
		READY,		// hardware is ready, waiting on criteria
		SCANNING,	// controller is scanning
		TRACING,	// cycle is in process
		STOPPING, 	// tracer is waiting/closing file
		CLOSED,		// scan was shut down
	}

	private Status mStatus;

	private int wifiScanTimeout = -1;
	private int ssidTriggerTimeout = -1;
	private int locationTriggerTimeout = -1;

	// objects
	private WifiController mWifiController;
	private TraceManager mTraceManager;


	public Runnable gps_hardware_enabled = new Runnable() {
		public synchronized void run() {

			Log.d(TAG, "gps enabled while "+mStatus);

			switch(mStatus) {

			// gps enabled before wifi
			case INIT:
				mStatus = Status.WAIT_WIFI;
				break;

				// app was waiting on gps
			case WAIT_GPS:
				attempt_scan();
				break;
			}
		}
	};

	public Runnable gps_hardware_disabled = new Runnable() {
		public void run() {

			switch(mStatus) {

			// gps was disabled before anything had a chance
			case INIT:
				Log.w(TAG, "GPS disabled before this initialized");
				break;

				// gps was disabled while it was starting up
			case WAIT_GPS:
				Log.e(TAG, "GPS was disabled while it was starting up");
				break;

				// gps was disabled while starting wifi
			case WAIT_WIFI:
				Log.i(TAG, "Why are you disabling GPS?");
				mStatus = Status.INIT;
				enable_gps();
				break;

				// gps was disabled while testing conditions
			case READY:
				Log.i(TAG, "Why are you disabling GPS? I am trying to test things");
				mStatus = Status.INIT;
				enable_gps();
				break;

				// gps was disabled during use
			case SCANNING:
			case TRACING:
				close();
				break;

				// wifi was disabled
			case STOPPING:
			case CLOSED:
				break;

			default:
				Log.w(TAG, "GPS disabled happend while: "+mStatus);
				break;
			}
		}
	};

	protected void enable_gps() {
		broadcast(UPDATES.SIMPLE, "Waiting for GPS to enable...");
		mHardwareMonitor.enable_gps(gps_hardware_enabled, gps_hardware_disabled, gps_hardware_fail);
	}



	/**
	 * runs when wifi is enabled 
	 */
	private Runnable wifi_hardware_enabled = new Runnable() {
		public synchronized void run() {

			switch(mStatus) {

			// wifi enabled before gps
			case INIT:
				mStatus = Status.WAIT_GPS;
				break;

				// app was waiting on wifi
			case WAIT_WIFI:
				attempt_scan();
				break;

			default:
				Log.w(TAG, "WiFi enabled while: "+mStatus);
				break;
			}
		}
	};

	/**
	 * runs if wifi is disabled 
	 */
	private Runnable wifi_hardware_disabled = new Runnable() {
		public void run() {

			switch(mStatus) {

			// wifi was disabled before anything had a chance
			case INIT:
				Log.w(TAG, "WiFi disabled before this initialized");
				break;

				// wifi was disabled while starting gps
			case WAIT_GPS:
				Log.i(TAG, "Why are you disabling WiFi?");
				mStatus = Status.INIT;
				enable_wifi();
				break;

				// wifi was disabled while it was starting up
			case WAIT_WIFI:
				Log.e(TAG, "WiFi was disabled while it was starting up");
				break;

				// wifi was disabled while testing criteria
			case READY:
				Log.w(TAG, "WiFi was disabled while testing conditions");
				break;

				// wifi was disabled during use
			case SCANNING:
			case TRACING:
				close();
				break;

				// wifi was disabled
			case STOPPING:
			case CLOSED:
				break;

			default:
				Log.w(TAG, "WiFi disabled happend while: "+mStatus);
				break;
			}
		}
	};

	/**
	 * runs if wifi could not be enabled on this device
	 */
	private Runnable wifi_hardware_fail = new Runnable() {
		public void run() {
			Log.e(TAG, "Your device does not support WiFi");
			postNotification(ActivityIntent.WIFI_FAIL);
		}
	};

	protected void enable_wifi() {
		broadcast(UPDATES.SIMPLE, "Waiting for WiFi to enable...");
		mHardwareMonitor.enable_wifi(wifi_hardware_enabled, wifi_hardware_disabled, wifi_hardware_fail);
	}	


	/**
	 * No-args constructor
	 * 
	 * @param context
	 */
	public WapTracer(Context context, Looper looper) {
		super(context, looper);
		init();
	}

	/**
	 * initialize fields 
	 */
	private void init() {
		mTraceManager = new TraceManager(mContext);
		mWifiController = new WifiController(mContext);

		// use GPS and WIFI to resolve a location 
		mLocationAdvisor.useProvider(LocationAdvisor.GPS | LocationAdvisor.WIFI);

		start();
	}

	// start
	@Override
	public void start() {
		if(mStatus == null || mStatus == Status.CLOSED) {
			mTraceManager.startNewTrace();
		}
		else {
			Log.e(TAG, "Nothing to start");
			return;
		}

		mStatus = Status.INIT;
		enable_wifi();
		enable_gps();
	}


	private Runnable location_fix = new Runnable() {
		public void run() {
			Location location = mLocationAdvisor.getLocation();
			if(location == null) {
				postNotification(ActivityIntent.GPS_SIGNAL_WEAK);
			}
			else {
				mLocationAdvisor.obtainLocation(LocationAdvisor.TRACE_LOCATION_LISTENER, null, position_lost);
				broadcast(UPDATES.SIMPLE, "Scanning nearby access points...");
				scan_wifi.run();
			}
		}
	};

	private Runnable position_lost = new Runnable() {
		public void run() {
			postNotification(ActivityIntent.GPS_SIGNAL_LOST);
		}
	};

	private void attempt_scan() {
		if(ssidTriggerTimeout != -1) {
			Timeout.clearTimeout(ssidTriggerTimeout);
		}
		if(locationTriggerTimeout != -1) {
			Timeout.clearTimeout(locationTriggerTimeout);
		}
		
		broadcast(UPDATES.SIMPLE, "Getting a location fix...");
		mStatus = Status.READY;
		mLocationAdvisor.obtainLocation(LocationAdvisor.BOUNDARY_CHECK_LISTENER, location_fix, position_lost);
	}

	private Runnable scan_wifi = new Runnable() {
		public void run() {

			switch(mStatus) {

			case SCANNING:
				break;

			case STOPPING:
				close();
				return;

			default:
				mStatus = Status.SCANNING;
				break;
			}

			mWifiController.scan(wifi_scan_success);
		}
	};

	@Override
	public void resume() {
		Log.d(TAG, "resuming "+mStatus);

		switch(mStatus) {

		case WAIT_WIFI:
			break;

		case WAIT_GPS:
			enable_gps();
			break;
		}
	}


	// user-genic stop
	@Override
	public void stop() {
		switch(mStatus) {
		case SCANNING:
		case TRACING:
			close();
			break;
		}
		mStatus = Status.CLOSED;
	}


	/**
	 * closes the trace file
	 */
	private void close() {
		if(wifiScanTimeout != -1) {
			Timeout.clearTimeout(wifiScanTimeout);
		}
		mWifiController.abort();
		mLocationAdvisor.shutDown();
		if(mStatus == Status.SCANNING || mStatus == Status.TRACING) {
			mStatus = Status.STOPPING;
			mTraceManager.close();
			broadcast(UPDATES.SIMPLE, "Attempting to upload trace file...");
			long bytes = mTraceManager.save();
			if(bytes == -1) {
				broadcast(UPDATES.UPLOADED, "No active internet connection");
				broadcast(UPDATES.SIMPLE, "Upload failed.");
			}
			else if(bytes == -2) {
				broadcast(UPDATES.UPLOADED, "No significant movement occurred");
				broadcast(UPDATES.SIMPLE, "Trace was discarded.");
			}
			else {
				broadcast(UPDATES.UPLOADED, "Contributed "+bytes+"-bytes of data.");
				broadcast(UPDATES.SIMPLE, "Upload successful."); 
			}
		}
		NotificationInterface.clear(mContext);
		Log.d(TAG, "Tracing complete");
		mStatus = Status.CLOSED;
	}

	/**
	 * runs when scan completes
	 */
	private Runnable wifi_scan_success = new Runnable() {
		public void run() {
			List<ScanResult> wapList = mWifiController.getResults();
			Location currentLocation = mLocationAdvisor.getLocation();

			if((System.currentTimeMillis()-currentLocation.getTime()) < GPS_AGE_MAX_MS) {
				mTraceManager.recordEvent(wapList, currentLocation);

				MainService.updateStaticFieldsForWap(wapList, currentLocation);
				broadcast(UPDATES.TRACING);

				mStatus = Status.TRACING;
				wifiScanTimeout = Timeout.setTimeout(scan_wifi, WIFI_SCAN_INTERVAL_MS);
			}
			else {
				close();
				postNotification(ActivityIntent.GPS_AGE_TOO_OLD);
				idle();
			}
		}
	};

	private void idle() {
		Timeout.setTimeout(remind_activity, 3600);
		mLocationAdvisor.shutDown();
		ssidTriggerTimeout = Timeout.setTimeout(ssid_trigger_check, SSID_CHECK_INTERVAL_MS);
		locationTriggerTimeout = Timeout.setTimeout(location_trigger_check, LOCATION_CHECK_INTERVAL_MS);
	}

	private Runnable ssid_trigger_test = new Runnable() {
		public void run() {
			mWifiController.abort();
			if(mWifiController.hasAnySSID(WIFI_SSID_TRIGGERS)) {
				attempt_scan();
			}
		}
	};

	private Runnable ssid_trigger_check = new Runnable() {
		public void run() {
			mWifiController.scan(ssid_trigger_test);
		}
	};

	private Runnable location_trigger_check = new Runnable() {
		public void run() {
			attempt_scan();
		}
	};


	private Runnable remind_activity = new Runnable() {
		public void run() {
			broadcast(UPDATES.SIMPLE, "Going idle until next scan...");
		}
	};
}
