package edu.ucsb.geog.blakeregalia;

import android.location.Location;

public class Boundary {
	
	public static final int UCSB_CAMPUS = 0;

	public static final double UCSB_CAMPUS_LAT_MIN = 34.40724;
	public static final double UCSB_CAMPUS_LAT_MAX = 34.42265;
	public static final double UCSB_CAMPUS_LON_MIN = -119.85352;
	public static final double UCSB_CAMPUS_LON_MAX = -119.838375;

	private double minLat;
	private double maxLat;
	private double minLon;
	private double maxLon;
	
	public Boundary(int preset) {
		switch(preset) {
		case UCSB_CAMPUS:
			init(UCSB_CAMPUS_LAT_MIN, UCSB_CAMPUS_LAT_MAX, UCSB_CAMPUS_LON_MIN, UCSB_CAMPUS_LON_MAX);
			break;
		}
	}

	public Boundary(double _minLat, double _maxLat, double _minLon, double _maxLon) {
		init(_minLat, _maxLat, _minLon, _maxLon);
	}
	
	private void init(double _minLat, double _maxLat, double _minLon, double _maxLon) {
		minLat = _minLat;
		maxLat = _maxLat;
		minLon = _minLon;
		maxLon = _maxLon;
	}

	public boolean check(Location location) {
		double latitude = location.getLatitude();
		double longitude = location.getLongitude();
		return (
				(latitude > minLat)
				&& (latitude < maxLat)
				&& (longitude > minLon)
				&& (longitude < maxLon)
				);
	}
}
