package edu.ucsb.geog.blakeregalia;

import android.content.Context;
import android.content.Intent;
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
public class Hardware_Monitor {

	public static final int GPS = (1 << 0);
	public static final int WIFI = (1 << 1);
	
	public static final int GPS_ENABLED_REQUEST_CODE = 0x0A;
	public static final String ENABLE_GPS = "enable_gps";
	public static final String ENABLE_WIFI = "enable_wifi";
	
	private static final boolean USE_GPS_TOGGLE_EXPLOIT_IF_AVAILABLE = false;
	
	/** objects */
	private Context context;
	
	private LocationManager location_manager;
	private WifiManager wifi_manager;
	
	/** fields */
	private boolean can_toggle_gps = false;
	
	public Hardware_Monitor(Context _context) {
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
		
		// callback the listener once gps is enabled
		/*
		Thread thread = (new Thread(new Runnable() {
			public void run() {
			/**/
				location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
					public void onLocationChanged(Location location) {}
					public void onProviderDisabled(String provider) {}
					public void onProviderEnabled(String provider) {
						
						System.out.println("--> gps enabled");
						
						location_manager.removeUpdates(this);
						callback(ready);
					}
					public void onStatusChanged(String provider, int status, Bundle extras) {}
				}, looper);
				/*
			}
		}));

		thread.setDaemon(true);
		thread.start();
		
		/**/
		
		
		// exploit a bug in the power manager widget
		final Intent poke = new Intent();
		poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider"); //$NON-NLS-1$//$NON-NLS-2$
		poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
		poke.setData(Uri.parse("3")); //$NON-NLS-1$
		context.sendBroadcast(poke);

		System.out.println("...");
	}	
	
	
	
	

	private boolean gps_enabled() {
		return location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}
	
	
	private void enable_gps_alert_dialog() {
		context.startActivity(
				new Intent(context, Alert_Enable_GPS.class)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.putExtra("objective", ENABLE_GPS)
			);
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
				 enable_gps_alert_dialog();
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
				enable_gps_alert_dialog();
				break;
			}
		}
	};
	
	
	private boolean wifi_enabled() {
		return wifi_manager.isWifiEnabled();
	}

	public void enable_wifi(final Runnable ready, final Runnable hardware_fail) {

		/**/
		if(context == null) System.err.println("context is null");
		if(Activity_Enable_WiFi.class == null) System.err.println("wifi_class is null");
		context.startActivity(
				new Intent(context, Activity_Enable_WiFi.class)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.putExtra("objective", ENABLE_WIFI)
			);
		/**/
		/**
		
		// wait for wifi to enable
		IntentFilter intent = new IntentFilter();
		intent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		context.registerReceiver(new BroadcastReceiver() {
			public void onReceive(Context c, Intent i) {
				context.unregisterReceiver(this);
				
				// if wifi was enabled successfully... 
				if(wifi_manager.isWifiEnabled()) {
					// notify the listener
					callback(ready);
				}
				// hardware could not be enabled
				else {
					// notify the listener
					callback(hardware_fail);
				}
			}
		}
		, intent);

		// enable the wifi 
		wifi_manager.setWifiEnabled(true);
		/**/
	}
	
	public void shutDown() {
		
	}
	
}
