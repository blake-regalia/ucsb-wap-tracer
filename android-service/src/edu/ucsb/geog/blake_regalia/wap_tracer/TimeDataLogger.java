package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.util.ArrayList;

public class TimeDataLogger {
	
	StringBuilder mData;
	
	public TimeDataLogger() {
		mData = new StringBuilder();
	}
	
	public void log(byte[] data) {
		mData.append(data);
	}
	
	public String getLog() {
		return mData.toString();
	}
}
