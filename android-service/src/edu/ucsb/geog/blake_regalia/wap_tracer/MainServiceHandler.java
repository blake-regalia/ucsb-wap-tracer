package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.util.Vector;

import edu.ucsb.geog.blake_regalia.wap_tracer.MainService.OBJECTIVE;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class MainServiceHandler extends Handler implements HardwareCallback, RecorderCallback, SensorLooperCallback {
	
	private static final String TAG = "MainServiceHandler";

	private static final int STATUS_SENSOR_LOCATION_PROVIDER_OFF      = 0x00;
	private static final int STATUS_SENSOR_LOCATION_PROVIDER_STARTING = 0x01;
	private static final int STATUS_SENSOR_LOCATION_PROVIDER_ON       = 0x02;

	private static final int OBJECTIVE_NONE      = 0x00;
	private static final int OBJECTIVE_RECOVER   = 0x03;
	private static final int OBJECTIVE_SHUTDOWN  = 0x05;

	private int sensorLocationProviderStatus;
	private long sensorLocationProviderTimeStarted;
	private long sensorLoopersBitmask;
	private int objectiveClose;

	private Looper mLooper;
	private Context mContext;

	private SensorLocationProvider mSensorLocationProvider;
	private Vector<SensorLooper> mSensorLoopers;
	private CallbackHandler mCallbackHandler;

	@Override
	public void handleMessage(Message msg) {
		int objective = msg.arg1;
		Log.d(TAG, "handleMessage("+objective+")");
		
		switch(objective) {

		case MainService.OBJECTIVE.INITIALIZE:
			Log.d(TAG, "=> initialize");
		case MainService.OBJECTIVE.START_GPS:
			// give sensorLocationProvider order to enable necessary hardware
			mSensorLocationProvider.attemptEnableHardware(this);
			break;

		case MainService.OBJECTIVE.STOP_SERVICE:
			shutdown(OBJECTIVE_SHUTDOWN);
			Log.d(TAG, "=> stop service");
			break;
		
		default:
			Log.w(TAG, "Out of context: "+objective);
			return;
		}
	}

	public MainServiceHandler(Looper looper, Context context) {

		// initialize primitive data type fields
		sensorLocationProviderStatus = STATUS_SENSOR_LOCATION_PROVIDER_OFF;
		sensorLoopersBitmask = 0;
		objectiveClose = OBJECTIVE_NONE;

		// initialize object fields
		mLooper = looper;
		mContext = context;
		mSensorLocationProvider = new SensorLocationProvider_GPS(this, mContext, mLooper);
		mSensorLoopers = new Vector<SensorLooper>();

		// initialize array of modular sensorLoopers
		int sensorLooperIndex = 0;
		mSensorLoopers.add(
				new SensorLooper_WiFi(sensorLooperIndex++, this, mContext) // who to notify when something changes
				);

		// setup an object to handle callback events from the sensorLoopers
		mCallbackHandler = new CallbackHandler(mSensorLoopers);
	}

	public void shutdown(int objective) {
		int loopIndex = 0;
		for(SensorLooper tSensorLooper : mSensorLoopers) {
			tSensorLooper.stopRecording(mCallbackHandler, loopIndex);
			loopIndex += 1;
		}
		mSensorLocationProvider.stopRecording(this);
	}

	/**
	 * the sensorLocationProvider hardware enables
	 */
	public void hardwareEnabled(int ignore) {
		Log.i(TAG, "sensorLocationProvider hardware enabled");

		mSensorLocationProvider.startRecording(this);
		
		int loopIndex = 0;
		for(SensorLooper tSensorLooper : mSensorLoopers) {
			tSensorLooper.attemptEnableHardware(mCallbackHandler, loopIndex);
			loopIndex += 1;
		}
	}

	/**
	 * the sensorLocationProvider hardware failed to enable
	 */
	public void hardwareFailedToEnable(int ignore, int reason) {
		Log.w(TAG, "sensorLocationProvider hardware failed to enable");
		
		switch(reason) {
		case HardwareCallback.REASON_GPS_USER:
			// notification: PROVIDER_GPS must be enabled
			NotificationInterface.post(mContext, ActivityAlertUser.class, "gps-enable", false, "GPS needs to be enabled", "GPS must remain enabled for app to run");
			break;

		case HardwareCallback.REASON_GPS_TOGGLE_EXPLOIT_ATTEMPT:
			// restart hardware attempt
			mSensorLocationProvider.setGpsToggleExploitMotivation(false);
			mSensorLocationProvider.attemptEnableHardware(this);
			break;
		}
	}

	/**
	 * the sensorLocationProvider hardware was disabled
	 */
	public void hardwareDisabled(int ignore, int reason) {
		Log.w(TAG, "sensorLocationProvider hardware was disabled");
		
		mSensorLocationProvider.stopRecording(this);

		switch(reason) {
		case HardwareCallback.REASON_GPS_USER:
			// notification: You disabled PROVIDER_GPS! PROVIDER_GPS must remain enabled
			Log.e(TAG, "GPS was disabled");
			break;

		}
	}



	public void sensorLooperOpened(int index) {
		this.sensorLoopersBitmask |= (1 << index);
	}

	public void sensorLooperClosed(int index) {
		this.sensorLoopersBitmask &= ~(1 << index);

		if(this.sensorLoopersBitmask == 0) {
			switch(objectiveClose) {
			// this looper was intending to shutdown
			case OBJECTIVE_SHUTDOWN:
				mSensorLocationProvider.stopRecording(this);
				break;

				// this looper failed and is attempting recovery
			case OBJECTIVE_RECOVER:
				Log.w(TAG, "Attempting to recover");
				break;

				// this looper closed unexpectedly (sensorLoopers closed first) 
			case OBJECTIVE_NONE:
				/*
				switch(mSensorLoopers.get(index).getReason()) {
				case SensorLooper.REASON_DATA_FULL:
					break;
				case SensorLooper.REASON_IO_ERROR:
					break;
				}
				*/
				break;

			default:
				break;
			}
		}
	}

	/**
	 * the sensorLocationProvider has failed, shutdown all sensorLoopers
	 */
	public void sensorLooperFailed(int index, int reason) {
		shutdown(OBJECTIVE_RECOVER);
		Log.d(TAG, "Loopers were shut down");
		/*
	this.sensorLoopersBitmask &= ~(1 << index);

	int loopIndex = 0;
	for(SensorLooper tSensorLooper : mSensorLoopers) {
		int bitmask = (1 << loopIndex);
		if((bitmask & this.sensorLoopersBitmask) != 0) {
			tSensorLooper.stopRecording(mCallbackHandler, loopIndex);
		}
		loopIndex += 1;
	}
		 */
	}

	/**
	 * triggered when the locationSensorProvider starts recording
	 */
	public synchronized void recordingStarted(int ignore, long timestamp) {
		Log.i(TAG, "sensorLocationProvider started recording");
		
		sensorLocationProviderStatus = STATUS_SENSOR_LOCATION_PROVIDER_ON;
		sensorLocationProviderTimeStarted = timestamp;

		// attempt to start recording any sensor loopers if they are not already recording
		int loopIndex = 0;
		for(SensorLooper tSensorLooper : mSensorLoopers) {
			tSensorLooper.startRecording(mCallbackHandler, loopIndex, sensorLocationProviderTimeStarted);
			loopIndex += 1;
		}
	}

	public void recordingFailedToStart(int ignore, int reason) {

		String reasonStr = null;
		switch(reason) {
		case RecorderCallback.REASON_UNIMPLEMENTED: reasonStr = "Unimplemented"; break;
		case RecorderCallback.REASON_RUNNING: reasonStr = "Running"; break;
		case RecorderCallback.REASON_STOPPED: reasonStr = "Stopped"; break;
		}
		
		Log.e(TAG, "SensorLocationProvider failed to start recording: "+reasonStr);
	}

	/**
	 * triggered when the locationSensorProvider stops recording
	 */
	public void recordingStopped(int ignore, int reason) {

		Log.e(TAG, "SensorLocationProvider stopped recording.");
	}

	/**
	 * CallbackHandler
	 * @author Blake
	 *
	 * handles all events for the SensorLoopers
	 */
	private class CallbackHandler implements HardwareCallback, RecorderCallback {
		private Vector<SensorLooper> mSensorLoopers;

		public CallbackHandler(Vector<SensorLooper> sensorLoopers) {
			mSensorLoopers = sensorLoopers;
		}

		public void hardwareEnabled(int index) {
			Log.d(TAG, "sensorLooper("+index+") hardware enabled");

			// attempt to start recording if the sensor location provider is recording
			if(sensorLocationProviderStatus == STATUS_SENSOR_LOCATION_PROVIDER_ON) {
				Log.e(TAG, "sensorLocationProvider gave clearance to start recording");
				mSensorLoopers.get(index).startRecording(this, index, sensorLocationProviderTimeStarted);
			}
		}

		public void hardwareFailedToEnable(int index, int reason) {
			Log.w(TAG, "sensorLooper("+index+") hardware failed to enable");
			
			switch(reason) {
			case HardwareCallback.REASON_WIFI_UNABLE:
				// your device is unable to use WiFi!
				break;

			case HardwareCallback.REASON_WIFI_USER:
				// notify: something is preventing wifi from starting. you must allow this app to enable wifi
				break;
			}

		}

		public void hardwareDisabled(int index, int reason) {
			Log.w(TAG, "sensorLooper("+index+") hardware was disabled");
			
			switch(reason) {
			case HardwareCallback.REASON_WIFI_USER:
				mSensorLoopers.get(index).stopRecording(this, index);
				// notify: you disabled WiFi! Any sensors depending on it have been shutdown
				NotificationInterface.post(mContext, ActivityControl.class, "wifi-disabled", false, "Wi-Fi was disabled", "Tracer has shut down", 0);
				break;
			}
		}

		public void recordingStarted(int index, long timestamp) {
			Log.d(TAG, "sensorLooper("+index+") started recording");
		}

		public void recordingFailedToStart(int index, int reason) {
			Log.w(TAG, "sensorLooper("+index+") failed to start recording");
		}

		public void recordingStopped(int index, int reason) {
			switch(reason) {
			case RecorderCallback.REASON_STOPPED:
				// okay, it was volunteered to stop
				break;
			}
		}

	}
}
