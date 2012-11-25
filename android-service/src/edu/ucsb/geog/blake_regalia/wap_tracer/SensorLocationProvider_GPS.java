package edu.ucsb.geog.blake_regalia.wap_tracer;

import android.content.Context;
import android.location.Location;
import android.os.Looper;

public class SensorLocationProvider_GPS extends SensorLocationProvider implements LocationResultsCallback {

	public static final double UCSB_CAMPUS_LAT_MIN = 34.40724;
	public static final double UCSB_CAMPUS_LAT_MAX = 34.42265;
	public static final double UCSB_CAMPUS_LON_MIN = -119.85352;
	public static final double UCSB_CAMPUS_LON_MAX = -119.838375;

	private long[] COORDINATE_PAIR_BASIS = {
			(long) UCSB_CAMPUS_LAT_MIN * TraceManager.CONVERT_COORDINATE_PRECISION_LONG,
			(long) UCSB_CAMPUS_LON_MIN * TraceManager.CONVERT_COORDINATE_PRECISION_LONG
	};

	private static final int MODE_SHUTDOWN       = 0xCB00;
	private static final int MODE_BOUNDARY_CHECK = 0xCB01;
	private static final int MODE_LOCATION_LOCK  = 0xCB02;

	private int senseMode;
	private TraceManager_GPS mTraceManager;
	private LocationHelper mLocationHelper;

	@Override
	protected boolean preLooperMethod() {
		mTraceManager = new TraceManager_GPS(mContext, sensorLocationProviderTimeStarted, COORDINATE_PAIR_BASIS);
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

	@Override
	protected int sensorLoopMethod() {
		switch(senseMode) {
		case MODE_BOUNDARY_CHECK:
			mLocationHelper.obtainLocation(LocationHelper.LISTENER_CHECK_BOUNDARY, this);
			break;
			
		case MODE_LOCATION_LOCK:
			mLocationHelper.obtainLocation(LocationHelper.LISTENER_TRACE_LOCATION, this);
			break;
		
		case MODE_SHUTDOWN:
			break;
			
		}
		return BLOCKING_ON;
	}
	
	private void closeResources() {		
		// stop requesting updates
		mLocationHelper.unbind();
		
		// close trace file
		mTraceManager.closeFile();

		// release hardware
		mHardwareManager.unbindGps();
	}

	public synchronized void locationReady(Location location) {
		switch(senseMode) {
		
		// perform boundary check
		case MODE_BOUNDARY_CHECK:
			// boundary check passed
			if(true) {
				// create & initiate trace file
				mTraceManager.openFile();
				
				// notify owner recording started
				notifyOwnerRecordingStarted();
				
				// advance mode to tracing
				senseMode = MODE_LOCATION_LOCK;
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
			
		// obtain location as trace
		case MODE_LOCATION_LOCK:
			// record location
			int reason = mTraceManager.recordEvent(location);
			
			// check if recordEvent needs to shutdown looper
			if(reason != REASON_NONE) {
				shutdownReason = reason;
			}
			
			if(shutdownReason != REASON_NONE) {
				closeResources();

				exitLoopNotifyOwner(reason);
				
				return;
			}
			break;
		}
		
		// allow loop to continue acquiring gps lock
		looperBlocking = BLOCKING_OFF;
	}

	public synchronized void positionLost(int reason) {
		switch(senseMode) {
		case MODE_BOUNDARY_CHECK:
			break;

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
