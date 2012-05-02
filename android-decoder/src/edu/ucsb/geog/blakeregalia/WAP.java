package edu.ucsb.geog.blakeregalia;

public class WAP {
	private String hw_addr;
	private int rssi;
	private int ssid_key;
	private int frequency;

	public WAP(String mac, int signal, int ssid) {
		hw_addr = mac;
		rssi = signal;
		ssid_key = ssid;
	}
	
	public WAP(String mac, int signal, int ssid, int mHz) {
		hw_addr = mac;
		rssi = signal;
		ssid_key = ssid;
		frequency = mHz;
	}
	
	public String getMAC() {
		return hw_addr;
	}
	
	public int getRSSI() {
		return rssi;
	}

	public int getSSID() {
		return ssid_key;
	}
	
	public int getFrequency() {
		return frequency;
	}
}
