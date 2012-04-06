package edu.ucsb.geog.blakeregalia;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

public class Alert_Enable_GPS extends Activity {
	private LocationManager location_manager;
	private NotificationManager notification_manager;
	
	private int GPS_ENABLED_REQUEST_CODE = 0x0A;
	private Activity self;
	private String objective;
	
	private boolean service_started = false;
	
	private final static String TOAST_STR = "Please enable GPS\nPress the back button to see more information";
	private final static String ALERT_STR = "GPS needs to be enabled for the UCSB Geog Tracer app to work."
			+" Your GPS coordinates will only be used when your phone detects it is on campus."
			+" Enabling the GPS does not waste battery life. "
			+" GPS only affects the battery when other apps are using it frequently."
			+" Please enable GPS."
			+"";
	
	private final static CharSequence TICKER_STR = "GPS needs to be enabled";
	private final static CharSequence STATUS_STR = "GPS must remain enabled for app to function properly";
	
	@Override
	public void onCreate(Bundle instanceState) {
		super.onCreate(null);
		
		self = this;
		objective = this.getIntent().getStringExtra("objective");
		
		notification_manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		location_manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		
		// if is actually enabled 
		if(location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Intent intent = new Intent(self, WAP_Tracer_Service.class);
			intent.putExtra("objective", objective);
			startService(intent);
			self.finish();
		}
		else {
			Toast.makeText(this, TOAST_STR, Toast.LENGTH_LONG).show();
			
			location_manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, 
					new LocationListener() {
						public void onLocationChanged(Location location) {	
						}
						public void onProviderDisabled(String provider) {	
						}
						public void onProviderEnabled(String provider) {
							location_manager.removeUpdates(this);
							self.finishActivity(GPS_ENABLED_REQUEST_CODE);
							start_service();
						}
						public void onStatusChanged(String provider, int status, Bundle extras) {	
						}
					}
			);
			
			Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			self.startActivityForResult(intent, GPS_ENABLED_REQUEST_CODE);
		}
	}
	
	
	@Override
	public void onStop() {
		super.onStop();
		if(!service_started) {
			long now = System.currentTimeMillis(); 
			Notification notification = new Notification(R.drawable.earth, TICKER_STR, now);
			Context context = getApplicationContext();
			
			CharSequence contentTitle = this.getString(R.string.app_name);
			CharSequence contentText = STATUS_STR;
			
			notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
			
			Intent notificationIntent = new Intent(this, Alert_Enable_GPS.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		
			notification_manager.notify(0, notification);
		}
	}
	
	private void start_service() {
		if(service_started) return;
		service_started = true;
		notification_manager.cancelAll();
		Intent intent = new Intent(self, WAP_Tracer_Service.class);
		intent.putExtra("objective", objective);
		startService(intent);
		self.finish();
	}
	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == GPS_ENABLED_REQUEST_CODE && resultCode == 0) {
			if(location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				start_service();
			}
			else {
				warning();
			}
		}
	}
	
	private void warning() {
		AlertDialog.Builder alert_dialog_builder = new AlertDialog.Builder(this);
		alert_dialog_builder.setMessage(ALERT_STR)
				.setCancelable(false)
				.setPositiveButton("Okay",
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						/* if gps was enabled since the alert was created */
						if(location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
							/* notify the service */
							Intent intent = new Intent(self, WAP_Tracer_Service.class);
							intent.putExtra("objective", objective);
							startService(intent);
							self.finish();
						}
						/*take the user to the settings menu */
						else {
							Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							self.startActivityForResult(intent, GPS_ENABLED_REQUEST_CODE);
						}
					}
				}
						);
		AlertDialog alert = alert_dialog_builder.create();
		alert.show();
	}
	
	public void onDestroy() {
		super.onDestroy();
		notification_manager.cancelAll();
		if(!service_started)
			start_service();
	}

}
