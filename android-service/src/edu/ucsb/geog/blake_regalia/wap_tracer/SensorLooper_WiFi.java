package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

public class SensorLooper_WiFi extends SensorLooper {
	
	private static final String TAG = "SensorLooper(WiFi)";
	
	private static final long INTERVAL_MIN_SCAN_WIFI_MS = 1500; // 1.5 second resolution
	
	private int timeoutNextScan = -1;
	
	private WifiManager mWifiManager;
	private TraceManager_WAP mTraceManager;
	private HardwareManager mHardwareManager;
	private BroadcastReceiver mBroadcastReceiver;
	
	
	/**
	 * Registers a broadcast receiver to be triggered when a scan completes
	 * @param receiver
	 */
	private void listenForBroadcasts(BroadcastReceiver receiver) {
		IntentFilter intent = new IntentFilter();
		intent.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		mBroadcastReceiver = receiver;
		mContext.registerReceiver(receiver, intent);
	}
	
	/**
	 * initialize the trace manager and create a trace file before writing to it
	 * @return		true if this trace has started recording after this method executes
	 */
	@Override
	protected boolean preLooperMethod() {
		mTraceManager = new TraceManager_WAP(mContext, sensorLocationProviderTimeStarted);
		if(!mTraceManager.openFile()) {
			Log.e(TAG, "Failed to create new trace file.");
			exitLoopNotifyOwner(REASON_IO_ERROR);
		}
		
		// register for wifi service
		listenForBroadcasts(new SaveResults());
		
		return true;
	}

	/**
	 * bind a receiver for scan results, save start time & begin scan
	 */
	@Override
	protected int sensorLoopMethod() {
		scanTimeStarted = System.currentTimeMillis();
		mWifiManager.startScan();
		return BLOCKING_ON;
	}
	
	/**
	 * the owner of this object requested the looper terminate,
	 * release resources and close files
	 */
	@Override
	protected synchronized void terminateLooper(int reason) {
		// do not start a new scan if there is a timeout for it
		if(timeoutNextScan != -1) {
			Timeout.clearTimeout(timeoutNextScan);
		}

		// assure safe shutdown
		closeResources();
	}
	
	/**
	 * close files & release hardware
	 */
	private void closeResources() {
		Log.d(TAG, "closeResources()");
		
		// unregister wifi scan receiever
		unregisterReceiver();
		
		// close the trace file
		if(!mTraceManager.closeFile()) {
			exitLoopNotifyOwner(REASON_IO_ERROR);
		}
	}
	
	/**
	 * release hardware 
	 */
	private void unregisterReceiver() {
		if(mBroadcastReceiver != null) {
			mContext.unregisterReceiver(mBroadcastReceiver);
		}
		mBroadcastReceiver = null;
	}

	private class SaveResults extends BroadcastReceiver {
		@Override
		public void onReceive(Context c, Intent i) {
			scanTimeStopped = System.currentTimeMillis();
			List<ScanResult> scanResults = mWifiManager.getScanResults();
			int reason = mTraceManager.recordEvent(scanResults, getScanTimeAverageOffset());
			
			// recordEvent() is causing a shutdown
			if(reason != REASON_NONE) {
				shutdownReason = reason;
			}
			
			// something initiated a shutdown
			if(shutdownReason != REASON_NONE) {
				// assure safe shutdown
				closeResources();

				// exit loop and notify owner
				exitLoopNotifyOwner(reason);
				
				// do not continue
				return;
			}
			
			// wait for the designated interval time to pass before scanning again 
			long lastScanTime = scanTimeStopped - scanTimeStarted;
			long waitTime = (INTERVAL_MIN_SCAN_WIFI_MS - lastScanTime);
			if(waitTime < 10) {
				looperBlocking = BLOCKING_OFF;
			}
			else {
				Log.d(TAG, "waiting "+waitTime+"ms before next scan");
				timeoutNextScan = Timeout.setTimeout(new Runnable() {
					public synchronized void run() {
						timeoutNextScan = -1;
						looperBlocking = BLOCKING_OFF;	
					}
				}, waitTime);
			}
		}
	}
	
	public SensorLooper_WiFi(int _sensorLooperIndex, SensorLooperCallback sensorLooperCallback, Context context)  {
		super(_sensorLooperIndex, sensorLooperCallback, context);
		mContext = context;
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		mHardwareManager = new HardwareManager(context);
	}

	@Override
	public void attemptEnableHardware(HardwareCallback callback, int index) {
		mHardwareManager.enable(HardwareManager.RESOURCE_WIFI, callback, index);
	}

	

}
