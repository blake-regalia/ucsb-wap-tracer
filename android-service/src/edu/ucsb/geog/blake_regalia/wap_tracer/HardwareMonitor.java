package edu.ucsb.geog.blake_regalia.wap_tracer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

/**
 * This class is responsible for monitoring and starting hardware interfaces
 * @author Blake
 *
 */
public class HardwareMonitor {

	private BroadcastReceiver wifiInterestReceiver;

	public static final int GPS = (1 << 0);
	public static final int WIFI = (1 << 1);

	public static final int GPS_ENABLED_REQUEST_CODE = 0x0A;
	public static final String ENABLE_GPS = "enable_gps";
	public static final String ENABLE_WIFI = "enable_wifi";

	private static final boolean USE_GPS_TOGGLE_EXPLOIT_IF_AVAILABLE = true;
	
	private static final String TAG = "HardwareMonitor";

	/** objects */
	private Context mContext;

	private LocationManager mLocationManager;
	private WifiManager mWifiManager;
	
	private LocationListener gpsInterestListener;

	/** fields */
	private boolean can_toggle_gps = false;

	public HardwareMonitor(Context _context) {
		mContext = _context;

		can_toggle_gps = gps_toggle_exploit_available() && USE_GPS_TOGGLE_EXPLOIT_IF_AVAILABLE;

		// Acquire a reference to the system Location Manager
		mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

		// Acquire a reference to the system Wifi Manager
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
	}



	private void callback(Runnable callback) {
		Thread thread = new Thread(callback);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}


	private boolean gps_toggle_exploit_available() {
		PackageManager pacman = mContext.getPackageManager();
		PackageInfo pacInfo = null;

		try {
			pacInfo = pacman.getPackageInfo("com.android.settings", PackageManager.GET_RECEIVERS);
		} catch (NameNotFoundException e) {
			// package not found 
			return false;
		}

		if(pacInfo != null){
			for(ActivityInfo actInfo : pacInfo.receivers){
				// test if receiver is exported. if so, we can toggle GPS
				if(actInfo.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && actInfo.exported){
					return true;
				}
			}
		}

		return false;
	}


	private void toggleGps(final Runnable ready, final Runnable hw_fail) {
		
		gpsInterestListener = new LocationListener() {
			public void onLocationChanged(Location location) {}
			public void onProviderDisabled(String provider) {}
			public void onProviderEnabled(String provider) {
				unbindGps();
				callback(ready);
			}
			public void onStatusChanged(String provider, int status, Bundle extras) {}
		};

		// listen for as soon as the gps is enabled
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsInterestListener);

		// exploit a bug in the power manager widget
		final Intent poke = new Intent();
		poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider"); //$NON-NLS-1$//$NON-NLS-2$
		poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
		poke.setData(Uri.parse("3")); //$NON-NLS-1$
		mContext.sendBroadcast(poke);

		Timeout.setTimeout(new Runnable() {
			public void run() {
				unbindGps();
				(new Thread(hw_fail)).start();
			}
		}, 5000);
	}	



	private void investGpsInterest(final Runnable disabled) {
		
		gpsInterestListener = new LocationListener() {			
			public void onLocationChanged(Location location) {}
			
			public void onProviderDisabled(String provider) {
				unbindGps();
				callback(disabled);
			}
			
			public void onProviderEnabled(String provider) {}
			public void onStatusChanged(String provider, int status, Bundle extras) {}
		};

		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsInterestListener);
	}

	
	/**
	 * Removes location updates for any current GPS listener
	 * @return null always.
	 */
	public LocationListener unbindGps() {
		if(gpsInterestListener != null) {
			mLocationManager.removeUpdates(gpsInterestListener);
			gpsInterestListener = null;
		}
		return null;
	}
	
	
	
	
	private int timeout_monitor_gps = -1;
	private long interval_monitor_gps = 0;
	private Runnable callback_monitor_gps_disabled;
	
	/**
	 * Periodically checks the hardware state of the GPS without binding any listeners.
	 */
	public void monitorGpsState(Runnable disabled, long interval) {
		Timeout.clearTimeout(timeout_monitor_gps);
		interval_monitor_gps = interval;
		callback_monitor_gps_disabled = disabled;
		timeout_monitor_gps = Timeout.setTimeout(check_gps_state, interval);
	}

	/**
	 * Stops checking the hardware state of the GPS 
	 */
	public void ignoreGpsState() {
		timeout_monitor_gps = Timeout.clearTimeout(timeout_monitor_gps);
	}
	
	private Runnable check_gps_state = new Runnable() {
		public void run() {
			if(isGpsEnabled()) {
				timeout_monitor_gps = Timeout.setTimeout(check_gps_state, interval_monitor_gps);
			}
			else {
				callback(callback_monitor_gps_disabled);
			}
		}
	};
	
	
	
	

	private boolean isGpsEnabled() {
		return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}
	
	/**
	 * Attempts to enable GPS programatically. If GPS is already enabled, it will listen for updates in case it gets disabled
	 * @param enabled		callback to run when GPS is/turns on
	 * @param disabled		callback to run if GPS gets disabled later
	 * @param failed		callback to run if it was unable to enable GPS programatically
	 */
	public void enableGps(final Runnable enabled, final Runnable disabled, final Runnable failed) {
		if(isGpsEnabled()) {
			investGpsInterest(disabled);
			callback(enabled);
			return;
		}
		
		if(can_toggle_gps) {
			toggleGps(enabled, failed);
		}
		else {
			callback(failed);
		}
	}
	
	private static int previousWifiState;
	
	
	
	/**
	 * Sets up a broadcastReceiver to listen for wifi hardware getting disabled
	 * 
	 * @param disabled	Callback listener to be fired if the wifi hardware is disabled
	 */
	private synchronized void investWifiInterest(final Runnable disabled) {
		IntentFilter wifiInterestIntent = new IntentFilter();
		wifiInterestIntent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		
		if(wifiInterestReceiver != null) {
			mContext.unregisterReceiver(wifiInterestReceiver);
		}
		
		wifiInterestReceiver = new BroadcastReceiver() {
			
			public void onReceive(Context context, Intent intent) {
				
				switch(mWifiManager.getWifiState()) {
				
				case WifiManager.WIFI_STATE_DISABLED:
				case WifiManager.WIFI_STATE_DISABLING:
				case WifiManager.WIFI_STATE_UNKNOWN:
					mContext.unregisterReceiver(wifiInterestReceiver);
					callback(disabled);
					break;
				}
			}
		};

		mContext.registerReceiver(wifiInterestReceiver, wifiInterestIntent);
	}
	

	private boolean wifi_enabled() {
		return mWifiManager.isWifiEnabled();
	}
	
	public void enable_wifi(final Runnable enabled, final Runnable disabled, final Runnable failed) {
		if(wifi_enabled()) {
			investWifiInterest(disabled);
			callback(enabled);
			return;
		}
		
		// fetch the initial state of the wifi to determine what to do later
		final int initialState = mWifiManager.getWifiState();
		previousWifiState = initialState; 
		
		switch(initialState) {

		case WifiManager.WIFI_STATE_ENABLED:
		case WifiManager.WIFI_STATE_ENABLING:
			Log.d(TAG, "WiFi interface is enabling...");
			break;
			
		case WifiManager.WIFI_STATE_DISABLED:
		case WifiManager.WIFI_STATE_DISABLING:
			Log.d(TAG, "WiFi interface is disabled");
			break;
			
		case WifiManager.WIFI_STATE_UNKNOWN:
			callback(failed);
			return;
		}

		// wait for wifi to enable
		IntentFilter intent = new IntentFilter();
		intent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		
		mContext.registerReceiver(new BroadcastReceiver() {
			
			public void onReceive(Context c, Intent i) {
				int state = mWifiManager.getWifiState();
				Log.d(TAG, "WiFi is now: "+WifiController.wifiStateToString(state)+", previously: "+WifiController.wifiStateToString(previousWifiState));
				
				switch(state) {
				
				case WifiManager.WIFI_STATE_ENABLED:
					investWifiInterest(disabled);
					mContext.unregisterReceiver(this);
					callback(enabled);
					return;
					
				case WifiManager.WIFI_STATE_ENABLING:
					Log.d(TAG, "Enabling WiFi interface...");
					break;
					
				case WifiManager.WIFI_STATE_DISABLED:
					switch(previousWifiState) {
					case WifiManager.WIFI_STATE_DISABLED:
					case WifiManager.WIFI_STATE_DISABLING:
						break;
					default:
						Log.e(TAG, "WiFi failed to start");
						mContext.unregisterReceiver(this);
						callback(failed);
						break;
					}
					break;
					
				case WifiManager.WIFI_STATE_DISABLING:
					if(previousWifiState != WifiManager.WIFI_STATE_DISABLING) {
						Log.e(TAG, "WiFi disabling when not supposed to");
						mContext.unregisterReceiver(this);
						callback(failed);
					}
					break;
					
				case WifiManager.WIFI_STATE_UNKNOWN:
					Log.w(TAG, "WiFi state went to unknown!");
					break;
				}
				
				previousWifiState = state;
			}
		}
		, intent);

		// enable the wifi 
		mWifiManager.setWifiEnabled(true);
	}

	public void shutDown() {

	}

}
