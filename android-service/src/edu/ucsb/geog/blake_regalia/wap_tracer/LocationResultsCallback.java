package edu.ucsb.geog.blake_regalia.wap_tracer;

import android.location.Location;

public interface LocationResultsCallback {
	
	public static final int REASON_POOR_RECEPTION = 0xA001;	
	public static final int REASON_POOR_ACCURACY  = 0xA002;
	public static final int REASON_LOCATION_NULL  = 0xA003;
	public static final int REASON_TIMED_OUT      = 0xA004;

	public void locationReady(Location location);
	public void positionLost(int reason);
	
}
