package edu.ucsb.geog.blake_regalia.wap_tracer;

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
			i.setAction("edu.ucsb.geog.blake_regalia.wap_tracer.WAP_Tracer_Service");
			context.startService(i);
		}
	}
}