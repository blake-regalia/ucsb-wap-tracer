package edu.ucsb.geog.blakeregalia;

public interface DecoderOutput {
	public void initialize(long size, long time, long lat, long lon);
	public void addEvent(WAP_Event event);
	public void setSSID(int key, String ssid);
	public String dump();
}
