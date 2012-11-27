package edu.ucsb.geog.blakeregalia;

public class Wap {
	private String bssid;
	private int ssid_key;
	private int frequency;
	private int rssi;
	
	public Wap(byte[] _bssid, byte _ssid, char _frequency, byte _rssi) {
		bssid = translateHardwareAddress(_bssid);
		ssid_key = (int) _ssid;
		frequency = (int) _frequency;
		rssi = (int) _rssi;
	}

	public Wap(String _bssid, byte _rssi, byte _ssid) {
		bssid = _bssid;
		rssi = (int) _rssi;
		ssid_key = (int) _ssid;
	}
	
	public Wap(String _bssid, byte _rssi, byte _ssid, char _frequency) {
		bssid = _bssid;
		rssi = (int) _rssi;
		ssid_key = (int) _ssid;
		frequency = (int) _frequency;
	}
	
	public String getBSSID() {
		return bssid;
	}

	public int getSSID() {
		return ssid_key;
	}
	
	public int getFrequency() {
		return frequency;
	}
	
	public int getRSSI() {
		return rssi;
	}

	

	private String translateHardwareAddress(byte[] hwAddr) {
    	StringBuilder hwAddrStr = new StringBuilder();
    	String tmpHex;
    	
    	tmpHex = Integer.toHexString(hwAddr[0] & 0xff);
    	if(tmpHex.length() == 1) {
    		hwAddrStr.append('0');
    	}
    	hwAddrStr.append(tmpHex);
    	
    	hwAddrStr.append(':');
    	
    	tmpHex = Integer.toHexString(hwAddr[1] & 0xff);
    	if(tmpHex.length() == 1) {
    		hwAddrStr.append('0');
    	}
    	hwAddrStr.append(tmpHex);

    	hwAddrStr.append(':');
    	
    	tmpHex = Integer.toHexString(hwAddr[2] & 0xff);
    	if(tmpHex.length() == 1) {
    		hwAddrStr.append('0');
    	}
    	hwAddrStr.append(tmpHex);

    	hwAddrStr.append(':');
    	
    	tmpHex = Integer.toHexString(hwAddr[3] & 0xff);
    	if(tmpHex.length() == 1) {
    		hwAddrStr.append('0');
    	}
    	hwAddrStr.append(tmpHex);

    	hwAddrStr.append(':');
    	
    	tmpHex = Integer.toHexString(hwAddr[4] & 0xff);
    	if(tmpHex.length() == 1) {
    		hwAddrStr.append('0');
    	}
    	hwAddrStr.append(tmpHex);

    	hwAddrStr.append(':');
    	
    	tmpHex = Integer.toHexString(hwAddr[5] & 0xff);
    	if(tmpHex.length() == 1) {
    		hwAddrStr.append('0');
    	}
    	hwAddrStr.append(tmpHex);

    	return hwAddrStr.toString();
	}

}
