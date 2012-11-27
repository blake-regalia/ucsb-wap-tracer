package edu.ucsb.geog.blakeregalia;

import java.util.ArrayList;

public class SensorTrace {
	
	protected ArrayList<Incident> incidentList;

	public SensorTrace() {
		incidentList = new ArrayList<Incident>();
	}
	
	public void add(Incident incident) {
		incidentList.add(incident);
	}
	
	public Incident get(int index) {
		return incidentList.get(index);
	}
	
	public int getIncidentSize() {
		return incidentList.size();
	}
	
}
