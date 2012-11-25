package edu.ucsb.geog.blake_regalia.wap_tracer;


import java.util.List;
import java.util.logging.LogManager;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;


public class EnvironmentMonitor {
	
	/*

	private static final String TAG = "EnvironmentMonitor";
	
	private SensorManager mSensorManager;
	
	private EnvironmentSensor mPressure;
	private EnvironmentSensor mHumidity;
	private EnvironmentSensor mTemperature;
	private EnvironmentSensor mMagnetic;
	
	private LogManager mLogManager;

	private enum Status {
		INIT,		// object is initializing
		READY,		// hardware is ready, waiting on criteria
		LOGGING,	// app is logging
		STOPPING, 	// monitor is waiting/closing file
		CLOSED,		// logging was shut down
	}

	private Status mStatus;

	// objects
	

	public Runnable gps_hardware_enabled = new Runnable() {
		public synchronized void run() {
			
			Log.d(TAG, "gps enabled while "+mStatus);

			switch(mStatus) {

			// gps enabled before wifi
			case INIT:
				break;
			}
		}
	};

	public Runnable gps_hardware_disabled = new Runnable() {
		public void run() {
			
			switch(mStatus) {

			// gps was disabled before anything had a chance
			case INIT:
				Log.w(TAG, "PROVIDER_GPS disabled before this initialized");
				break;
			
			// gps was disabled while testing conditions
			case READY:
				Log.i(TAG, "Why are you disabling PROVIDER_GPS? I am trying to test things");
				mStatus = Status.INIT;
				enable_gps();
				break;

			default:
				Log.w(TAG, "PROVIDER_GPS disabled happend while: "+mStatus);
				break;
			}
		}
	};

	protected void enable_gps() {
		// enable gps via hardwareManager
	}
	
	

	public EnvironmentMonitor(Context context, Looper looper) {
		super(context, looper);
		init();
	}
	
	
	private void init() {
		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		
		mPressure = new EnvironmentSensor(mSensorManager, Sensor.TYPE_PRESSURE);
		mTemperature = new EnvironmentSensor(mSensorManager, Sensor.TYPE_TEMPERATURE);
		mMagnetic = new SpatialEnvironmentSensor(mSensorManager, Sensor.TYPE_MAGNETIC_FIELD);
		
		
		//Sensor_LogManager.LogStyle logStyle = new Sensor_LogManager.LogStyle();
		
		//logStyle.addType()
		
//		mLogManager = new LogManager(logType);

		// use PROVIDER_GPS and PROVIDER_WIFI to resolve a location 
		//mLocationHelper.useProvider(LocationAdvisor.GPS | LocationAdvisor.WIFI);
		
		start();
	}

	// start
	@Override
	public void start() {
		if(mStatus == null || mStatus == Status.CLOSED) {
//			mLogManager.startNewTrace();
		}
		else {
			Log.e(TAG, "Nothing to start");
			return;
		}
			
		mStatus = Status.INIT;

		mPressure.sense();
		mMagnetic.sense();
		mTemperature.sense();
	}
	
	
	private Runnable location_fix = new Runnable() {
		public void run() {
			Location location = mLocationHelper.getLocation();
			if(location == null) {
				notifyUser(ActivityIntent.GPS_SIGNAL_WEAK);
			}
			else {
			}
		}
	};
	
	private Runnable position_lost = new Runnable() {
		public void run() {
			notifyUser(ActivityIntent.GPS_SIGNAL_LOST);
		}
	};
	
	private void attempt_scan() {
		mStatus = Status.READY;
		//mLocationHelper.obtainLocation(LocationAdvisor.BOUNDARY_CHECK_LISTENER, location_fix, position_lost);
	}

	@Override
	public void resume() {
		Log.d(TAG, "resuming "+mStatus);
		
		switch(mStatus) {}
	}


	// user-genic stop
	@Override
	public void stop() {
		switch(mStatus) {}
		//mSensorManager.unregisterListener()
		String log = mMagnetic.getLog();
		System.out.println("Magnetic: "+log);
		System.out.println("Pressure: "+mPressure.getLog());
	}

	private enum Mode {
		SENSE,
		PAUSE,
		OFF,
	}
	
	private class EnvironmentSensor implements SensorEventListener {
		
		protected Mode mode;	
		protected float[] valueSum;
		protected int accuracySum;
		protected int numSamples;
		protected int maxNumSamples;
		
		protected Sensor mSensor;
		protected SensorManager mSensorManager;
		protected TimeDataLogger mDataLogger;

		private static final int NUM_VALUES_DEFAULT = 10;
		private static final int NUM_VALUES_MORE = 20;
		
		public EnvironmentSensor(SensorManager manager, int sensorType) {
			mSensorManager = manager;
			mSensor = mSensorManager.getDefaultSensor(sensorType);
			mDataLogger = new TimeDataLogger();
			
			mode = Mode.SENSE;

			valueSum = new float[3];
			numSamples = 0;
			maxNumSamples = NUM_VALUES_DEFAULT;
		}
		
		public void sense() {
			mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		
		public void stop() {
			mode = Mode.PAUSE;
		}
		
		public String getLog() {
			return mDataLogger.getLog();
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			
		}
		
		public synchronized void onSensorChanged(SensorEvent event) {
			if(mode == Mode.PAUSE) {
				mSensorManager.unregisterListener(this);
				return;
			}
			else if(mode != Mode.SENSE) {
				return;
			}
			
			sampleSensor(event);
			accuracySum += event.accuracy;
			
			numSamples += 1;
			if(numSamples >= maxNumSamples) {
				mode = Mode.PAUSE;
				commitAverage(event);
				valueSum[0] = valueSum[1] = valueSum[2] = 0;
				accuracySum = 0;
				numSamples = 0;
			}
		}
		
		public void sampleSensor(SensorEvent event) {
			Log.d(TAG, mSensor.getName()+": "+event.values[0]+"; "+event.accuracy);
			
			valueSum[0] += event.values[0];
		}
		
		public void commitAverage(SensorEvent event) {
			ByteBuilder b = new ByteBuilder(8 + 4 + 4);
			b.append_8(Encoder.encode_long(event.timestamp));
			b.append_4(Encoder.encode_float(valueSum[0] / numSamples));
			b.append_4(Encoder.encode_int(accuracySum / numSamples));
			
			mDataLogger.log(b.getBytes());
		}

	}
	
	private class SpatialEnvironmentSensor extends EnvironmentSensor {

		public SpatialEnvironmentSensor(SensorManager manager, int sensorType) {
			super(manager, sensorType);
			
		}
		
		@Override
		public void sampleSensor(SensorEvent event) {
			Log.d(TAG, mSensor.getName()+": "+event.values[0]+"; "+event.values[1]+"; "+event.values[2]+"; "+event.accuracy);

			valueSum[0] += event.values[0];
			valueSum[1] += event.values[1];
			valueSum[2] += event.values[2];
		}
		
		public void commitAverage(SensorEvent event) {
			ByteBuilder b = new ByteBuilder(8 + 4 + 4 + 4 + 4);
			b.append_8(Encoder.encode_long(event.timestamp));
			b.append_4(Encoder.encode_float(valueSum[0] / numSamples));
			b.append_4(Encoder.encode_float(valueSum[1] / numSamples));
			b.append_4(Encoder.encode_float(valueSum[2] / numSamples));
			b.append_4(Encoder.encode_int(accuracySum / numSamples));
			
			mDataLogger.log(b.getBytes());
		}
		
		
	}
*/

}
