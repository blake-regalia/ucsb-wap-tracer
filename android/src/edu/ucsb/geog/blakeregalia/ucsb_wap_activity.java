package edu.ucsb.geog.blakeregalia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import edu.ucsb.geog.blakeregalia.WAP_Manager.WAP_Listener;

/**
 * 
 * DATA:
 * HTTTTLLLLLLLLMMMMMMrrMMMMMMrr
 * 0123456789ABCDEF0123456789ABC
 * 
 * Header - 0+1
 * Time - 1+4
 * Location - 5+8
 * WAP - 13+8, 21+8, 29+8
 * 
 * Timestamp = z
 * E(z) => (z-start_time)*10^-2
 * 
 * LatLng = z
 * E(z) => (int) ((z-nw_campus_corner)*10^6)
 * 
 * @author Blake
 *
 */

public class ucsb_wap_activity extends Activity {

	public static final int GPS_ENABLED_REQUEST_CODE = 0;
	protected static final long WIFI_SCAN_INTERVAL_MS = 1000;
	private static final String DATA_FILE_NAME = "trace.bin"; 
	
	/* precision of the recorded time-stamp values in milliseconds, value of 100 yields 0.1 second resolution */
	private static final int TIMESTAMP_PRECISION_MS = 100;
	private static final double TIMESTAMP_REDUCTION_FACTOR = 1.0 / TIMESTAMP_PRECISION_MS;
	
	private WAP_Manager wap_manager;
	private GPS_Locator gps_locator;

	private TableLayout table; 
	private TextView debugTextView;
	
	private FileOutputStream data_file;
	private long start_time;
	
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
    
    /**
     * turns a 8-byte long representing a timestamp to a 2-byte char
     * this offers at most 65535 unique entries
     * translated to available time span in minutes => 65535*(1 minute / 60 seconds)*(1 second / 10 deciseconds) = 109 minutes & 13.5 seconds
     * @param ts
     * @return
     */
    private char encode_timestamp(long timestamp) {
    	return Character.toChars((int) ((timestamp-start_time)*TIMESTAMP_REDUCTION_FACTOR))[0];
    }
    
    private int decode_timestamp(char ts) {
    	return (int) ts;
    }
    
    private byte[] encode_long(long longInt) {
    	 byte[] array = new byte[8];
         
    	 /* convert from an unsigned long int to a 8-byte array */
    	 array[0] = (byte) ((longInt >> 56) & 0xFF);
    	 array[1] = (byte) ((longInt >> 48) & 0xFF);
    	 array[2] = (byte) ((longInt >> 40) & 0xFF);
    	 array[3] = (byte) ((longInt >> 32) & 0xFF);
    	 array[4] = (byte) ((longInt >> 24) & 0xFF);
    	 array[5] = (byte) ((longInt >> 16) & 0xFF);
    	 array[6] = (byte) ((longInt >> 8) & 0xFF);
    	 array[7] = (byte) (longInt & 0xFF);
    	 
		return array;
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
    	
    	try {
			data_file = this.openFileOutput(DATA_FILE_NAME, Context.MODE_APPEND);
		} catch (FileNotFoundException e) {
			debug(e.getMessage());
		}
    	
    	debug("device hardware ready. waiting for location fix\ndata file is currently "+(new File(this.getFilesDir(), DATA_FILE_NAME)).length()+"kb");
    	
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
    	/* set the start time so we only store the time differences */
    	start_time = (new Date()).getTime();
    	
    	/* write the file header */
    	try {
    		data_file.write(encode_long(start_time));
    	} catch (IOException e) {
			debug(e.getMessage());
    	}
    	
		/* start scanning */
		wap_manager.startScanning(wap_listener());
    }

    private WAP_Listener wap_listener() {
    	return new WAP_Manager.WAP_Listener() {
    		/**
    		 * gets called once the scan has completed
    		 */
			public void onComplete(int size, long time) {
				
				/* assure number of rows */
				correct_table_length(size);
				
				/* write data */
				handle_data(size, time);
				
				/* display results */
//				display_scan_results_to_table(size);
				
				
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
    
    private void display_scan_result_to_table_row(int row, String text) {
		TableRow tr = (TableRow) table.getChildAt(row);    		
		TextView tv = (TextView) tr.getChildAt(0);
		
		tv.setText(text);
    }
    
    /**
     * @param time 
     * 
     */
    private void handle_data(int size, long time) {
    	StringBuilder data = new StringBuilder();
    	WAP_Manager.AccessPointIncident wap;
    	
    	/**/
    	data.append(encode_timestamp(time));
    	data.append((byte) size);
    	
    	int i = size;
    	while(i-- != 0) {
    		wap = wap_manager.getWAP(i);
    		data.append(wap.encode());
    		display_scan_result_to_table_row(i, wap.toString());
    	}
    	
    	/*
    	try {
			data_file.write(data.toString().getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
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