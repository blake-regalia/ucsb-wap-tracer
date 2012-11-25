package edu.ucsb.geog.blake_regalia.wap_tracer;

import android.content.Context;

public abstract class SensorLocationProvider extends SensorLooper {
	
	protected HardwareManager mHardwareManager;

	public SensorLocationProvider(SensorLooperCallback callback, Context context) {
		super(-1, callback, context);
		mHardwareManager = new HardwareManager(context);
	}

	public void startRecording(RecorderCallback callback) {
		startRecording(callback, -1, System.currentTimeMillis());
	}
	
	public void stopRecording(RecorderCallback callback) {
		stopRecording(callback, -1);
	}

	public void setGpsToggleExploitMotivation(boolean motivation) {
		mHardwareManager.setGpsToggleExploitMotivation(motivation);
	}
	
}
