package edu.ucsb.geog.blakeregalia;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.Toast;

public class WAP_Tracer_Service extends Service {
	private ServiceHandler service_handler;
	
	protected Hardware_Monitor hardware_monitor;
	protected Location_Advisor locator;
	
	/** how often to use the GPS receiver to check if the device is within boundaries  **/ 
	protected static long BOUNDARY_CHECK_INTERVAL_MS = 15 * 60 * 1000;

	
	protected static int wts_objective_length = 0;
	protected static final int WTO_INITIALIZE               = 0;
	protected static final int WTO_START_WIFI_MONITOR       = 1;
	protected static final int WTO_START_GPS_MONITOR        = 2;
	protected static final int WTO_WAKE_GPS                 = 3;
	protected static final int WTO_CHECK_BOUNDARY           = 4;
	protected static final int WTO_WAIT_FOR_WITHIN_BOUNDARY = 5;
	
	protected static final String gps = "gps";
	
	protected boolean stop_service_flag = false; 
	
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

		// runs if the wifi was toggled on by this program
		private Runnable wifi_ready = new Runnable() {
			public void run() {
				debug("wifi is ready, enable gps");
				next_objective(WTO_START_GPS_MONITOR);
			}
		};

		// runs if the wifi is disabled and cannot be enabled via program
		private Runnable wifi_fail = new Runnable() {
			public void run() {
				Toast.makeText(context, "Unable to enable WiFi. This app cannot run on your device", Toast.LENGTH_LONG);
			}
		};
		
		// runs when the location manager has fixed a position 
		private Runnable gps_fix = new Runnable() {
			public void run() {
				create_thread();
				startJob(WTO_CHECK_BOUNDARY);
			}
		};
		
		// runs if the gps was toggled on by this program
		private Runnable gps_ready = new Runnable() {
			public void run() {
				debug("gps is ready, wake gps");
				create_thread();
				startJob(WTO_WAKE_GPS);
			}
		};

		// runs if the gps is disabled and cannot be enabled via program
		private Runnable gps_fail = new Runnable() {
			public void run() {
				hardware_monitor.enable(Hardware_Monitor.GPS, gps_fix, gps_fail);
			}
		};
		
		@Override
		public void handleMessage(Message origin_msg) {
			/* fetch the context of this service from the obj property of the Message */

			debug("handle Message called");
			int objective = origin_msg.arg1;

			while(true) {
				debug("switching on "+objective);
				if(stop_service_flag) {
					return;
				}
				
				switch(objective) {
				
				
				case WTO_INITIALIZE:

					objective = WTO_START_WIFI_MONITOR;
					break;

					
				/* initialize the WiFi */
				case WTO_START_WIFI_MONITOR:
					
					debug("starting wifi if it's off");
					
					if(hardware_monitor.enable(Hardware_Monitor.WIFI, wifi_ready, wifi_fail)) {
						objective = WTO_START_GPS_MONITOR; 
					}
					else {
						stopSelf();
						return;
					}
					
					break;

					
				/* initialize GPS listener */
				case WTO_START_GPS_MONITOR:

					debug("starting gps if it's off. WTF?!");
					
					// returns true if GPS is already enabled
					if(hardware_monitor.enable(Hardware_Monitor.GPS, gps_ready, gps_fail)) {
						objective = WTO_WAKE_GPS;
					}
					else {
						// gps is disabled, the hardware monitor wants this thread to stop
						stopSelf();
						return;
					}
					
					break;

					
				/* begin the attempt to obtain a position fix in order to check boundaries */
				case WTO_WAKE_GPS:
					
					debug("attempting gps fix");

					// make sure the gps is enabled
					if(!hardware_monitor.is_enabled(Hardware_Monitor.GPS)) {
						debug("gps is not enabled, going back to start monitor");
						objective = WTO_START_GPS_MONITOR;
						break;
					}
					
					locator.obtainGpsLocation(gps_fix, gps_fail);
					
					/* do not continue looping this thread, the locator will call this service_handler again */
					
					debug("waiting for location...");
					return;
					

				/* check if the device is within the boundaries */
				case WTO_CHECK_BOUNDARY:
					debug("check boundary!!");
					locator.getLocation();
					stopSelf();
					return;
					
					
				/* wait until the device is within the target boundaries */
				case WTO_WAIT_FOR_WITHIN_BOUNDARY:
										
					/* to save cycles, use SystemClock's wait to put the thread to sleep */
					long end_time = System.currentTimeMillis() + BOUNDARY_CHECK_INTERVAL_MS;
					while(System.currentTimeMillis() < end_time) {
				          synchronized (this) {
				              try {
				                  wait(end_time - System.currentTimeMillis());
				              } catch (Exception e) {
				            	  
				              }
				          }
				     }
					objective = WTO_WAKE_GPS;
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
		
		// prevent this service from being terminated by task killers / low system memory
		startForeground(R.string.app_name, new Notification());
	}
	
	protected void create_thread() {
		HandlerThread thread = new HandlerThread("WAP_Tracer", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		service_handler = new ServiceHandler(thread.getLooper(), this);
	}
		
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int restart = START_STICKY;
		int objective = WTO_INITIALIZE;
		
		String obj = intent.getStringExtra("objective");
		if(obj != null) {
			if(obj.equals(Hardware_Monitor.ENABLE_GPS)) {
				objective = WTO_WAKE_GPS;
			}
			else if(obj.equals(Hardware_Monitor.ENABLE_WIFI)) {
				objective = WTO_START_GPS_MONITOR;
			}
			else if(obj.equals("no_wifi") || obj.equals("shutdown")) {
				objective = -1;
			}
		}
		if(objective != -1) {
			startJob(objective);
		}
		else {
			stop_service_flag = true;
			locator.shutDown();
			hardware_monitor.shutDown();
			restart = START_NOT_STICKY;
		}

		// If we get killed, after returning from here, restart
		return restart;
	}
	
	public void startJob(int objective) {
		
		Message msg = service_handler.obtainMessage();
		
		msg.arg1 = objective;

		service_handler.sendMessage(msg);
	}

	/** We don't provide binding, so return null **/
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		debug("service done");
	}
}
