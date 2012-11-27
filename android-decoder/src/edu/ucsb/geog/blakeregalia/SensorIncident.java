package edu.ucsb.geog.blakeregalia;

public abstract class SensorIncident extends Incident {

	private long timeOffsetMs;

	public SensorIncident(long timeOffset) {
		timeOffsetMs = timeOffset;
	}
	
	public long getTime(long timeStart) {
		return timeOffsetMs + timeStart;
	}
}
