package edu.ucsb.geog.blakeregalia;

import java.util.Hashtable;

public class SensorTrace_WAP extends SensorTrace {
	
	protected Hashtable<Integer, String> ssidNames;

	public SensorTrace_WAP() {
		super();
		ssidNames = new Hashtable<Integer, String>(64);
	}
	
	@Override
	public Incident_WAP get(int index) {
		return (Incident_WAP) incidentList.get(index);
	}
	
	public void setSsidName(int key, String name) {
		ssidNames.put(new Integer(key), name);
	}
	
	public String ssidKeyToName(int ssidKey) {
		return ssidNames.get(new Integer(ssidKey));
	}
	
}
