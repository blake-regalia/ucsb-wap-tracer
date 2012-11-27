package edu.ucsb.geog.blakeregalia;

import java.util.HashMap;

public abstract class OutputRenderer {
	
	private static final int CAPACITY_STRING_BUILDER = 64;
	
	protected int upcomingIncidentSize = -1;
	
	protected HashMap<String, String> values;
	protected StringBuilder dumpString;
	protected Location mLocation;
	
	public OutputRenderer() {
		dumpString = new StringBuilder(CAPACITY_STRING_BUILDER);
		values = new HashMap<String, String>(8);
	}

	public abstract void openEntity();
	public abstract void closeEntity();
	protected abstract void locationUpdate();

	public void set(String id, int value) {
		values.put(id, value+"");
	}

	public void set(String id, String value) {
		values.put(id, value);
	}
	
	public void setLocation(Location location, int size) {
		mLocation = location;
		upcomingIncidentSize = size;
		locationUpdate();
	}

	public String dump() {
		return dumpString.toString();
	}

}
