package edu.ucsb.geog.blake_regalia.wap_tracer;

public interface SensorLooperCallback {

	public void sensorLooperOpened(int index);
	public void sensorLooperClosed(int index);
	public void sensorLooperFailed(int index, int reason);
	
}
