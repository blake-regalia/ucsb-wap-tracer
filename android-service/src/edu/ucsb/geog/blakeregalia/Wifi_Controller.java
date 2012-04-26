package edu.ucsb.geog.blakeregalia;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

public class Wifi_Controller {
	
	private Context context;
	
	private WifiManager wifi_manager;
	
	private BroadcastReceiver broadcast_receiver = null;
	
	private List<ScanResult> scan_results;
	
	private Runnable callback_scan_ready;
	
	public Wifi_Controller(Context _context) {
		
		context = _context;
		
		wifi_manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	}
	
	private void callback(Runnable code) {
		Thread thread = (new Thread(code));
		thread.start();
	}
	
	public void abort() {
		if(broadcast_receiver != null) {
			context.unregisterReceiver(broadcast_receiver);
			broadcast_receiver = null;
		}
	}
	
	public void scan(Runnable ready, Runnable fail) {
		callback_scan_ready = ready;
		
		listen_for_broadcasts(new saveResults());
		
		wifi_manager.startScan();
	}
	
	private void listen_for_broadcasts(BroadcastReceiver receiver) {
		abort();
		IntentFilter intent = new IntentFilter();
		intent.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		broadcast_receiver = receiver;
		context.registerReceiver(receiver, intent);
	}
	
	public class saveResults extends BroadcastReceiver {
		@Override
		public void onReceive(Context c, Intent i) {
			scan_results = wifi_manager.getScanResults();
			callback(callback_scan_ready);
		}
	}
	
	public List<ScanResult> getResults() {
		return scan_results;
	}
}
