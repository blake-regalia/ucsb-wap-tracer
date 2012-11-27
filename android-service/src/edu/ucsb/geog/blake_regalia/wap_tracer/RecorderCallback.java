package edu.ucsb.geog.blake_regalia.wap_tracer;

public interface RecorderCallback {

	public static int REASON_UNIMPLEMENTED = 0x01;
	public static int REASON_RUNNING       = 0x02;
	public static int REASON_STOPPED       = 0x03;

	public void recordingStarted(int index, long timestamp);
	public void recordingFailedToStart(int index, int reason);
	public void recordingStopped(int index, int reason);
	
}
