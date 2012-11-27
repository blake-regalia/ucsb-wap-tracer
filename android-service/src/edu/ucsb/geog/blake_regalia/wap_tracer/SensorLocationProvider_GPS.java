package edu.ucsb.geog.blake_regalia.wap_tracer;

import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

public class SensorLocationProvider_GPS extends SensorLocationProvider implements LocationResultsCallback {
	
	private static final String TAG = "SensorLocationProvider(GPS)";

	public static final double UCSB_CAMPUS_LAT_MIN = 34.40724;
	public static final double UCSB_CAMPUS_LAT_MAX = 34.42265;
	public static final double UCSB_CAMPUS_LON_MIN = -119.85352;
	public static final double UCSB_CAMPUS_LON_MAX = -119.838375;

	private double[] COORDINATE_PAIR_BASIS = {
			UCSB_CAMPUS_LAT_MIN,
			UCSB_CAMPUS_LON_MIN
	};

	private static final int MODE_SHUTDOWN       = 0xCB00;
	private static final int MODE_BOUNDARY_CHECK = 0xCB01;
	private static final int MODE_FIRST_LOCATION = 0xCB02;
	private static final int MODE_LOCATION_LOCK  = 0xCB03;

	private int senseMode;
	private TraceManager_GPS mTraceManager;
	private LocationHelper mLocationHelper;

	@Override
	protected boolean preLooperMethod() {
		Log.d(TAG, "preLooperMethod()");
		mTraceManager = new TraceManager_GPS(mContext, sensorLocationProviderTimeStarted, COORDINATE_PAIR_BASIS, "location");
		senseMode = MODE_BOUNDARY_CHECK;
		return false;
	}

	public SensorLocationProvider_GPS(SensorLooperCallback callback, Context context, Looper looper) {
		super(callback, context);
		mLocationHelper = new LocationHelper(context, looper);
		mLocationHelper.useProvider(LocationHelper.PROVIDER_GPS);
	}

	@Override
	public void attemptEnableHardware(HardwareCallback callback) {
		mHardwareManager.enable(HardwareManager.RESOURCE_GPS, callback);
	}
	
	private String modeToString(int mode) {
		switch(mode) {
		case MODE_BOUNDARY_CHECK: return "Boundary Check";
		case MODE_FIRST_LOCATION: return "First Location";
		case MODE_LOCATION_LOCK: return "Location Lock";
		case MODE_SHUTDOWN: return "Shutdown";
		}
		return "?";
	}

	@Override
	protected void sensorLoopMethod() {
		looperBlocking = BLOCKING_ON;
		
		Log.d(TAG, "= > "+modeToString(senseMode));
		
		switch(senseMode) {
		case MODE_BOUNDARY_CHECK:
			mLocationHelper.obtainLocation(LocationHelper.LISTENER_CHECK_BOUNDARY, this);
			break;

		case MODE_FIRST_LOCATION:
			mLocationHelper.obtainLocation(LocationHelper.LISTENER_TRACE_LOCATION, this);
			break;

		case MODE_LOCATION_LOCK:
			break;
		
		case MODE_SHUTDOWN:
			break;
			
		}
	}
	
	private void closeResources() {
		Log.i(TAG, "closeResources()");
		
		// stop requesting updates
		mLocationHelper.unbind();
		
		// close trace file
		mTraceManager.closeFile();

		// release hardware
		mHardwareManager.unbindGps();
	}

	public synchronized void locationReady(Location location) {
		boolean notifyAfterRecord = false;
		
		Log.d(TAG, "mode was: "+modeToString(senseMode));
		
		switch(senseMode) {
		
		// perform boundary check
		case MODE_BOUNDARY_CHECK:
			// boundary check passed
			if(true) {
				// create & initiate trace file
				mTraceManager.openFile();
				
				// advance mode to tracing
				senseMode = MODE_FIRST_LOCATION;
			}
			// boundary check failed
			else {
				// unbind from listeners
				mLocationHelper.unbind();
				
				// let owner know we are shutting off & why
				exitLoopNotifyOwner(REASON_BOUNDARY_CONDITION);
				
				// do not continue;
				return;
			}
			break;
			
		case MODE_FIRST_LOCATION:
			notifyAfterRecord = true;
			senseMode = MODE_LOCATION_LOCK;
			
		// obtain location as trace
		case MODE_LOCATION_LOCK:
			// record location
			int reason = mTraceManager.recordEvent(location);
			
			if(notifyAfterRecord) {
				// notify owner recording started
				notifyOwnerRecordingStarted();
				Log.i(TAG, "--> notifying owner recording started <--");
			}
			
			// check if recordEvent needs to shutdown looper
			if(reason != REASON_NONE) {
				shutdownReason = reason;
				Log.w(TAG, "recordEvent() => "+reason);
			}
			
			if(shutdownReason != REASON_NONE) {
				closeResources();

				exitLoopNotifyOwner(reason);
				
				return;
			}
			break;
		}

		Log.d(TAG, "mode changed to: "+modeToString(senseMode));
		
		// allow loop to continue acquiring gps lock
		looperBlocking = BLOCKING_OFF;		
	}

	public synchronized void positionLost(int reason) {
		Log.w(TAG, "position lost!");
		
		switch(senseMode) {
		case MODE_BOUNDARY_CHECK:
			break;

		case MODE_FIRST_LOCATION:
		case MODE_LOCATION_LOCK:
			mTraceManager.closeFile();
			break;
		}

		mLocationHelper.unbind();
		exitLoopNotifyOwner(REASON_LOCATION_LOST);
	}

	@Override
	protected synchronized void terminateLooper(int reason) {
		senseMode = MODE_SHUTDOWN;
		closeResources();
	}
	

}
