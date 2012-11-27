package edu.ucsb.geog.blakeregalia;

public class Location extends Incident {

	private double latitude;
	private double longitude;
	private long timestamp;
	private float accuracy;
	private float altitude;

	public Location(double lat, double lon, long time, float _accuracy, float _altitude) {
		super();
		latitude = lat;
		longitude = lon;
		timestamp = time;
		accuracy = _accuracy;
		altitude = _altitude;
	}
	
	public Location(Location a, Location b, long t) {
		super();
		
		long aTime = a.getTime();
		double aLat = a.getLatitude();
		double aLon = a.getLongitude();
		float aAcc = a.getAccuracy();
		float aAlt = a.getAltitude();
		
		double f = (timestamp - aTime) / (b.getTime() - aTime);
		
		latitude = ((b.getLatitude()-aLat) * f) + aLat;
		longitude = ((b.getLongitude()-aLon) * f) + aLon;
		timestamp = t;
		accuracy = (float) (((b.getAccuracy()-aAcc) * f) + aAcc);
		altitude = (float) (((b.getAltitude()-aAlt) * f) + aAlt);
	}

	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public long getTime() {
		return timestamp;
	}
	
	public float getAccuracy() {
		return accuracy;
	}
	
	public float getAltitude() {
		return altitude;
	}
	
}
