package edu.ucsb.geog.blakeregalia;

import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

public class Info {
	
	{
		/*
		TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
	    final String DeviceId, SerialNum, androidId;
	     DeviceId = tm.getDeviceId();
	     SerialNum = tm.getSimSerialNumber();
	     androidId = Secure.getString(getContentResolver(),Secure.ANDROID_ID);

	     UUID deviceUuid = new UUID(androidId.hashCode(), ((long)DeviceId.hashCode() << 32) | SerialNum.hashCode());
	     String mydeviceId = deviceUuid.toString();
	     Log.v("My Id", "Android DeviceId is: " +DeviceId); 
	     Log.v("My Id", "Android SerialNum is: " +SerialNum); 
	     Log.v("My Id", "Android androidId is: " +androidId); 
	     */
	}
	
	public int getVersion() {
		return ucsb_wap_activity.VERSION;
	}
	
	public int getUserHash() {
		return ucsb_wap_activity.VERSION;
	}
	
}
