package edu.ucsb.geog.blakeregalia;

import java.util.Date;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.text.format.Time;

public class WAP_Manager {

	ucsb_wap_activity main;
	Context context;

	WifiManager wifi;
	WAP_Listener scan_callback;
	List<ScanResult> scan_result;

	public WAP_Manager(ucsb_wap_activity activity) {
		main = activity;
		context = (Context) main;

		wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	}

	/**
	 * binds a listener for broadcast events; notifies that wifi scan results have become available 
	 */
	private void listen_for_scans() {
		IntentFilter intent = new IntentFilter();
		intent.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		context.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent i) {
				receive_waps(((WifiManager) c.getSystemService(Context.WIFI_SERVICE)).getScanResults());
			}
		}, intent);
	}

	/**
	 * prepares the scan results to be fetched by the accessing class
	 */
	private void receive_waps(List<ScanResult> waps) {
		scan_result = waps;
		scan_callback.onComplete(scan_result.size(), (new Date()).getTime());
	}

	/**
	 * initiates the scan for wireless access points
	 */
	private void scan_waps() {
		//main.debug(" * = UCSB Wireless Web");
		wifi.startScan();
	}

	/**
	 * public functions
	 * 
	 */
	
	public void enableWifi(final Hardware_Ready_Listener callback) {
		/* if wifi is enabled... */
		if(wifi.isWifiEnabled() == true) { 
			/* notify the listener */
			callback.onReady();
		}
		/* wifi is disabled.. */
		else {
			/* wait for wifi to enable **/
			IntentFilter intent = new IntentFilter();
			intent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
			context.registerReceiver(new BroadcastReceiver() {
				public void onReceive(Context c, Intent i) {
					wifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);

					/* if wifi was enabled successfully... */
					if(wifi.isWifiEnabled()) {
						
						/* notify the listener */
						callback.onReady();
					}
					/* hardware could not be enabled */
					else {

						/* notify the listener */
						callback.onFail();
					}
				}
			}
			, intent);

			/* enable the wifi */
			wifi.setWifiEnabled(true);
		}
	}
	
	/**
	 * initiates the scanning process
	 * @param callback		the method to be called every time a scan completes
	 */
	public void startScanning(WAP_Listener callback) {
		scan_callback = callback;
		
    	/* get ready to be notified about all future scans */
		listen_for_scans();
		
		/* begin the scanning process */
		scan_waps();
	}

	/**
	 * continue the scanning process
	 * @param callback		the method to be called every time a scan completes
	 */
	public void continueScanning() {
		scan_waps();
	}
	
	public AccessPointIncident getWAP(int index) {
		return new AccessPointIncident(scan_result.get(index));
	}

	public class AccessPointIncident {
		ScanResult access_point;

		public AccessPointIncident(ScanResult wap) {
			access_point = wap;
		}

		public String toString() {
			StringBuffer info = new StringBuffer();

			/* signal */
			int signal_level = (int) (WifiManager.calculateSignalLevel(access_point.level, 46) * 2.2222);
			info.append(signal_level+"%");

			/* bssid */
			info.append("  "+access_point.BSSID);

			/* asterik */
			if(access_point.SSID.equals("UCSB Wireless Web")) {
				info.append(" *");
			}

			return info.toString();
		}
		
		/**
		 * 
		 * @return		a fixed n-byte string of this encoded access point incident
		 */
		public String encode() {
			return encode_hw_addr(access_point.BSSID);
		}

	    /**
	     * to make the storage of BSSIDs efficient, turn the 34-byte string into a 6-byte string
	     * to make the encoding of the BSSIDs quick, don't use a loop
	     */ 
		private String encode_hw_addr(String hw_addr) {
			char[] mac = new char[3];
			char a, b, c,
			d, e;

			d = hw_addr.charAt(0);
			if(d > 96) d -= 'W';
			else d -= '0';

			e = hw_addr.charAt(1);
			if(e > 96) e -= 'W';
			else e -= '0';

			a = (char) (((d << 4) | e) << 8);


			d = hw_addr.charAt(3);
			if(d > 96) d -= 'W';
			else d -= '0';

			e = hw_addr.charAt(4);
			if(e > 96) e -= 'W';
			else e -= '0';

			a |= (char) ((d << 4) | e);


			d = hw_addr.charAt(6);
			if(d > 96) d -= 'W';
			else d -= '0';

			e = hw_addr.charAt(7);
			if(e > 96) e -= 'W';
			else e -= '0';

			b = (char) (((d << 4) | e) << 8);


			d = hw_addr.charAt(9);
			if(d > 96) d -= 'W';
			else d -= '0';

			e = hw_addr.charAt(10);
			if(e > 96) e -= 'W';
			else e -= '0';

			b |= (char) ((d << 4) | e);


			d = hw_addr.charAt(12);
			if(d > 96) d -= 'W';
			else d -= '0';

			e = hw_addr.charAt(13);
			if(e > 96) e -= 'W';
			else e -= '0';

			c = (char) (((d << 4) | e) << 8);


			d = hw_addr.charAt(15);
			if(d > 96) d -= 'W';
			else d -= '0';

			e = hw_addr.charAt(16);
			if(e > 96) e -= 'W';
			else e -= '0';

			c |= (char) ((d << 4) | e);

			mac[0] = a;
			mac[1] = b;
			mac[2] = c;

			return new String(mac);
		    
		}
	}

	public interface WAP_Listener {
		public void onComplete(int size, long time);
	}

}
