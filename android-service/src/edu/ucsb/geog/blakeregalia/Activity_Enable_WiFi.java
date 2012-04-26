package edu.ucsb.geog.blakeregalia;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class Activity_Enable_WiFi extends Activity {

	private Activity self;
	
	private String objective;
	
	private WifiManager wifi_manager;
	
	private final String wifi_unavailable = "no_wifi";
	
	private TextView wifi_text;
	
	private final String text_no_wifi = "ERROR: It appears that your device does not support WiFi."
			+" This application cannot run if there is no WiFi.\n";
	
	@Override
	public void onCreate(Bundle savedInst) {
		super.onCreate(null);
		
		this.setContentView(R.layout.no_wifi);
		wifi_text = (TextView) this.findViewById(R.id.wifi_text);

		self = this;
		
		objective = this.getIntent().getStringExtra("objective");
		wifi_manager = (WifiManager) self.getSystemService(Context.WIFI_SERVICE);

		

		final int previous_state = wifi_manager.getWifiState();
		switch(previous_state) {
		case WifiManager.WIFI_STATE_ENABLING:
		case WifiManager.WIFI_STATE_ENABLED:
			wifi_text.setText("WiFi interface is enabling...");
			break;
		case WifiManager.WIFI_STATE_DISABLED:
		case WifiManager.WIFI_STATE_DISABLING:
			wifi_text.append("WiFi interface is disabled\n");
			break;
		case WifiManager.WIFI_STATE_UNKNOWN:
			hardware_fail();
			return;
		}
		
		
		// wait for wifi to enable
		IntentFilter intent = new IntentFilter();
		intent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		self.registerReceiver(new BroadcastReceiver() {
			public void onReceive(Context c, Intent i) {
				
				int state = wifi_manager.getWifiState();
				switch(state) {
				case WifiManager.WIFI_STATE_ENABLED:
					wifi_text.append("Success\n");
					self.unregisterReceiver(this);
					start_service();
					return;
				case WifiManager.WIFI_STATE_ENABLING:
					wifi_text.append("Enabling WiFi interface...\n");
					break;
				case WifiManager.WIFI_STATE_DISABLED:
					if(previous_state != WifiManager.WIFI_STATE_DISABLING) {
						hardware_fail();
					}
				case WifiManager.WIFI_STATE_DISABLING:
					hardware_fail();
					break;
				}
			}
		}
		, intent);

		// enable the wifi 
		wifi_manager.setWifiEnabled(true);
		
	}
	
	private void hardware_fail() {
		wifi_text.append("ERROR: Failed\n");
		wifi_text.append(text_no_wifi);
		stop_service();
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
	}
}
