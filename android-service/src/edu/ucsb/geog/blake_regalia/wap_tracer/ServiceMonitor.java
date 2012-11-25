package edu.ucsb.geog.blake_regalia.wap_tracer;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

public abstract class ServiceMonitor {
	
	protected static final String TAG = "ServiceMonitor";
	
	/** context-free constants: broadcast updates define what the service is telling an activity about **/
	public static final String BROADCAST_UPDATES = ServiceMonitor.class.getPackage().getName()+".UPDATES";
	public static class UPDATES {
		public static final String TRACING = "tracing";
		public static final String SIMPLE = "simple";
		public static final String UPLOADED = "upload";
		public static final String OUT_OF_BOUNDS = "bounds";
		public static final String LOCATION_UNKNOWN = "location";
		public static final String GPS_LOST = "gps lost";
		public static final String SLEEPING = "sleeping";
		public static final String GPS_EXPIRED = "gps expired";
	}

	protected Context mContext;
	protected Looper mMainThread;
	
	protected enum ActivityIntent {
		WIFI_FAIL,
		WIFI_DISABLED,
		GPS_ENABLE,
		GPS_DISABLED,
		GPS_SIGNAL_WEAK,
		GPS_SIGNAL_LOST,
		GPS_AGE_TOO_OLD,
	}
	
	public ServiceMonitor(Context context, Looper looper) {
		mContext = context;
		mMainThread = looper;
	}

	public abstract void start();
	public abstract void resume();
	public abstract void stop();
	
	protected void notifyUser(ActivityIntent reason) {
		
		switch(reason) {

		case WIFI_FAIL:
			NotificationInterface.post(mContext, ActivityAlertUser.class, "wifi-fail", false, "This app can't run on your device", "Touch here for more information");			
			break;
			
		case WIFI_DISABLED:
			NotificationInterface.post(mContext, ActivityControl.class, "wifi-disabled", false, "Wi-Fi was disabled", "Tracer has shut down", 0);			
			break;

		case GPS_ENABLE:
			NotificationInterface.post(mContext, ActivityAlertUser.class, "gps-enable", false, "PROVIDER_GPS needs to be enabled", "PROVIDER_GPS must remain enabled for app to run");
			break;

		case GPS_DISABLED:
			NotificationInterface.post(mContext, ActivityControl.class, "gps-disabled", false, "PROVIDER_GPS was disabled", "Tracer has shut down", 0);
			break;

		case GPS_SIGNAL_WEAK:
			broadcast(UPDATES.LOCATION_UNKNOWN);
			break;
			
		case GPS_SIGNAL_LOST:
			broadcast(UPDATES.GPS_LOST);
			break;
			
		case GPS_AGE_TOO_OLD:
			broadcast(UPDATES.GPS_EXPIRED);
			break;
		}
	}
	
	/** broadcasts a message of the given type, for Activities to receive updates */
	protected void broadcast(String type) {
		Intent intent = new Intent(BROADCAST_UPDATES);
		intent.putExtra("type", type);
        mContext.sendBroadcast(intent);
	}
	
	/** broadcasts a message of the given type with extra string, for Activities to receive updates */
	protected void broadcast(String type, String extra) {
		Intent intent = new Intent(BROADCAST_UPDATES);
		intent.putExtra("type", type);
		intent.putExtra(type, extra);
		mContext.sendBroadcast(intent);
	}

}
