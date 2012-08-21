package edu.ucsb.geog.blakeregalia;

import java.util.ArrayList;
import java.util.List;

public class WAP_Event {
	private long timestamp;
	private float latitude;
	private float longitude;
	private int altitude;
	private int num_waps;
	private int accuracy;
	private float gps_stale_time;
	private List<WAP> wap_list;

	public WAP_Event(long time, float lat, float lng, int location_accuracy) {
		timestamp = time;
		latitude = lat;
		longitude = lng;
		accuracy = location_accuracy;
		num_waps = 0;
		wap_list = new ArrayList<WAP>();
	}
	
	public WAP_Event(long time, float lat, float lng, int alt, int location_accuracy, float stale_time) {
		timestamp = time;
		latitude = lat;
		longitude = lng;
		altitude = alt;
		accuracy = location_accuracy;
		gps_stale_time = stale_time;
		num_waps = 0;
		wap_list = new ArrayList<WAP>();
	}
	
	public void add_WAP(WAP wap) {
		wap_list.add(wap);
		num_waps += 1;
	}
	
	public long getTime() {
		return timestamp;
	}
	
	public float getLat() {
		return latitude;
	}
	
	public float getLon() {
		return longitude;
	}
	
	public float getAlt() {
		return altitude;
	}
	
	public int getNumWAPs() {
		return num_waps;
	}
	
	public WAP getWAP(int index) {
		return wap_list.get(index);
	}
	
	public int getAccuracy() {
			return accuracy;
	}
	
	public float getStaleness() {
			return gps_stale_time;
	}
}
