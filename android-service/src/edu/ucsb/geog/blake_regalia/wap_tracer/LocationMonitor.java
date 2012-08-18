package edu.ucsb.geog.blake_regalia.wap_tracer;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

public class LocationMonitor extends ServiceMonitor {
	
	protected LocationAdvisor mLocationAdvisor;
	protected HardwareMonitor mHardwareMonitor;

	protected void enable_gps() {
		mHardwareMonitor.enable_gps(gps_hardware_enabled, gps_hardware_disabled, gps_hardware_fail);
	}
	
	public LocationMonitor(Context context, Looper looper) {
		super(context, looper);
		mLocationAdvisor = new LocationAdvisor(mContext, mMainThread);
		mHardwareMonitor = new HardwareMonitor(mContext);
	}
	

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	
	public Runnable gps_hardware_enabled = new Runnable() {
		public void run() {
			
		}
	};
	
	protected Runnable gps_hardware_disabled = new Runnable() {
		public void run() {
			Log.d(TAG, "gps disabled");
			enable_gps();
		}
	};
	
	protected Runnable gps_hardware_fail = new Runnable() {
		public void run() {
			Log.d(TAG, "gps failed");
			postNotification(ActivityIntent.GPS_ENABLE);
		}
	};

	@Override
	public void resume() {
	}

}
