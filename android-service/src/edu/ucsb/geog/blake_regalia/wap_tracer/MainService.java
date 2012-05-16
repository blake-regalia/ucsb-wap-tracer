package edu.ucsb.geog.blake_regalia.wap_tracer;


import java.util.Date;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

public class MainService extends Service {
	
	public static boolean serviceRunning = false;
	private static boolean isTracing = false;
	private int wifiScanTimeout = -1;
	private long wakeTime;

	/** service fields **/
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	/** context-free constants: objectives that define the purpose of running the thread **/
	private static class OBJECTIVE {
		public static final int KILL_SERVICE    = -1;
		public static final int INITIALIZE      = 0x0;
		public static final int START_GPS       = 0x1;
		public static final int START_WIFI      = 0x2;
		public static final int REGISTER        = 0x3;
		public static final int SCAN_WIFI       = 0x4;
		public static final int WAKE_GPS        = 0x5;
		public static final int CHECK_BOUNDARY  = 0x6;
		public static final int BEGIN_TRACE     = 0x7;
		public static final int GOTO_SLEEP      = 0x8;
		public static final int SLEEP_EYES_OPEN = 0x9;
		public static final int LOST_POSITION   = 0xA;
	}

	/** context-free constants: intent objectives that define what just took place, for purpose of starting the service **/
	public static class INTENT_OBJECTIVE {
		public static final String START = "start";
		public static final String START_GPS  = "start gps";
		public static final String START_WIFI = "start wifi";
		public static final String FORCE_CHECK = "force check";
		public static final String KILL_SERVICE = "kill service";
	}
	
	/** context-free constants: activity intents define what the user interface activity should be doing **/
	public static class ACTIVITY_INTENT {
		public static final String GPS_ENABLE = "gps-enable";
		public static final String WIFI_FAIL = "wifi-fail";
		public static final String DO_NOTHING = "do-nothing";
	}

	/** context-free constants: broadcast updates define what the service is telling an activity about **/
	public static final String BROADCAST_UPDATES = MainService.class.getPackage().getName()+".UPDATES";
	public static class UPDATES {
		public static final String TRACING = "tracing";
		public static final String SIMPLE = "simple";
		public static final String UPLOADED = "upload";
		public static final String OUT_OF_BOUNDS = "bounds";
		public static final String LOCATION_UNKNOWN = "location";
		public static final String GPS_LOST = "gps lost";
		public static final String SLEEPING = "sleeping";
	}

	/** context-free constants: **/
	// how long to wait between [after a scan completes, is encoded, and saved] and [starting a new scan]
	private static final long WIFI_SCAN_INTERVAL_MS = 1000; // 1 second

	/// how often to perform a wifi scan to check if the device is within boundaries
	protected long SSID_CHECK_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes

	// how often to use the GPS receiver to check if the device is within boundaries within the precision of the ssid check interval
	protected long LOCATION_CHECK_INTERVAL_MS = 3 * 60 * 60 * 1000; // 3 hours


	/** class objects */
	protected HardwareMonitor hardware_monitor;
	protected LocationAdvisor locator;
	protected WifiController wireless;
	protected Boundary campus_boundary;
	protected Registration client;
	protected TraceManager tracer;
	protected NotificationInterface notification;


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

	/** broadcast fields */
	private static List<ScanResult> lastScanResults;
	private static Location lastLocation;

	public static List<ScanResult> getLastScan() {
		return lastScanResults;
	}
	public static Location getLastLocation() {
		return lastLocation;
	}

	/** this gets called once the service is born **/
	@Override
	public void onCreate() {
		// Start up the thread running the service. we create a separate thread because the service normally runs in the process's
		// main thread, which we don't want to block (Activity's UI thread). we also make it background priority
		HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		Log.d("MainService::onCreate", "**create**");

		/* initialize the class object fields */

		// controls the power status of hardware interfaces
		hardware_monitor = new HardwareMonitor(this);

		// responsible for all location functionalilty
		locator = new LocationAdvisor(this);

		// 	... use GPS and WIFI to resolve a location 
		locator.useProvider(LocationAdvisor.GPS | LocationAdvisor.WIFI);

		// responsible for manager the wifi interface
		wireless = new WifiController(this);

		// defines a boundary for testing lat/lng coordinates
		campus_boundary = new Boundary(Boundary.UCSB_CAMPUS);

		// handles any procedure/information that has to do with registering this device uniquely
		client = new Registration(this);

		// this object will manage the current trace. handles file saving, uploading, etc.
		tracer = new TraceManager(this);
		
		// handles UI notifications in the status bar and starting activities
		notification = new NotificationInterface(this);


		// Get the HandlerThread's Looper and use it for our Handler 
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}



	/** this gets called any time an Activity explicity starts the service **/
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		serviceRunning = true;
		
		Log.e("MainService", "startService("+intent+")");

		// by default, assume this service was started for the first time
		int objective = OBJECTIVE.INITIALIZE;
		
		// something bad has happened. exit the app
		if(intent == null) {
			System.err.println("no intent");
			//stopSelf();
			return START_NOT_STICKY;
		}

		// get the objective with which this service was started: as passed in by the calling Activity
		String intentObjective = intent.getStringExtra("objective");

		// if the service was started with an intent objective
		if(intentObjective != null) {
			// the service is starting up to be initialized
			if(intentObjective.equals(INTENT_OBJECTIVE.START)) {

			}
			// the service is starting up again because GPS was just enabled
			else if(intentObjective.equals(INTENT_OBJECTIVE.START_GPS)) {
				notification.clear();
			}
			// the service is starting up again because WIFI was just enabled
			else if(intentObjective.equals(INTENT_OBJECTIVE.START_WIFI)) {
				notification.clear();
			}
			// an activity wants to check for gps position
			else if(intentObjective.equals(INTENT_OBJECTIVE.FORCE_CHECK)) {
				if(!isTracing) {
					wakeTime = 0;
				}
			}
			// the service is trying to be killed
			else if(intentObjective.equals(INTENT_OBJECTIVE.KILL_SERVICE)) {
				System.out.println("*** shutting down... ***");
				serviceRunning = false;
				stopSelf();
				return START_NOT_STICKY;
			}
		}
		
		serviceRunning = true;

		// don't worry about unique start ids, this service should only be running one process at a time
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = objective;
		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	
	/** broadcasts a message of the given type, for Activities to receive updates */
	private void broadcast(String type) {
		Intent intent = new Intent(BROADCAST_UPDATES);
		intent.putExtra("type", type);
        sendBroadcast(intent);
	}
	
	/** broadcasts a message of the given type with extra string, for Activities to receive updates */
	private void broadcast(String type, String extra) {
		Intent intent = new Intent(BROADCAST_UPDATES);
		intent.putExtra("type", type);
		intent.putExtra(type, extra);
        sendBroadcast(intent);
	}

	/** abort tracing; attempt to stop writing to the file, close resources, listeners, etc. */
	private void stopTrace() {
		if(wifiScanTimeout != -1) {
			Timeout.clearTimeout(wifiScanTimeout);
		}
		wireless.abort();
		locator.shutDown();
		if(isTracing) {
			tracer.close();
			broadcast(UPDATES.SIMPLE, "Attempting to upload trace file...");
			long bytes = tracer.save();
			if(bytes == -1) {
				broadcast(UPDATES.UPLOADED, "Upload failed. No active internet connection");
			}
			else if(bytes == -2) {
				broadcast(UPDATES.UPLOADED, "No significant movement occurred. Trace was discarded");
			}
			else {
				broadcast(UPDATES.UPLOADED, "Upload successful. Contributed "+bytes+"-bytes of data.");			
			}
			isTracing = false;
		}
		notification.clear();
		System.out.println("Tracing complete");
	}

	/** handler class receives messages from the thread  **/
	private final class ServiceHandler extends Handler {

		private Context mContext;
		private Location mLastLocation;

		/** this helper function calls the message handling function, passing it the intended objective in message form */
		private void start(int objective) {
			Message msg = this.obtainMessage();
			msg.arg1 = objective;
			handleMessage(msg);
		}

		/** print messages for the debugger of this program */
		private void debug(String text) {
			Log.i("MainService", text);
		}

		/** launches an activity on the UI thread */
		private void activate(Class activity) {
			/*
				context.startActivity(intent)
				activity
			 */
		}


		/** runnable thread functions for callback **/

		// fires when the registration process was a success
		private Runnable registration_success = new Runnable() {
			public void run() {
				start(OBJECTIVE.REGISTER + 1);
			}
		};

		// fires when it is concluded that the internet could not be reached
		private Runnable registration_failure = new Runnable() {
			public void run() {
				debug("::failed:: internet registration");
				debug("==> "+Registration.getAndroidId()+" : "+Registration.getPhoneNumber());
				start(OBJECTIVE.REGISTER + 1);
			}
		};

		// fires when one of the hardware interfaces has been disabled
		private Runnable hw_disabled = new Runnable() {
			public void run() {
				start(OBJECTIVE.INITIALIZE);
			}
		};

		// runs if the wifi was toggled on by this program
		private Runnable wifi_ready = new Runnable() {
			public void run() {
				debug("wifi enabled");
				start(OBJECTIVE.START_WIFI + 1);
			}
		};

		// runs if the wifi is disabled and cannot be enabled via program
		private Runnable wifi_fail = new Runnable() {
			public void run() {
				notification.postNotification(ActivityAlertUser.class, ACTIVITY_INTENT.WIFI_FAIL, false, "This app can't run on your device", "Touch here for more information");
				stopSelf();
			}
		};

		// runs if the gps was toggled on by this program
		private Runnable gps_ready = new Runnable() {
			public void run() {
				start(OBJECTIVE.START_GPS + 1);
			}
		};

		// runs if the gps is disabled and cannot be enabled via program
		private Runnable gps_fail = new Runnable() {
			public void run() {
				notification.postNotification(ActivityAlertUser.class, ACTIVITY_INTENT.GPS_ENABLE, true, "GPS needs to be enabled", "GPS must remain enabled for app to run");
			}
		};

		// runs when the location manager has fixed a position after waking gps
		private Runnable location_fix = new Runnable() {
			public void run() {
				start(OBJECTIVE.WAKE_GPS + 1);
			}
		};
		
		// runs when the gps is ready for a trace
		private Runnable begin_tracing = new Runnable() {
			public void run() {
				notification.postNotification(ActivityAlertUser.class, ACTIVITY_INTENT.DO_NOTHING, false, "Your location is being recorded", "");
				tracer.startNewTrace();
				wireless.scan(wifi_scan_trace, wifi_disabled_during_trace);				
			}
		};
		
		// runs if gps was disabled during the trace
		private Runnable gps_disabled_during_trace = new Runnable() {
			public void run() {
				stopTrace();
				start(OBJECTIVE.START_GPS);
			}
		};

		// runs if the user location was lost during trace
		private Runnable gps_position_lost_during_trace = new Runnable() {
			public void run() {
				stopTrace();
				broadcast(UPDATES.GPS_LOST);
				start(OBJECTIVE.LOST_POSITION);
			}
		};

		// runs if wifi was disabled during the trace
		private Runnable wifi_disabled_during_trace = new Runnable() {
			public void run() {
				stopTrace();
				start(OBJECTIVE.START_WIFI);
			}
		};

		// wait for the minimum interval time to pass before scanning for waps again
		private Runnable scan_for_waps = new Runnable() {
			public void run() {
				if(!isTracing) return;
				wireless.scan(wifi_scan_trace, wifi_disabled_during_trace);
			}
		};
		
		
		
		// wifi was disabled while monitoring
		private Runnable wifi_disabled_during_monitor = new Runnable() {
			public void run() {
				
			}
		};
		
		// wait for the minimum interval time to pass before scanning for waps again
		private Runnable scan_for_rssi = new Runnable() {
			public void run() {
				if(!hardware_monitor.is_enabled(HardwareMonitor.WIFI)) {
					
				}
				else if(!hardware_monitor.is_enabled(HardwareMonitor.GPS)) {
					start(OBJECTIVE.START_GPS);
				}
				else if(System.currentTimeMillis() > wakeTime) {
					start(OBJECTIVE.SCAN_WIFI);
				}
				wireless.scan(monitor_waps, wifi_disabled_during_monitor);
			}
		};
		
		// continuously monitor the wifi access point levels 
		private Runnable monitor_waps = new Runnable() {
			public void run() {
				scan_for_rssi.run();
			}
		};

		
		// runs when a wifi scan completes, test for ssid trigger
		private Runnable wifi_ssid_test = new Runnable() {
			public void run() {
				wireless.abort();

				// if the ssid is triggered
				if(wireless.hasAnySSID(WIFI_SSID_TRIGGERS)) {
					debug("## SSID triggered");
					start(OBJECTIVE.SCAN_WIFI + 1);
					return;
				}
				// or if we haven't checked for a gps position in a while
				else if(
						mLastLocation == null
						|| mLastLocation.getTime() < ((new Date()).getTime() - LOCATION_CHECK_INTERVAL_MS))
				{
					debug("## last location is too old");
					start(OBJECTIVE.SCAN_WIFI + 1);
					return;
				}
				// they probably aren't on campus
				else {
					debug("## boundary check returned false");
					start(OBJECTIVE.GOTO_SLEEP);
					return;
				}
			}
		};

		// the heart and soul of this program
		@SuppressWarnings("unused")
		private Runnable wifi_scan_trace = new Runnable() {
			public void run() {
				if(!isTracing) {
					start(OBJECTIVE.INITIALIZE);
					return;
				}
				lastScanResults = wireless.getResults();
				lastLocation = locator.getLocation();
				int status = tracer.recordEvent(lastScanResults, lastLocation);
				
				// the trace file is overflowing
				if(status < 0) {
					stopTrace();
					start(OBJECTIVE.BEGIN_TRACE);
					return;
				}
				
				broadcast(UPDATES.TRACING);
		        
				if(WIFI_SCAN_INTERVAL_MS == 0) {
					scan_for_waps.run();
				}
				else {
					wifiScanTimeout = Timeout.setTimeout(scan_for_waps, WIFI_SCAN_INTERVAL_MS);
				}
			}
		};

		/** constructor **/
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {

			int objective = msg.arg1;
			
			Log.e("MainService", "handleMessage("+objective+")");

			while(true) {
				if(!serviceRunning) return;
					
				switch(objective) {

				case OBJECTIVE.INITIALIZE:

				case OBJECTIVE.START_GPS:
					broadcast(UPDATES.SIMPLE, "Waiting for GPS to enable...");
					debug("GPS: "+hardware_monitor.is_enabled(HardwareMonitor.GPS));

					// gps is disabled, the hardware monitor wants this loop to stop
					if(!hardware_monitor.enable(HardwareMonitor.GPS, gps_ready, gps_fail, mServiceLooper)) {
						return;
					}

				case OBJECTIVE.START_WIFI:
					broadcast(UPDATES.SIMPLE, "Waiting for WiFi to enable...");
					debug("WIFI: "+hardware_monitor.is_enabled(HardwareMonitor.WIFI));

					// wifi is disabled, the hardware monitor wants this loop to stop
					if(!hardware_monitor.enable(HardwareMonitor.WIFI, wifi_ready, wifi_fail, mServiceLooper)) {
						return; 
					}

				case OBJECTIVE.REGISTER:
					broadcast(UPDATES.SIMPLE, "Registering device...");
					if(!client.register(registration_success, registration_failure)) {
						return;						
					}
					
				case OBJECTIVE.SCAN_WIFI:
					broadcast(UPDATES.SIMPLE, "Scanning nearby access points...");
					// wifi was disabled, break the switch and let the loop jump to the start_wifi objective
					if(!hardware_monitor.is_enabled(HardwareMonitor.WIFI)) {
						debug("wifi was disabled");
						objective = OBJECTIVE.START_WIFI;
						break;
					}
					// resume sleeping if it's not time to wake up yet 
					if(wakeTime > System.currentTimeMillis()) {
						objective = OBJECTIVE.SLEEP_EYES_OPEN;
						break;
					}

					wireless.scan(wifi_ssid_test, hw_disabled);

					debug("waiting for scan...");

					// do not continue looping this thread, the locator will call this service_handler again
					return;

				case OBJECTIVE.WAKE_GPS:
					broadcast(UPDATES.SIMPLE, "Getting a location fix...");
					// make sure the gps is enabled
					if(!hardware_monitor.is_enabled(HardwareMonitor.GPS)) {
						debug("gps was disabled");
						objective = OBJECTIVE.START_GPS;
						break;
					}

					System.out.println("MainService::WAKE_GPS: locator.obtainLocation()");
					locator.obtainLocation(LocationAdvisor.BOUNDARY_CHECK_LISTENER, location_fix, hw_disabled, mServiceLooper);

					debug("waiting for location...");

					// do not continue looping this thread, the locator will call this service_handler again
					return;


				case OBJECTIVE.CHECK_BOUNDARY:
					broadcast(UPDATES.SIMPLE, "Checking your location...");
					debug("** check boundary");

					Location location = locator.getLocation();

					// if the gps location timed out, go back to idle mode
					if(location == null) {
						debug("** location advisor timed out. failed to get a position fix");
						broadcast(UPDATES.LOCATION_UNKNOWN);
						objective = OBJECTIVE.GOTO_SLEEP; 
						break;
					}

					if(!campus_boundary.check(location)) {
						broadcast(UPDATES.OUT_OF_BOUNDS);
						objective = OBJECTIVE.GOTO_SLEEP;
						break;
					}

					debug("** boundary positive!!");


				case OBJECTIVE.BEGIN_TRACE:
					isTracing = true;
					broadcast(UPDATES.SIMPLE, "Starting trace...");
					debug("** starting scan");
					locator.obtainLocation(LocationAdvisor.TRACE_LOCATION_LISTENER, begin_tracing, gps_position_lost_during_trace, gps_disabled_during_trace, mServiceLooper);
					return;


				case OBJECTIVE.GOTO_SLEEP:
					broadcast(UPDATES.SLEEPING, "Sleeping for a few minutes before checking location again. Press \"Begin Tracing\" to initiate a trace.");
					debug("** going to sleep");
					wakeTime = System.currentTimeMillis() + SSID_CHECK_INTERVAL_MS;
					objective = OBJECTIVE.SLEEP_EYES_OPEN;
					break;

				case OBJECTIVE.SLEEP_EYES_OPEN:
					if(!hardware_monitor.is_enabled(HardwareMonitor.WIFI)) {
						objective = OBJECTIVE.START_WIFI;
					}
					else if(!hardware_monitor.is_enabled(HardwareMonitor.GPS)) {
						objective = OBJECTIVE.START_GPS;
					}
					else if(System.currentTimeMillis() > wakeTime) {
						objective = OBJECTIVE.SCAN_WIFI;
					}
					break;

				case OBJECTIVE.LOST_POSITION:
					objective = OBJECTIVE.GOTO_SLEEP;
					break;

				}
			}
		}
	}


	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onDestroy() {
		broadcast(UPDATES.SIMPLE, "Service stopped.");
		serviceRunning = false;
		stopTrace();
		Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show(); 
	}

}
