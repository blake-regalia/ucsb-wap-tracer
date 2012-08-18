package edu.ucsb.geog.blake_regalia.wap_tracer;

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

public class MainService extends Service {
	
	private static final String TAG = "MainService";

	public static boolean serviceRunning = false;
	private static boolean isTracing = false;
	private int wifiScanTimeout = -1;
	private long wakeTime;
	
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	private ServiceMonitor mWapTracer;
	private ServiceMonitor mEnvironmentMonitor;
	

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
		public static final int STOP_SERVICE    = 0xB;
	}
	
	/** context-free constants: intent objectives that define what just took place, for purpose of starting the service **/
	public static class INTENT_OBJECTIVE {
		public static final String START = "start";
		public static final String START_GPS  = "start gps";
		public static final String START_WIFI = "start wifi";
		public static final String FORCE_CHECK = "force check";
		public static final String STOP_SERVICE = "stop service";
		public static final String KILL_SERVICE = "kill service";
	}

	public static List<ScanResult> mWapList;
	public static Location mCurrentLocation;
	public static void updateStaticFieldsForWap(List<ScanResult> wapList, Location currentLocation) {
		mWapList = wapList;
		mCurrentLocation = currentLocation;
	}
	public static List<ScanResult> getLastWapScan() {
		return mWapList;
	}	
	public static Location getLastLocation() {
		return mCurrentLocation;
	}

	/** this gets called once the service is born **/
	@Override
	public void onCreate() {
		// Start up the thread running the service. we create a separate thread because the service normally runs in the process's
		// main thread, which we don't want to block (Activity's UI thread). we also make it background priority
		HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		Log.d("MainService::onCreate", "**create**");

		// Get the HandlerThread's Looper and use it for our Handler 
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper, this);
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
				objective = OBJECTIVE.START_GPS;
				NotificationInterface.clear(this);
			}
			// the service is starting up again because WIFI was just enabled
			else if(intentObjective.equals(INTENT_OBJECTIVE.START_WIFI)) {
				objective = OBJECTIVE.START_WIFI;
				NotificationInterface.clear(this);
			}
			else if(intentObjective.equals(INTENT_OBJECTIVE.STOP_SERVICE)) {
				objective = OBJECTIVE.STOP_SERVICE;
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
	
	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onDestroy() {
		serviceRunning = false; 
	}

	/** handler class receives messages from the thread  **/
	private final class ServiceHandler extends Handler {

		private Context mContext;
		private Looper mLooper;

		public ServiceHandler(Looper looper, Context context) {
			super(looper);
			mContext = context;
			mLooper = looper;
		}
		
		@Override
		public void handleMessage(Message msg) {
			int objective = msg.arg1;
			Log.d(TAG, "handleMessage("+objective+")");
			
			switch(objective) {

			case OBJECTIVE.INITIALIZE:
				mWapTracer = new WapTracer(mContext, mLooper);
				mEnvironmentMonitor = new EnvironmentMonitor(mContext, mLooper);
				break;

			case OBJECTIVE.START_GPS:
			case OBJECTIVE.START_WIFI:
				mWapTracer.resume();
				break;

			case OBJECTIVE.STOP_SERVICE:
				mWapTracer.stop();
				mEnvironmentMonitor.stop();
				break;
			
			default:
				Log.e(TAG, "Out of context: "+objective);
				return;
			}
		}
	}
}
