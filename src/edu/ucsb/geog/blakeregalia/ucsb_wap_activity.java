package edu.ucsb.geog.blakeregalia;

import java.util.List;

import android.app.Activity;
import android.app.Activity.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.net.wifi.*;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ucsb_wap_activity extends Activity {
	private WifiManager wifi;
	private List<ScanResult> waps;
	private TableLayout table; 
	private TextView debugTextView;
	
	private boolean ignore_rssi_change = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        table = (TableLayout) findViewById(R.id.tableLayout1);
        debugTextView = (TextView) findViewById(R.id.debugTextView);
        
        encode();
        
        //initialize();
    }
    
    private void debug(String message) {
    	debugTextView.setText(message);
    }
    
    private void initialize() {
    	wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    	
    	// enable wifi if it is off
    	if(wifi.isWifiEnabled() == false) {
	        IntentFilter intent = new IntentFilter();
	        intent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
	        this.registerReceiver(new BroadcastReceiver() {
	        	@Override
	        	public void onReceive(Context c, Intent i) {
	        		wifi = (WifiManager) c.getSystemService(Context.WIFI_SERVICE);
	        		if(wifi.isWifiEnabled()) {
	        			debug("wifi enabled");
	        			start_listeners();
	        		}
	        		else {
	        			debug("could not enable wifi");
	        		}
	        	}
	        }
	        , intent);

    		debug("enabling wifi...");
    		wifi.setWifiEnabled(true);
    	}
    	// wifi is already enabled
    	else {
			start_listeners();
    	}
    }
    
    private void start_listeners() {
		//listen_for_rssi_change();
		listen_for_scans();
		scan();
    }
    
    private void scan() {
        debug(" * = UCSB Wireless Web");
        ignore_rssi_change = true;
    	wifi.startScan();
    }
    
    public void getWAPs() {
    	ScanResult access_point;
    	TableRow tr;
    	TextView text;
    	StringBuilder info;
    	int signal_level;
    	int i;
    	
    	int wap_len = waps.size();
    	
    	int row_len = table.getChildCount();
    	while(row_len < wap_len) {
    		tr = new TableRow(this);
    		text = new TextView(this);
    		tr.addView(text);
    		table.addView(tr);
    		row_len += 1;
    	}

    	i = wap_len;
    	while(i > row_len) {
    		table.removeViewAt(i);
    		i -= 1;
    	}
    	
    	i = wap_len;
    	while(i-- != 0) {
    		access_point = waps.get(i);
    		tr = (TableRow) table.getChildAt(i);    		
    		text = (TextView) tr.getChildAt(0);

    		info = new StringBuilder();
    		
    		// signal
    		signal_level = (int) (WifiManager.calculateSignalLevel(access_point.level, 46) * 2.2222);
    		info.append(signal_level+"%");
    		
    		// bssid
    		info.append("  "+access_point.BSSID);
    		
    		// asterik
    		if(access_point.SSID.equals("UCSB Wireless Web")) {
    			info.append(" *");
    		}
    		
    		
    		
    		text.setText(info.toString());
    	}
    	// should wait here a bit so we don't fry their wifi card?
    	
		scan();
    }
    
    // to make the storage of BSSIDs efficient, turn the 34-byte string into a 6-byte string
    // to make the encoding of the BSSIDs quick, don't use a loop 
    public String encode_hw_addr(String hw_addr) {
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
    
    public String decode(char a, char b, char c) {
    	char[] m = new char[12];
    	char f = 0x000f;
    	
    	m[0] = (char) ((a >> 12) & f) ;
    	m[1] = (char) ((a >> 8) & f);
    	m[2] = (char) ((a >> 4) & f);
    	m[3] = (char) (a & f);

    	m[4] = (char) ((b >> 12) & f);
    	m[5] = (char) ((b >> 8) & f);
    	m[6] = (char) ((b >> 4) & f);
    	m[7] = (char) (b & f);
    	
    	m[8] = (char) ((c >> 12) & f);
    	m[9] = (char) ((c >> 8) & f);
    	m[10] = (char) ((c >> 4) & f);
    	m[11] = (char) (c & f);

    	m[0] += '0';
    	if(m[0] > 57) m[0] += 39;
    	m[1] += '0';
    	if(m[1] > 57) m[1] += 39;
    	m[2] += '0';
    	if(m[2] > 57) m[2] += 39;
    	m[3] += '0';
    	if(m[3] > 57) m[3] += 39;
    	m[4] += '0';
    	if(m[4] > 57) m[4] += 39;
    	m[5] += '0';
    	if(m[5] > 57) m[5] += 39;
    	m[6] += '0';
    	if(m[6] > 57) m[6] += 39;
    	m[7] += '0';
    	if(m[7] > 57) m[7] += 39;
    	m[8] += '0';
    	if(m[8] > 57) m[8] += 39;
    	m[9] += '0';
    	if(m[9] > 57) m[9] += 39;
    	m[10] += '0';
    	if(m[10] > 57) m[10] +=39;
    	m[11] += '0';
    	if(m[11] > 57) m[11] += 39;
    	
    	return new String(m);
    }
    
    
    public void store_WAP(String SSID, String BSSID, int rssi_level, int signal_level) {
		String hw_addr = encode_hw_addr(BSSID);
		
    }
    
    
    public void listen_for_rssi_change() {
    	IntentFilter intent = new IntentFilter();
    	intent.addAction(WifiManager.RSSI_CHANGED_ACTION);
    	this.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent i) {
				if(ignore_rssi_change == false) {
					scan();
				}
			}
    	}, intent);
    }
    
    public void listen_for_scans() {
        IntentFilter intent = new IntentFilter();
        intent.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.registerReceiver(new BroadcastReceiver() {
        	@Override
        	public void onReceive(Context c, Intent i) {
        		waps = ((WifiManager) c.getSystemService(Context.WIFI_SERVICE)).getScanResults();
        		getWAPs();
        	}
        }, intent);
    }
}