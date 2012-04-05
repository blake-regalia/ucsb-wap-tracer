package edu.ucsb.geog.blakeregalia;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

public class GPS_Locator {

	//	34.42316,-119.859295 (North-West corner)
	public final static class SOUTHWEST_CAMPUS_CORNER {
		public final static double LATITDUE = 34.403511;
		public final static double LONGITUDE = -119.881353;
	};
	
	// 10^6
	public final static int COORDINATE_PRECISION = 1000000; 

	private final static long TOGGLE_EXPLOIT_INTERVAL_MS = 200;
	/* wait for up to 5 seconds */
	private final static int TOGGLE_EXPLOIT_CHECK_NUMS = 25; 

	/* reject the old location after 15 seconds */
	private int EXPIRATION_TIME_MS = 15000;

	LocationManager location_manager;
	LocationListener location_listener = null;
	Location current_location;

	ucsb_wap_activity main;
	Context context;

	Hardware_Ready_Listener stored_callback = null;

	float best_accuracy = Float.MAX_VALUE;

	boolean can_toggle_gps = false;
	int gps_exploit_attempts = 0;

	boolean use_network_provider = false;

	public GPS_Locator(ucsb_wap_activity activity) {
		main = activity;
		context = (Context) main;

		// Acquire a reference to the system Location Manager
		location_manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		can_toggle_gps = gps_toggle_exploit_available();
	}

	private boolean gps_toggle_exploit_available() {
		PackageManager pacman = context.getPackageManager();
		PackageInfo pacInfo = null;

		try {
			pacInfo = pacman.getPackageInfo("com.android.settings", PackageManager.GET_RECEIVERS);
		} catch (NameNotFoundException e) {
			/* package not found */
			return false;
		}

		if(pacInfo != null){
			for(ActivityInfo actInfo : pacInfo.receivers){
				/* test if receiver is exported. if so, we can toggle GPS */
				if(actInfo.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && actInfo.exported){
					return true;
				}
			}
		}

		return false;
	}

	public void enableGps(Hardware_Ready_Listener callback) {
		/* if gps is enabled */
		if(location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			/* notify the listener */
			callback.onReady();
		}

		/* gps is disabled... */
		else {

			/* if the toggle exploit is available */
			if(can_toggle_gps) {

				/* and if the exploit is being tried */
				if(gps_exploit_attempts != 0) {

					/* try waiting up to 5 seconds */
					if(gps_exploit_attempts < TOGGLE_EXPLOIT_CHECK_NUMS) {

						/* and check again */
						enableGps(callback);
					}
					/* give up after that much time */
					else {
						can_toggle_gps = false;
						enableGps(callback);
					}
				}
				/* exploit hasn't been attempted yet, try it */
				else {
					main.debug("toggle exploit available! enabling gps");

					/* exploit a bug in the power manager widget */ 
					final Intent poke = new Intent();
					poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider"); //$NON-NLS-1$//$NON-NLS-2$
					poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
					poke.setData(Uri.parse("3")); //$NON-NLS-1$
					context.sendBroadcast(poke);

					gps_exploit_attempts += 1;
					try {
						/* wait n milliseconds in between each check */
						Thread.sleep(TOGGLE_EXPLOIT_INTERVAL_MS);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					/* perform the next check */
					enableGps(callback);
				}
			}

			/* the exploit is unavailable, ask the user to enable gps the old-fashioned way */ 
			else {
				stored_callback = callback;
				showAlertDialogToEnableGPS();
			}
		}
	}
	

	public byte[] test_encode() {
		ByteBuilder bytes = new ByteBuilder(8);
		
		double latitude = 34.415535 - SOUTHWEST_CAMPUS_CORNER.LATITDUE;
		double longitude = -119.845211 - SOUTHWEST_CAMPUS_CORNER.LONGITUDE;
		
		System.out.println(latitude);
		
		bytes.append_4(Encoder.encode_double_to_4_bytes(latitude, COORDINATE_PRECISION));
		bytes.append_4(Encoder.encode_double_to_4_bytes(longitude, COORDINATE_PRECISION));
		
		return bytes.getBytes();
	}
	
	public byte[] encode() {
		ByteBuilder bytes = new ByteBuilder(9);
		
		double latitude = current_location.getLatitude() - SOUTHWEST_CAMPUS_CORNER.LATITDUE;
		double longitude = current_location.getLongitude() - SOUTHWEST_CAMPUS_CORNER.LONGITUDE;
		int accuracy = Math.round(current_location.getAccuracy());
		if(accuracy > 255) {
			accuracy = 255;
		}
		
		bytes.append_4(Encoder.encode_double_to_4_bytes(latitude, COORDINATE_PRECISION));
		bytes.append_4(Encoder.encode_double_to_4_bytes(longitude, COORDINATE_PRECISION));
		bytes.append(Encoder.encode_byte(accuracy));
		
		return bytes.getBytes();
	}
	
	public byte[] encode(long time) {
		ByteBuilder bytes = new ByteBuilder(8+1+1+1);
		
		double latitude = current_location.getLatitude() - SOUTHWEST_CAMPUS_CORNER.LATITDUE;
		double longitude = current_location.getLongitude() - SOUTHWEST_CAMPUS_CORNER.LONGITUDE;
		int accuracy = Math.round(current_location.getAccuracy());
		if(accuracy > 255) {
			accuracy = 255;
		}
		
		// v3
		int time_difference = (int) ((time-current_location.getTime())/100);
		if(time_difference < 0) time_difference = 0;
		else if(time_difference > 255) time_difference = 255;
		bytes.append(Encoder.encode_byte(time_difference));
		
		bytes.append_4(Encoder.encode_double_to_4_bytes(latitude, COORDINATE_PRECISION));
		bytes.append_4(Encoder.encode_double_to_4_bytes(longitude, COORDINATE_PRECISION));
		
		//v4
		int altitude = (int) Math.round(current_location.getAltitude());
		bytes.append(Encoder.encode_byte(altitude));
		
		bytes.append(Encoder.encode_byte(accuracy));
		
		return bytes.getBytes();
	}

	private void showAlertDialogToEnableGPS() {
		AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(context);
		alert_dialog_builder.setMessage("GPS must be enabled to run this app. Please enable GPS")
		.setCancelable(false)
		.setPositiveButton("Okay",
				new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int id) {
				/* if gps was enabled since the alert was created */
				if(location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					/* notify the listener */
					stored_callback.onReady();
				}
				/*take the user to the settings menu */
				else {
					Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					main.startActivityForResult(intent, ucsb_wap_activity.GPS_ENABLED_REQUEST_CODE);
				}
			}
		}
				);
		AlertDialog alert = alert_dialog_builder.create();
		alert.show();
	}

	public void checkIfGPSWasEnabled(Intent data) {
		if(stored_callback == null) return;
		String provider = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
		if(provider != null){
			stored_callback.onReady();
		}
		else {
			stored_callback.onFail();
		}
	}

	public void waitForPositionFix(final Position_Fix_Listener callback, final float maximumErrorMeters) {
		if(location_listener == null) {
			/* define a listener that responds to location updates */
			location_listener = new LocationListener() {

				public void onLocationChanged(Location location) {
					current_location = location;
					if(location.getAccuracy() <= maximumErrorMeters) {
						main.debug("location fixed");

						best_accuracy = location.getAccuracy(); 
						location_manager.removeUpdates(location_listener);
						location_listener = gps_location_listener();
						location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, location_listener);
						if(use_network_provider) {
							location_manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, network_location_listener());
						}
						callback.onReady();
					}
					else {
						main.debug("waiting for accuracy within "+maximumErrorMeters+"m.\n"+location.getProvider()+" currently gives an accuracy of "+location.getAccuracy()+"m");
					}
				}

				public void onStatusChanged(String provider, int status_code, Bundle extras) {
					String status = "";
					switch(status_code) {
					case LocationProvider.AVAILABLE:
						status = "available";
					case LocationProvider.OUT_OF_SERVICE:
						status = "out of service";
					case LocationProvider.TEMPORARILY_UNAVAILABLE:
						status = "temporarily unavailable";
					}
					main.debug("status changed\n"+provider+" is "+status);
					if(status_code != LocationProvider.AVAILABLE) {
						callback.onFail();
					}
				}

				public void onProviderEnabled(String provider) {}

				public void onProviderDisabled(String provider) {}

			};

			/* register the listener with the Location Manager to receive location updates */
			location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, location_listener);
			if(use_network_provider) {
				location_manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, location_listener);
			}
		}
	}

	public void useNetworkProvider() {
		use_network_provider = true;
		if(location_listener != null) {
			location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, network_location_listener());
		}
	}

	private LocationListener gps_location_listener() {
		return new LocationListener() {
			public void onLocationChanged(Location location) {
				if((location.hasAccuracy() && location.getAccuracy() <= best_accuracy)
						//					|| (location.getProvider().equals(current_location.getProvider()))
						|| (location.getTime() - current_location.getTime() > EXPIRATION_TIME_MS)) {
					best_accuracy = location.getAccuracy();
					current_location = location;
				}
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}

			public void onProviderEnabled(String provider) {}

			public void onProviderDisabled(String provider) {}
		};
	}

	private LocationListener network_location_listener() {
		return new LocationListener() {
			public void onLocationChanged(Location location) {
				if((location.getAccuracy() <= best_accuracy)
						//					|| (location.getProvider().equals(current_location.getProvider()))
						|| (location.getTime() - current_location.getTime() > EXPIRATION_TIME_MS)) {
					best_accuracy = location.getAccuracy();
					current_location = location;
				}
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}

			public void onProviderEnabled(String provider) {}

			public void onProviderDisabled(String provider) {}
		};
	}

	public Location getCurrentLocation() {
		return new Location(current_location);
	}	

	public LatLng getCurrentLatLng() {
		return new LatLng(current_location.getLatitude(), current_location.getLongitude());
	}

	public interface Position_Fix_Listener {
		public void onReady();
		public void onFail();
	}

	public class LatLng {
		private double lat;
		private double lng;

		public LatLng(double latitude, double longitude) {
			lat = latitude;
			lng = longitude;
		}

		public double getLatitude() {
			return lat;
		}

		public double getLongitude() {
			return lng;
		}
	}
}
