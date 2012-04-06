package edu.ucsb.geog.blakeregalia;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.Toast;

public class Activity_Enable_WiFi extends Activity {

	private Activity self;
	
	private String objective;
	
	private WifiManager wifi_manager;
	
	private final String wifi_unavailable = "no_wifi";
	
	@Override
	public void onCreate(Bundle savedInst) {
		super.onCreate(null);
		
		this.setContentView(R.layout.no_wifi);

		self = this;
		
		objective = this.getIntent().getStringExtra("objective");
		wifi_manager = (WifiManager) self.getSystemService(Context.WIFI_SERVICE);
		
		// wait for wifi to enable
		IntentFilter intent = new IntentFilter();
		intent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		self.registerReceiver(new BroadcastReceiver() {
			public void onReceive(Context c, Intent i) {
				self.unregisterReceiver(this);
				
				// if wifi was enabled successfully... 
				if(wifi_manager.isWifiEnabled()) {
					// notify the listener
					start_service();
				}
				// hardware could not be enabled
				else {
					Toast.makeText(self, "Could not enable WiFi. Your device cannot run this app.", Toast.LENGTH_LONG).show();
					stop_service();
				}
			}
		}
		, intent);

		// enable the wifi 
		wifi_manager.setWifiEnabled(true);
		
	}

	private void start_service() {
		Intent intent = new Intent(self, WAP_Tracer_Service.class);
		intent.putExtra("objective", objective);
		startService(intent);
		self.finish();
	}
	
	private void stop_service() {
		Intent intent = new Intent(self, WAP_Tracer_Service.class);
		intent.putExtra("objective", wifi_unavailable);
		startService(intent);
		self.finish();
	}
}
