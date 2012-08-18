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

	private LocationManager location_manager;
	private WifiManager wifi_manager;

	/** fields */
	private boolean can_toggle_gps = false;

	public HardwareMonitor(Context _context) {
		mContext = _context;

		can_toggle_gps = gps_toggle_exploit_available() && USE_GPS_TOGGLE_EXPLOIT_IF_AVAILABLE;

		// Acquire a reference to the system Location Manager
		location_manager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

		// Acquire a reference to the system Wifi Manager
		wifi_manager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
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


	private void toggle_gps(final Runnable ready, final Runnable hw_fail) {

		System.out.println(">/<: GPS");
		
		final LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {}
			public void onProviderDisabled(String provider) {}
			public void onProviderEnabled(String provider) {

				System.out.println("--> gps enabled");

				location_manager.removeUpdates(this);
				callback(ready);
			}
			public void onStatusChanged(String provider, int status, Bundle extras) {}
		};

		// listen for as soon as the gps is enabled
//		location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, looper);
		location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

		// exploit a bug in the power manager widget
		final Intent poke = new Intent();
		poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider"); //$NON-NLS-1$//$NON-NLS-2$
		poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
		poke.setData(Uri.parse("3")); //$NON-NLS-1$
		mContext.sendBroadcast(poke);

		System.out.println("...");
		
		Timeout.setTimeout(new Runnable() {
			public void run() {
				location_manager.removeUpdates(locationListener);
				(new Thread(hw_fail)).start();
			}
		}, 5000);
	}	



	private void investGpsInterest(final Runnable disabled) {
		
		final LocationListener gpsInterestListener = new LocationListener() {			
			public void onLocationChanged(Location location) {}
			
			public void onProviderDisabled(String provider) {
				location_manager.removeUpdates(this);
				callback(disabled);
			}
			
			public void onProviderEnabled(String provider) {}
			public void onStatusChanged(String provider, int status, Bundle extras) {}
		};

		location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsInterestListener);
	}


	private boolean gps_enabled() {
		return location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}
	
	public void enable_gps(final Runnable enabled, final Runnable disabled, final Runnable failed) {
		if(gps_enabled()) {
			investGpsInterest(disabled);
			callback(enabled);
			return;
		}
		
		if(can_toggle_gps) {
			toggle_gps(enabled, failed);
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
				
				switch(wifi_manager.getWifiState()) {
				
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
		return wifi_manager.isWifiEnabled();
	}
	
	public void enable_wifi(final Runnable enabled, final Runnable disabled, final Runnable failed) {
		if(wifi_enabled()) {
			investWifiInterest(disabled);
			callback(enabled);
			return;
		}
		
		// fetch the initial state of the wifi to determine what to do later
		final int initialState = wifi_manager.getWifiState();
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
				int state = wifi_manager.getWifiState();
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
		wifi_manager.setWifiEnabled(true);
	}

	public void shutDown() {

	}

}
