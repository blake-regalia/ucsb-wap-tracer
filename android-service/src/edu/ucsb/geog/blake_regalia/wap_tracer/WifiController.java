package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

public class WifiController {
	
	private Context context;
	
	private WifiManager wifi_manager;
	
	private BroadcastReceiver broadcast_receiver = null;
	
	private List<ScanResult> scan_results;
	
	private Runnable callback_scan_ready;
	
	public WifiController(Context _context) {
		
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
	
	public boolean hasAnySSID(String[] ssids) {
		boolean test_result = false;
		
		List<ScanResult> wap_list = getResults();
		int i = wap_list.size();
		while(i-- != 0) {
			ScanResult wap = wap_list.get(i);
			int x = ssids.length;
			while(x-- != 0) {
				if(wap.SSID.equals(ssids[x])) {
					test_result = true;
					break;
				}
			}
			if(test_result) break;
		}
		
		return test_result;
	}
	
	
	public static String wifiStateToString(int wifiState) {
		String state = "?";
		switch(wifiState) {
		case WifiManager.WIFI_STATE_ENABLED:
			state = "ENABLED"; break;
		case WifiManager.WIFI_STATE_ENABLING:
			state = "ENABLING"; break;
		case WifiManager.WIFI_STATE_DISABLED:
			state = "DISABLED"; break;
		case WifiManager.WIFI_STATE_DISABLING:
			state = "DISABLING"; break;
		case WifiManager.WIFI_STATE_UNKNOWN:
			state = "UNKNOWN"; break;
		}
		return state;
	}
	
	public static byte[] encodeBSSID(String bssid) {
		String s = bssid.replaceAll(":", "");
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
}
