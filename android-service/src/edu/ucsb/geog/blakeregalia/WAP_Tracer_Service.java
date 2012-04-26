package edu.ucsb.geog.blakeregalia;

import java.util.Date;
import java.util.List;

import android.app.Notification;
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
import android.widget.Toast;

public class WAP_Tracer_Service extends Service {
	protected Looper service_looper;
	private ServiceHandler service_handler;
	private HandlerThread service_thread;
	
	protected Hardware_Monitor hardware_monitor;
	protected Location_Advisor locator;
	protected Wifi_Controller wireless;
	protected Boundary campus_boundary;
	protected Registration client;
	protected TraceManager tracer;
	
	/** how long to wait between [after a scan completes, is encoded, and saved] and [starting a new scan] **/
	protected static final long WIFI_SCAN_INTERVAL_MS = 0;
	
	/** how often to perform a wifi scan to check if the device is within boundaries **/
	protected long SSID_CHECK_INTERVAL_MS = 1 * 60 * 1000; // 1 minute
			
	/** how often to use the GPS receiver to check if the device is within boundaries
	 *     within the precision of the ssid check interval
	 **/ 
	protected long LOCATION_CHECK_INTERVAL_MS = 3 * 60 * 60 * 1000; // 3 hours

	
	protected int wts_objective_length = 0;
	
	protected final int WTO_INITIALIZE               = 0;
	
	protected final int WTO_START_GPS                = 1;
	protected final int WTO_START_WIFI               = 2;

	protected final int WTO_REGISTER                 = 3;
	
	protected final int WTO_SCAN_WIFI                = 4;

	protected final int WTO_GOTO_SLEEP               = 5;
	protected final int WTO_SLEEP_IDLE               = 6;

	protected final int WTO_WAKE_GPS                 = 7;
	protected final int WTO_CHECK_BOUNDARY           = 8;

	protected final int WTO_BEGIN_TRACE              = 9;
	
	protected final int WTO_LOST_POSITION            = 10;
	
	protected final String[] WIFI_SSID_TRIGGERS = {
			"UCSB Wireless Web",
			"UCSBIOGatICESS",
			"nanonet107",
			"nanonet100.1",
			"nanonet105",
			"PSetherwire",
			"nanonet_Eng1"
	};
	
	protected final String gps = "gps";
	
	protected boolean stop_service_flag = false; 
	protected Location last_location;
	protected long wake_time;
	
	protected GpsLocator gpsL;
	
	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
		
		Context context;
		
		private void debug(String msg) {
			System.out.println(msg);
		}
		
		public ServiceHandler(Looper looper, Context _context) {
			super(looper);
			context = _context;
		}
		
		private void next_objective(int objective) {
			Message msg = service_handler.obtainMessage();
			msg.arg1 = objective;
			service_handler.handleMessage(msg);
		}

		// fires when the registration process was a success
		private Runnable registration_success = new Runnable() {
			public void run() {
				debug("::start:: ENABLE WIFI");
				next_objective(WTO_START_WIFI);
			}
		};
		
		// fires when it is concluded that the internet could not be reached
		private Runnable internet_fail = new Runnable() {
			public void run() {
				debug("::fail:: internet registration");
				debug("==> "+client.getAndroidId()+" : "+client.getPhoneNumber());
				next_objective(WTO_REGISTER + 1);
			}
		};
		
		// fires when one of the hardware interfaces has been disabled
		private Runnable hw_fail = new Runnable() {
			public void run() {
				debug("::start:: HW FAIL");
				next_objective(WTO_INITIALIZE);
			}
		};

		// runs if the wifi was toggled on by this program
		private Runnable wifi_ready = new Runnable() {
			public void run() {
				debug("::start:: REGISTER");
				next_objective(WTO_START_WIFI + 1);
			}
		};

		// runs if the wifi is disabled and cannot be enabled via program
		private Runnable wifi_fail = new Runnable() {
			public void run() {
				Toast.makeText(context, "Unable to enable WiFi. This app cannot run on your device", Toast.LENGTH_LONG);
			}
		};

		// runs when a wifi scan completes, test for ssid trigger
		private Runnable wifi_ssid_test = new Runnable() {
			public void run() {
				wireless.abort();
				
				boolean test_result = false;
				List<ScanResult> wap_list = wireless.getResults();
				int i = wap_list.size();
				while(i-- != 0) {
					ScanResult wap = wap_list.get(i);
					int x = WIFI_SSID_TRIGGERS.length;
					while(x-- != 0) {
						if(wap.SSID.equals(WIFI_SSID_TRIGGERS[x])) {
							test_result = true;
							break;
						}
					}
					if(test_result) break;
				}
				
				// if the ssid is triggered
				if(test_result) {
					debug("## SSID triggered");
					next_objective(WTO_WAKE_GPS);
					return;
				}
				// or if we haven't checked for a gps position in a while
				else if(
						last_location == null
						|| last_location.getTime() < ((new Date()).getTime() - LOCATION_CHECK_INTERVAL_MS))
				{
					debug("## last location is too old");
					next_objective(WTO_WAKE_GPS);
					return;
				}
				// they probably aren't on campus
				else {
					debug("## boundary check returned false");
					next_objective(WTO_GOTO_SLEEP);
					return;
				}
			}
		};
		
		// runs if the gps was toggled on by this program
		private Runnable gps_ready = new Runnable() {
			public void run() {
				debug("::start:: START WIFI");
				create_thread();
				next_objective(WTO_START_WIFI);
			}
		};

		// runs if the gps is disabled and cannot be enabled via program
		private Runnable gps_fail = new Runnable() {
			public void run() {
				hardware_monitor.enable(Hardware_Monitor.GPS, gps_ready, gps_fail, service_looper);
			}
		};

		// runs when the location manager has fixed a position 
		private Runnable location_fix = new Runnable() {
			public void run() {
				debug("::start:: LOCATION FIX");
				create_thread();
				next_objective(WTO_CHECK_BOUNDARY);
			}
		};

		// runs if gps was disabled during the trace
		private Runnable gps_fail_during_trace = new Runnable() {
			public void run() {
				debug("::stop:: TRACING");
				stop_scan();
				create_thread();
				next_objective(WTO_START_GPS);
			}
		};

		// runs if the user location was lost
		private Runnable gps_position_lost = new Runnable() {
			public void run() {
				debug("::stop:: TRACING");
				stop_scan();
				create_thread();
				next_objective(WTO_LOST_POSITION);
			}
		};

		// runs if gps was disabled during the trace
		private Runnable wifi_fail_during_trace = new Runnable() {
			public void run() {
				debug("::stop:: TRACING");
				stop_scan();
				create_thread();
				next_objective(WTO_START_WIFI);
			}
		};
		
		// wait for the minimum interval time to pass before scanning for waps again
		private Runnable wifi_scan_wait = new Runnable() {
			public void run() {
				long end_time = System.currentTimeMillis() + WIFI_SCAN_INTERVAL_MS;
				while (System.currentTimeMillis() < end_time) {
					try {
						wait(end_time - System.currentTimeMillis());
					} catch (Exception e) {}
				}
				wireless.scan(wifi_scan_trace, wifi_fail_during_trace);
			}
		};
		
		// the heart and soul of this program
		@SuppressWarnings("unused")
		private Runnable wifi_scan_trace = new Runnable() {
			public void run() {
				tracer.recordEvent(wireless.getResults(), locator.getLocation());
				if(WIFI_SCAN_INTERVAL_MS == 0) {
					wireless.scan(wifi_scan_trace, wifi_fail_during_trace);
				}
				else {
					Thread wait = (new Thread(wifi_scan_wait));
					wait.start();
				}
			}
		};
		
		private void stop_scan() {
			tracer.close();
			wireless.abort();
			locator.shutDown();
			tracer.save();
			System.out.println("Tracing complete. exiting");
			stopSelf();
		}
		
		@Override
		public void handleMessage(Message origin_msg) {

			debug("==== service looper begin ====");
			
			int objective = origin_msg.arg1;
			debug("::objective:: "+objective);

			while(true) {
				
				if(stop_service_flag) {
					debug("::stop:: service flag");
					return;
				}
				
				switch(objective) {
				
				
				case WTO_INITIALIZE:

//					objective = WTO_START_GPS; break;

					
					/* initialize GPS listener */
				case WTO_START_GPS:

					debug("GPS: "+hardware_monitor.is_enabled(Hardware_Monitor.GPS));

					// gps is disabled, the hardware monitor wants this thread to stop
					if(!hardware_monitor.enable(Hardware_Monitor.GPS, gps_ready, gps_fail, service_looper)) {
						return;
					}

					// otherwise, GPS is already enabled
//					objective = WTO_START_WIFI; break;
					
					
					/* initialize the WiFi */
				case WTO_START_WIFI:

					debug("WIFI: "+hardware_monitor.is_enabled(Hardware_Monitor.WIFI));

					if(!hardware_monitor.enable(Hardware_Monitor.WIFI, wifi_ready, wifi_fail, service_looper)) {
						return; 
					}

//					objective = WTO_REGISTER; break;

					
				case WTO_REGISTER:
					
					if(!client.register(registration_success, internet_fail)) {
						return;						
					}
					
//					objective = WTO_SCAN_WIFI; break;
					

					/* preliminary location fixer, test if any known wifi ssids are within range */
				case WTO_SCAN_WIFI:

					if(!hardware_monitor.is_enabled(Hardware_Monitor.WIFI)) {
						debug("wifi was disabled");
						objective = WTO_START_WIFI;
						break;
					}
					// resume sleeping if it's not time to wake up yet 
					if(wake_time > System.currentTimeMillis()) {
						objective = WTO_SLEEP_IDLE;
						break;
					}

					wireless.scan(wifi_ssid_test, hw_fail);

					debug("waiting for scan...");
					
					// do not continue looping this thread, the locator will call this service_handler again
					return;


					
				case WTO_GOTO_SLEEP:
					debug("** going to sleep");
					wake_time = System.currentTimeMillis() + SSID_CHECK_INTERVAL_MS;
					objective = WTO_SLEEP_IDLE;
					break;
					

				case WTO_SLEEP_IDLE:
					if(!hardware_monitor.is_enabled(Hardware_Monitor.WIFI)) {
						objective = WTO_START_WIFI;
					}
					else if(!hardware_monitor.is_enabled(Hardware_Monitor.GPS)) {
						objective = WTO_START_GPS;
					}
					else if(System.currentTimeMillis() > wake_time) {
						objective = WTO_SCAN_WIFI;
					}
					break;
					
					
				/* begin the attempt to obtain a position fix in order to check boundaries */
				case WTO_WAKE_GPS:

					// make sure the gps is enabled
					if(!hardware_monitor.is_enabled(Hardware_Monitor.GPS)) {
						debug("gps was disabled");
						objective = WTO_START_GPS;
						break;
					}
					
					locator.obtainLocation(Location_Advisor.BOUNDARY_CHECK_LISTENER, location_fix, hw_fail, service_looper);
					
					debug("waiting for location...");
					
					// do not continue looping this thread, the locator will call this service_handler again
					return;
					

				/* check if the device is within the boundaries */
				case WTO_CHECK_BOUNDARY:
					debug("** check boundary");
					
					Location location = locator.getLocation();
					
					// if the gps location timed out, go back to idle mode
					if(location == null) {
						System.out.println("** gps timed out. failed to get a position fix");
						objective = WTO_GOTO_SLEEP; 
						break;
					}
					
					if(!campus_boundary.check(location)) {
						objective = WTO_GOTO_SLEEP;
						break;
					}
					
					debug("** boundary positive!!");
					
					/*
					String crd = "" 
						+(Math.round(location.getLatitude()*10000)/10000.f)
						+", "+
						+(Math.round(location.getLongitude()*10000)/10000.f);
					
					Toast.makeText(context, crd, Toast.LENGTH_LONG).show();

					if(true) {
						objective = WTO_GOTO_SLEEP;
					}*/


				case WTO_BEGIN_TRACE:
					debug("** starting scan");
					locator.obtainLocation(Location_Advisor.TRACE_LOCATION_LISTENER, null, gps_position_lost, gps_fail_during_trace, service_looper);
					tracer.startNewTrace(client);
					wireless.scan(wifi_scan_trace, wifi_fail_during_trace);
					return;

				case WTO_LOST_POSITION:
					objective = WTO_GOTO_SLEEP;
					break;
					
				}
			}
		}
	}
	
	protected void debug(String msg) {
		System.out.println(msg);
	}

	@Override
	public void onCreate() {
		create_thread();

		hardware_monitor = new Hardware_Monitor(this);
		
		locator = new Location_Advisor(this);
		locator.useProvider(Location_Advisor.GPS | Location_Advisor.WIFI);
		
		wireless = new Wifi_Controller(this);
		
		campus_boundary = new Boundary(Boundary.UCSB_CAMPUS);
		
		client = new Registration(this);

		tracer = new TraceManager(this);
		
		// prevent this service from being terminated by task killers / low system memory
		startForeground(R.string.app_name, new Notification());
	}
	
	protected void create_thread() {
		service_thread = new HandlerThread("WAP_Tracer", Process.THREAD_PRIORITY_BACKGROUND);
		service_thread.start();
		service_looper = service_thread.getLooper();
		service_handler = new ServiceHandler(service_looper, this);
	}
		
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int restart = START_STICKY;
		int objective = WTO_INITIALIZE;

		service_looper = service_thread.getLooper();
		
		String obj = intent.getStringExtra("objective");

		debug("++++ service launched ++++");
		debug("::objective:: "+obj);
		
		if(obj != null) {
			if(obj.equals(Hardware_Monitor.ENABLE_GPS)) {
				objective = WTO_START_WIFI;
			}
			else if(obj.equals(Hardware_Monitor.ENABLE_WIFI)) {
				objective = WTO_SCAN_WIFI;
			}
			else if(obj.equals("no_wifi") || obj.equals("shutdown")) {
				objective = -1;
			}
		}
		if(objective != -1) {
			startJob(objective);
		}
		else {
			System.out.println("shutting down");
			stop_service_flag = true;
			locator.shutDown();
			hardware_monitor.shutDown();
			restart = START_NOT_STICKY;
		}

		// If we get killed, after returning from here, restart
		return restart;
	}

	private void startJob(int objective) {
		Message msg = service_handler.obtainMessage();
		msg.arg1 = objective;
		service_handler.handleMessage(msg);
	}

	/** We don't provide binding, so return null **/
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		debug("==== service looper done ////");
	}
}
