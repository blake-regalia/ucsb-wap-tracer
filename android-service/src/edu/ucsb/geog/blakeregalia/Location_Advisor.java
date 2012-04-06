package edu.ucsb.geog.blakeregalia;

import java.util.Date;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class Location_Advisor {
	
	public static final int GPS = (1 << 0);
	public static final int NETWORK = (1 << 1);

	// minimum 100m accuracy
	protected static final int BOUNDARY_CHECK_GPS_MIN_ACCURACY_M = 100;
	// good enough for boundary check: 60m accuracy
	protected static final int BOUNDARY_CHECK_GPS_GOOD_ACCURACY_M = 60;
	// maximum 30s old fix
	protected static final long BOUNDARY_CHECK_GPS_MAX_AGE_MS = 30 * 1000;
	// max number of location updates
	protected static final int BOUNDARY_CHECK_MAX_NUM_EVENTS = 7;
	// timeout duration: how long to wait for a good location (12s)
	private static final long BOUNDARY_CHECK_TIMEOUT_MS = 12 * 1000;
	
	private boolean use_gps = false;
	private boolean use_network = false;
	
	private Context context;
	
	private LocationManager location_manager;
	
	private Location last_location = null;
	
	private LocationListener active_gps_listener = null;
	private int active_gps_location_events = 0;

	private Runnable callback_pending_location_fix = null;
	private Runnable callback_incaseof_hardware_fail = null;
	

	public Location_Advisor(Context _context) {
		context = _context;

		// Acquire a reference to the system Location Manager
		location_manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}
	
	public boolean useProvider(int provider) {
		use_gps = ((GPS & provider) != 0);
		use_network = ((NETWORK & provider) != 0);
		
		return false;
	}
	
	
	public Location obtainLocation(final Handler service_handler, final Message msg) {
		Runnable code = new Runnable() { 
			public void run() {
				service_handler.sendMessage(msg);
			}
		};
		
		if(use_gps) {
	//		obtainGpsLocation(code);
		}
		return null;
	}
	
	
	
	public Location getLocation() {
		return last_location;
	}

	public void obtainGpsLocation(final Runnable code, final Runnable disabled) {
		callback_pending_location_fix = code;
		callback_incaseof_hardware_fail = disabled;
		
		System.out.println("gps location requested");
		
		if(active_gps_listener == null) {
			
			System.out.println("active gps_listener = boundary check");
			
			set_active_gps_listener(boundary_check_gps_listener);
			//location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, active_gps_listener, this.context.getMainLooper());
			if(location_manager == null) {
				System.out.println("location manager is null");
			}
			if(active_gps_listener == null) {
				System.out.println("active gps listener is null");
			}
			System.out.println("** starting request updates");
			location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, active_gps_listener);
		}
		
		System.out.println("waiting for updates...");
	}
	
	private void set_active_gps_listener(LocationListener gps_listener) {
		active_gps_listener = gps_listener;
		active_gps_location_events = 0;
	}
	
	private void stop_gps_listener() {
		System.out.println("Location_Advisor: aborting gps listener");
		location_manager.removeUpdates(active_gps_listener);
	}
	
	private Thread timeout_thread; 
	
	private void callback(Runnable callback) {
		Thread thread = new Thread(callback);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}
	
	private void stop_timeout() {
		if(timeout_thread != null) {
			timeout_thread.interrupt();
			timeout_thread = null;
		}
	}
	
	private void location_fixed() {
		stop_gps_listener();
		stop_timeout();
		callback(callback_pending_location_fix);
		callback_pending_location_fix = null;
	}
	
	private void hardware_fail() {
		System.out.println("Location_Advisor: hardware fail");
		stop_gps_listener();
		stop_timeout();
		callback(callback_incaseof_hardware_fail);
		callback_incaseof_hardware_fail = null;
	}
	
	public LocationListener boundary_check_gps_listener = new LocationListener() {
		public void onLocationChanged(Location location) {

		System.out.println("location change event: "+active_gps_location_events);
		Toast.makeText(context, "location change event: "+active_gps_location_events, Toast.LENGTH_SHORT).show();
			
			if(!location.hasAccuracy()) {
				System.out.println("### location has no accuracy");
				// no accuracy!?
				return;
			}

			long expiration = ((new Date()).getTime() - BOUNDARY_CHECK_GPS_MAX_AGE_MS);
			
			
			// if this location is accurate enough
			if(location.getAccuracy() <= BOUNDARY_CHECK_GPS_MIN_ACCURACY_M
				// and new enough
					&& (location.getTime() > expiration)) {
				
		System.out.println("location is accurate & new enough to use");
				
				// save it if we don't have one saved
				if(last_location == null
					// or if the one we have is too old
						|| (last_location.getTime() > expiration)
					// or if this location is more accurate
						|| (last_location.getAccuracy() <= location.getAccuracy()) ) {

		System.out.println("location was saved");
					last_location = location;
				}
			}

			if(last_location.getAccuracy() <= BOUNDARY_CHECK_GPS_GOOD_ACCURACY_M) {
				
		System.out.println("location is good enough for boundary check");
		
				location_fixed();
			}
			// if location listener has been called several times..
			else if(active_gps_location_events >= BOUNDARY_CHECK_MAX_NUM_EVENTS) {
				
		System.out.println("location changed enough times, using best location");
				
				location_fixed();
			}
			
			stop_timeout();
			// start a timeout thread to return the best location after a period of inactivity
			timeout_thread = new Thread(boundary_check_timeout);
			timeout_thread.start();
			
			active_gps_location_events += 1;
		}
		public void onProviderDisabled(String arg0) {
			hardware_fail();
		}
		public void onProviderEnabled(String arg0) {
		}
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			String str = "unknown";
			switch(arg1) {
			case LocationProvider.OUT_OF_SERVICE:
				str = "out of service";
				break;
			case LocationProvider.AVAILABLE:
				str = "available";
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				str = "temporarily unavailable";
				break;
			}
			
			Toast.makeText(context, "status change event: "+str, Toast.LENGTH_SHORT).show();
			System.out.println("status change event: "+str);
			
		}
	};
	
	private Runnable boundary_check_timeout = new Runnable() {
		public void run() {
			long end_time = System.currentTimeMillis() + BOUNDARY_CHECK_TIMEOUT_MS;
			while (System.currentTimeMillis() < end_time) {
				try {
					wait(end_time - System.currentTimeMillis());
				} catch (Exception e) {}
			}
			location_fixed();
		}
	};
	
	public void shutDown() {
		stop_gps_listener();
		stop_timeout();
	}

}
