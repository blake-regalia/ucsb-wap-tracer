package edu.ucsb.geog.blakeregalia;

import java.util.List;

import edu.ucsb.geog.blakeregalia.WAP_Manager.WAP_Listener;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ucsb_wap_activity extends Activity {

	
	public static final int GPS_ENABLED_REQUEST_CODE = 0;
	protected static final long WIFI_SCAN_INTERVAL_MS = 200; 
	
	private WAP_Manager wap_manager;
	private GPS_Locator gps_locator;

	private TableLayout table; 
	private TextView debugTextView;
	
	private boolean wifi_ready = false;
	private boolean gps_ready = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /* establish wap_manager */
        wap_manager = new WAP_Manager(this);
        
        /* establish gps_locator */
        gps_locator = new GPS_Locator(this);
        
        /* set content view */
        setContentView(R.layout.main);
        
        /* fetch table layout */
        table = (TableLayout) findViewById(R.id.tableLayout1);
        
        /* fetch top text view for printing status */
        debugTextView = (TextView) findViewById(R.id.debugTextView);
        
        /* startup */
        initialize();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == GPS_ENABLED_REQUEST_CODE && resultCode == 0) {
        	gps_locator.checkIfGPSWasEnabled(data);
        }
    }
    
    public void debug(String message) {
    	debugTextView.setText(message);
    }

    /**
     * check wifi state, check GPS state
     */
    private void initialize() {
    	/* enable wifi */
    	wap_manager.enableWifi(new Hardware_Ready_Listener() {
    		public void onReady() {
	    		wifi_ready = true;
	    		if(gps_ready) {
    				hardware_ready();
    			}
    		}
    		public void onFail() {
    			debug("failed to start wifi");
    		}
    	});

    	/* enable gps */
    	gps_locator.enableGps(new Hardware_Ready_Listener() {
    		public void onReady() {
    			gps_ready = true;
    			if(wifi_ready) {
    				hardware_ready();
    			}
    		}
    		public void onFail() {
    			debug("failed to start gps");
    		}
    	});
    }
    
    private void hardware_ready() {
    	debug("device hardware ready. waiting for location fix");
    	gps_locator.useNetworkProvider();
		
    	gps_locator.waitForPositionFix(new GPS_Locator.Position_Fix_Listener() {
    		public void onReady() {
    			device_ready();
    		}
    		public void onFail() {
    			
    		}
    	}, (float) 10.0);
    }

    /**
     * indicate to the user that the device is ready for action, and wait for their approval
     */
    private void device_ready() {
		/* start scanning */
		wap_manager.startScanning(wap_listener());
    }

    private WAP_Listener wap_listener() {
    	return new WAP_Manager.WAP_Listener() {
    		/**
    		 * gets called once the scan has completed
    		 */
			public void onComplete(int size) {
				
				/* assure number of rows */
				correct_table_length(size);
				
				/* display results */
				display_scan_results_to_table(size);
				
				
				Location gps = gps_locator.getCurrentLocation();
				debug("location: "+gps.getLatitude()+","+gps.getLongitude()+"; "+gps.getAccuracy()+"m");
				
				try {
					Thread.sleep(WIFI_SCAN_INTERVAL_MS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				/* scan for waps again */
				wap_manager.continueScanning();
			}
		};
	}
    
    /**
     * make sure that the correct number of rows is present in the tableview layout
     */
    private void correct_table_length(int size) {
    	TableRow tr;
    	TextView text;
    	int i;

    	/* add rows while there aren't enough */
    	int row_len = table.getChildCount();
    	while(row_len < size) {
    		tr = new TableRow(this);
    		text = new TextView(this);
    		tr.addView(text);
    		table.addView(tr);
    		row_len += 1;
    	}

    	/* remove rows while there's too many */
    	i = size;
    	while(i > row_len) {
    		table.removeViewAt(i);
    		i -= 1;
    	}
    }

    /**
     * iteratively push the string summary of each access point to the tableview
     */
    private void display_scan_results_to_table(int size) {
    	TableRow tr;
    	TextView text;
    	WAP_Manager.AccessPointIncident wap;
    	
    	int i = size;
    	while(i-- != 0) {
    		wap = wap_manager.getWAP(i);

    		tr = (TableRow) table.getChildAt(i);    		
    		text = (TextView) tr.getChildAt(0);
    		
    		text.setText(wap.toString());
    	}
    }
    
    /**
     * to make the storage of BSSIDs efficient, turn the 34-byte string into a 6-byte string
     * to make the encoding of the BSSIDs quick, don't use a loop
     */ 
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
}