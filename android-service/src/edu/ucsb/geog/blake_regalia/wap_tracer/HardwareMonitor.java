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

/**
 * This class is responsible for monitoring and starting hardware interfaces
 * @author Blake
 *
 */
public class HardwareMonitor {

	public static final int GPS = (1 << 0);
	public static final int WIFI = (1 << 1);

	public static final int GPS_ENABLED_REQUEST_CODE = 0x0A;
	public static final String ENABLE_GPS = "enable_gps";
	public static final String ENABLE_WIFI = "enable_wifi";

	private static final boolean USE_GPS_TOGGLE_EXPLOIT_IF_AVAILABLE = true;

	/** objects */
	private Context context;

	private LocationManager location_manager;
	private WifiManager wifi_manager;

	/** fields */
	private boolean can_toggle_gps = false;

	public HardwareMonitor(Context _context) {
		context = _context;

		can_toggle_gps = gps_toggle_exploit_available() && USE_GPS_TOGGLE_EXPLOIT_IF_AVAILABLE;

		// Acquire a reference to the system Location Manager
		location_manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		// Acquire a reference to the system Wifi Manager
		wifi_manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	}



	private void callback(Runnable callback) {
		Thread thread = new Thread(callback);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}


	private boolean gps_toggle_exploit_available() {
		PackageManager pacman = context.getPackageManager();
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


	private void toggle_gps(final Runnable ready, final Runnable hw_fail, Looper looper) {

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
		location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, looper);		

		// exploit a bug in the power manager widget
		final Intent poke = new Intent();
		poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider"); //$NON-NLS-1$//$NON-NLS-2$
		poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
		poke.setData(Uri.parse("3")); //$NON-NLS-1$
		context.sendBroadcast(poke);

		System.out.println("...");
		
		Timeout.setTimeout(new Runnable() {
			public void run() {
				location_manager.removeUpdates(locationListener);
				(new Thread(hw_fail)).start();
			}
		}, 5000);
	}	





	private boolean gps_enabled() {
		return location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}


	private void enable_gps_alert_dialog(Runnable gps_not_enabled) {
		Thread thread = new Thread(gps_not_enabled);
		thread.run();
		/*
		context.startActivity(
				new Intent(context, Alert_Enable_GPS.class)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.putExtra("objective", MainService.INTENT_OBJECTIVE.START_GPS)
				);
				*/
	}

	public boolean is_enabled(int hardware) {
		boolean enabled = true;
		if((GPS & hardware) != 0) {
			enabled &= gps_enabled();
		}
		if((WIFI & hardware) != 0) {
			enabled &= wifi_enabled();
		}
		return enabled;
	}

	public boolean enable(int hardware, Runnable ready, Runnable hardware_fail, Looper looper) {
		boolean rx = true;
		if((GPS & hardware) != 0) {
			if(gps_enabled()) {
				rx &= true;
			}
			else {
				if(can_toggle_gps) {
					toggle_gps(ready, hardware_fail, looper);
				}
				else {
					enable_gps_alert_dialog(hardware_fail);
				}
				rx &= false;
			}
		}
		if((WIFI & hardware) != 0) {
			if(wifi_enabled()) {
				rx &= true;
			}
			else {
				enable_wifi(ready, hardware_fail);
				rx &= false;
			}
		}
		return rx;
	}

	GpsStatus.Listener gps_listener = new GpsStatus.Listener() {
		public void onGpsStatusChanged(int event) {
			System.out.println("GpsStatus changed");
			switch(event) {
			case GpsStatus.GPS_EVENT_STOPPED:
				//enable_gps_alert_dialog();
				break;
			}
		}
	};


	private boolean wifi_enabled() {
		return wifi_manager.isWifiEnabled();
	}

	
	private static int previousWifiState;
	
	public void enable_wifi(final Runnable ready, final Runnable hardware_fail) {

		final int initialState = wifi_manager.getWifiState();
		switch(initialState) {
		case WifiManager.WIFI_STATE_ENABLING:
		case WifiManager.WIFI_STATE_ENABLED:
			System.out.println("WiFi interface is enabling...");
			break;
		case WifiManager.WIFI_STATE_DISABLED:
		case WifiManager.WIFI_STATE_DISABLING:
			System.out.println("WiFi interface is disabled\n");
			break;
		case WifiManager.WIFI_STATE_UNKNOWN:
			callback(hardware_fail);
			return;
		}
		
		previousWifiState = initialState; 

		// wait for wifi to enable
		IntentFilter intent = new IntentFilter();
		intent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		context.registerReceiver(new BroadcastReceiver() {
			public void onReceive(Context c, Intent i) {

				int state = wifi_manager.getWifiState();

				System.out.println("WiFi is now: "+WifiController.wifiStateToString(state)+", previously: "+WifiController.wifiStateToString(previousWifiState));
				
				switch(state) {
				case WifiManager.WIFI_STATE_ENABLED:
					context.unregisterReceiver(this);
					callback(ready);
					return;
				case WifiManager.WIFI_STATE_ENABLING:
					System.out.println("Enabling WiFi interface...\n");
					break;
				case WifiManager.WIFI_STATE_DISABLED:
					switch(previousWifiState) {
					case WifiManager.WIFI_STATE_DISABLED:
					case WifiManager.WIFI_STATE_DISABLING:
						break;
					default:
						System.out.println("ERROR: WiFi disabled, couldn't start it!");
						context.unregisterReceiver(this);
						callback(hardware_fail);
						break;
					}
					break;
				case WifiManager.WIFI_STATE_DISABLING:
					if(previousWifiState != WifiManager.WIFI_STATE_DISABLING) {
						System.out.println("ERROR: WiFi disabling when not supposed to.");
						callback(hardware_fail);
					}
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
