package edu.ucsb.geog.blakeregalia;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GpsLocator {

	private LocationManager mLocationManager;
	private LocationListener listener;
	
	private Context context;
	
	public GpsLocator(Context aContext) {
		context = aContext;

		listener = new basicListener();
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}
	
	public void start() {
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
	}
	
	public void stop() {
		mLocationManager.removeUpdates(listener);
	}

	public class basicListener implements LocationListener {
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			System.out.println("gps location change: "+location.getAccuracy());
		}
	
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}
	
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}
	
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
	
			System.out.println("gps status change: "+status);
		}
	}
	
}
