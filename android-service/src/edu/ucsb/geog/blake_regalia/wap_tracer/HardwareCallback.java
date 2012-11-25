package edu.ucsb.geog.blake_regalia.wap_tracer;

public interface HardwareCallback {
	
	public static final int REASON_UNIMPLEMENTED              = 0xA001;
	
	public static final int REASON_GPS_USER                   = 0xA102;
	public static final int REASON_GPS_TOGGLE_EXPLOIT_ATTEMPT = 0xA103;

	public static final int REASON_WIFI_USER                  = 0xA201;
	public static final int REASON_WIFI_UNABLE                = 0xA202;
	
	public void hardwareEnabled(int index);
	public void hardwareFailedToEnable(int index, int reason);
	public void hardwareDisabled(int index, int reason);
	
}
