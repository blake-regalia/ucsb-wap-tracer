package edu.ucsb.geog.blakeregalia;

public class WAP {
	private String hw_addr;
	private int rssi;
	private int ssid_key;
	
	public WAP(String mac, int signal, int ssid) {
		hw_addr = mac;
		rssi = signal;
		ssid_key = ssid;
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
}
