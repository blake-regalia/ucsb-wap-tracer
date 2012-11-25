package edu.ucsb.geog.blake_regalia.wap_tracer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

/**
 * @author Blake
 *
 */
/**
 * @author Blake
 *
 */
public class HardwareManager {

	/** static */
	private static final boolean USE_GPS_TOGGLE_EXPLOIT_IF_AVAILABLE = true;
	
	public static final int RESOURCE_GPS = (1 << 0);
	public static final int RESOURCE_WIFI = (1 << 1);

	private static final String TAG = "HardwareManager";
	
	private static int previousWifiState;

	
	/** fields */
	private boolean gpsToggleAvailable = false;
	private int gpsEventTimeout = -1;


	
	/** objects */
	private Context mContext;

	private LocationManager mLocationManager;
	private WifiManager mWifiManager;

	private LocationListener gpsInterestListener;
	private BroadcastReceiver wifiInterestReceiver;
	
	
	/** constructor */
	public HardwareManager(Context context) {
		mContext = context;

		gpsToggleAvailable = gpsToggleExploitAvailable() && USE_GPS_TOGGLE_EXPLOIT_IF_AVAILABLE;
		
		// Acquire a reference to the system Location Manager
		mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

		// Acquire a reference to the system Wifi Manager
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
	}
	
	
	/** methods */
	public void setGpsToggleExploitMotivation(boolean motivation) {
		gpsToggleAvailable = gpsToggleExploitAvailable() && motivation;
	}
	
	public void enable(int resource, HardwareCallback callback) {
		enable(resource, callback, -1);
	}
	
	public void enable(int resource, HardwareCallback callback, int index) {
		switch(resource) {
		case RESOURCE_GPS:
			enableGps(callback, index);
			break;
			
		case RESOURCE_WIFI:
			enableWiFi(callback, index);
			break;
		}
	}
	

	/**
	 * Attempts to enable PROVIDER_GPS programatically. If PROVIDER_GPS is already enabled, it will listen for updates in case it gets disabled
	 * @param enabled		callback to run when PROVIDER_GPS is/turns on
	 * @param disabled		callback to run if PROVIDER_GPS gets disabled later
	 * @param failed		callback to run if it was unable to enable PROVIDER_GPS programatically
	 */
	private void enableGps(HardwareCallback callback, int index) {
		if(isGpsEnabled()) {
			investGpsInterest(callback, index);
			callback.hardwareEnabled(index);
			return;
		}
		
		if(gpsToggleAvailable) {
			toggleGps(callback, index);
		}
		else {
			callback.hardwareFailedToEnable(index, HardwareCallback.REASON_GPS_USER);
		}
	}

	/**
	 * returns the status of the PROVIDER_GPS hardware: enabled/disabled
	 * @return		
	 */
	private boolean isGpsEnabled() {
		return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	/**
	 * takes an interest in monitoring the enabled state of the PROVIDER_GPS
	 * @param disabled
	 */
	private void investGpsInterest(final HardwareCallback callback, final int index) {
		
		// create a listener for event: PROVIDER_GPS is disabled
		gpsInterestListener = new LocationListener() {			
			public void onLocationChanged(Location location) {}
			public void onProviderEnabled(String provider) {}
			public void onStatusChanged(String provider, int status, Bundle extras) {}
			
			// if PROVIDER_GPS gets disabled
			public void onProviderDisabled(String provider) {
				unbindGps();
				callback.hardwareDisabled(index, HardwareCallback.REASON_GPS_USER);
			}
		};

		// register listener & request updates
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsInterestListener);
	}

	/**
	 * @return		whether or not the toggle exploit is available
	 */
	private boolean gpsToggleExploitAvailable() {
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
				// test if receiver is exported. if so, we can toggle PROVIDER_GPS
				if(actInfo.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && actInfo.exported){
					return true;
				}
			}
		}

		return false;
	}

	
	private void pokeGpsExploit(final HardwareCallback callback, final int index) {
		Timeout.clearTimeout(gpsEventTimeout);
		
		// exploit a bug in the power manager widget
		final Intent poke = new Intent();
		poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider"); //$NON-NLS-1$//$NON-NLS-2$
		poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
		poke.setData(Uri.parse("3")); //$NON-NLS-1$
		mContext.sendBroadcast(poke);

		// sit & wait for event, otherwise trigger backup safety time-out
		gpsEventTimeout = Timeout.setTimeout(new Runnable() {
			public void run() {
				gpsEventTimeout = -1;
				unbindGps();
				callback.hardwareFailedToEnable(index, HardwareCallback.REASON_GPS_TOGGLE_EXPLOIT_ATTEMPT);
			}
		}, 5000);
	}

	/**
	 * attempt to toggle PROVIDER_GPS: enabled/disabled
	 * @param ready
	 * @param hw_fail
	 */
	private void toggleGps(final HardwareCallback callback, final int index) {
		
		// create a listener for event: PROVIDER_GPS is enabled
		gpsInterestListener = new LocationListener() {
			public void onLocationChanged(Location location) {}
			public void onStatusChanged(String provider, int status, Bundle extras) {}
			
			// if PROVIDER_GPS was accidentally toggled OFF
			public void onProviderDisabled(String provider) {
				pokeGpsExploit(callback, index);
			}

			// when PROVIDER_GPS becomes enabled 
			public void onProviderEnabled(String provider) {
				unbindGps();
				callback.hardwareEnabled(index);
			}
		};

		// register listener & request updates
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsInterestListener);

		// toggle PROVIDER_GPS programatically
		pokeGpsExploit(callback, index);
	}	



	
	/**
	 * Removes location updates for any current PROVIDER_GPS listener
	 * @return		null always.
	 */
	public LocationListener unbindGps() {
		if(gpsInterestListener != null) {
			mLocationManager.removeUpdates(gpsInterestListener);
			gpsInterestListener = null;
		}
		return null;
	}
	
	
	
	
	
	
	
	
	/**
	 * Sets up a broadcastReceiver to listen for wifi hardware getting disabled
	 * 
	 * @param disabled	Callback listener to be fired if the wifi hardware is disabled
	 */
	private synchronized void investWifiInterest(final HardwareCallback callback, final int index) {
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
					callback.hardwareDisabled(index, HardwareCallback.REASON_WIFI_USER);
					break;
				}
			}
		};

		mContext.registerReceiver(wifiInterestReceiver, wifiInterestIntent);
	}

	public String wifiStateToString(int state) {
		switch(state) {
		case WifiManager.WIFI_STATE_ENABLED:
			return "enabled";
		case WifiManager.WIFI_STATE_ENABLING:
			return "enabling";
		case WifiManager.WIFI_STATE_DISABLED:
			return "disabled";
		case WifiManager.WIFI_STATE_DISABLING:
			return "disabling";
		case WifiManager.WIFI_STATE_UNKNOWN:
			return "unknown";
		default:
			return "{other}";
		}
	}
	
	public void enableWiFi(final HardwareCallback callback, final int index) {
		if(mWifiManager.isWifiEnabled()) {
			investWifiInterest(callback, index);
			callback.hardwareEnabled(index);
			return;
		}
		
		// fetch initial state of WiFi hardware to determine what to do later
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
			callback.hardwareFailedToEnable(index, HardwareCallback.REASON_WIFI_UNABLE);
			return;
		}

		// wait for WiFi to enable
		IntentFilter intent = new IntentFilter();
		intent.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		
		mContext.registerReceiver(new BroadcastReceiver() {
			
			public void onReceive(Context c, Intent i) {
				int state = mWifiManager.getWifiState();
				Log.d(TAG, "WiFi is now: "+wifiStateToString(state)+", previously: "+wifiStateToString(previousWifiState));
				
				switch(state) {
				
				case WifiManager.WIFI_STATE_ENABLED:
					investWifiInterest(callback, index);
					mContext.unregisterReceiver(this);
					callback.hardwareEnabled(index);
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
						callback.hardwareFailedToEnable(index, HardwareCallback.REASON_WIFI_UNABLE);
						break;
					}
					break;
					
				case WifiManager.WIFI_STATE_DISABLING:
					if(previousWifiState != WifiManager.WIFI_STATE_DISABLING) {
						Log.e(TAG, "WiFi disabling when not supposed to");
						mContext.unregisterReceiver(this);
						callback.hardwareFailedToEnable(index, HardwareCallback.REASON_WIFI_USER);
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

		// attempt to enable WiFi hardware 
		mWifiManager.setWifiEnabled(true);
	}

	
	
	
}
