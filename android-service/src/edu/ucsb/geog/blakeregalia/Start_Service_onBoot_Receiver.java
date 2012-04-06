package edu.ucsb.geog.blakeregalia;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
 
public class Start_Service_onBoot_Receiver extends BroadcastReceiver 
{
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent i = new Intent();
			i.setAction("edu.ucsb.geog.blakeregalia.WAP_Tracer_Service");
			context.startService(i);
		}
	}
}