package edu.ucsb.geog.blakeregalia;

import java.util.Date;

public class OutputRenderer_Text extends OutputRenderer {

	public OutputRenderer_Text() {
		super();
	}

	@Override
	public void openEntity() {
		values.clear();
	}

	@Override
	public void closeEntity() {
		String entity =
				"\t"
				+values.get("rssi")+"dBm: "
				+values.get("bssid")+" "
				+"("+values.get("ssid")+")"
				+" - "+values.get("frequency")+"mHz";
		
		dumpString.append(entity+'\n');
	}

	@Override
	protected void locationUpdate() {
		String location = 
				(new Date(mLocation.getTime()))
				+": "+upcomingIncidentSize+" Wap(s) "
				+"["
				 +mLocation.getLatitude()+", "
				 +mLocation.getLongitude()
				+"] "
				+"@"+mLocation.getAccuracy()+"m;"
				+" altitude:"+mLocation.getAltitude()+"m";
		
		dumpString.append('\n'+
				location+'\n');
	}
	
	
}
