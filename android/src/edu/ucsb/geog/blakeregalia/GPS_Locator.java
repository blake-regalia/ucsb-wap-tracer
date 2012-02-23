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
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

public class GPS_Locator {
	
	LocationManager location_manager;
	ucsb_wap_activity main;
	Context context;
	
	Hardware_Ready_Listener stored_callback = null;
	
	boolean can_toggle_gps = false;
	int gps_exploit_attempts = 0;
	
	public GPS_Locator(ucsb_wap_activity activity) {
		main = activity;
		context = (Context) main;
		
		// Acquire a reference to the system Location Manager
		location_manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		can_toggle_gps = gps_toggle_exploit_available() && false;
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
					if(gps_exploit_attempts < 25) {

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
						/* wait 200 milliseconds in between each check */
						Thread.sleep(200);
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

	private void unused() {
		// Define a listener that responds to location updates
		LocationListener location_listener = new LocationListener() {
			public void onLocationChanged(Location location) {
				/* Called when a new location is found by the network location provider. */
				//makeUseOfNewLocation(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {}

			public void onProviderEnabled(String provider) {}

			public void onProviderDisabled(String provider) {}

		};

		// Register the listener with the Location Manager to receive location updates
		location_manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, location_listener);
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
}
