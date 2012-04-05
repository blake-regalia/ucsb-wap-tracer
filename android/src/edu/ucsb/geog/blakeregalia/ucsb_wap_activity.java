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
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.View;
import android.widget.Button;
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
	
	public static final int VERSION = 4;
	
	/** wifi scanning **/
	protected static final long WIFI_SCAN_INTERVAL_MS = 100;
	private static final int MAX_NUM_WAPS = 255;
	
	/** data file **/
	private static final String DATA_FILE_NAME = "trace.bin";

	/** gps tracking **/
	protected static final float MIN_POSITION_ACCURACY = 15.0f;

	/** precision of the recorded time-stamp values in milliseconds, value of 10 yields 0.1 second resolution **/
	private static final int TIMESTAMP_PRECISION_MS = 10;
	private static final double TIMESTAMP_REDUCTION_FACTOR = 0.1 / TIMESTAMP_PRECISION_MS;

	/** context-free constants **/
	public static final int GPS_ENABLED_REQUEST_CODE = 0;
	private static long unique_id = 0x01;
	public static String android_id;

	/** objects **/
	private WAP_Manager wap_manager;
	private GPS_Locator gps_locator;
	private HTTP_Uploader http_uploader;

	/** ui **/
	private TableLayout table; 
	private TextView debugTextView;
	private Button action_button;

	/** io **/
	private File files_dir;
	private File trace_file;
	private FileOutputStream data_file;
	
	/** private globals **/
	private long start_time;
	private boolean wifi_ready = false;
	private boolean gps_ready = false;
	private boolean stop_scanning = false;

	private enum Action_Mode {
		EXIT, START, STOP, UPLOAD
	}
	private Action_Mode action_mode;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Info.init(this);

		android_id = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);

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

		/* fetch action button */
		action_button = (Button) findViewById(R.id.action);
		action_button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				action_button.setEnabled(false);
				switch(action_mode) {
				case EXIT:
					System.exit(0);
					break;
				case START:
					action_start();
					break;
				case STOP:
					action_stop();
					break;
				case UPLOAD:
					action_upload();
					break;
				}
			}
		});
		
		http_uploader = new HTTP_Uploader(this);

		/* startup */
		initialize();
	}

	private void set_action_mode(Action_Mode to) {
		action_mode = to;
		switch(action_mode) {
		case EXIT:
			action_button.setText("Exit");
			break;
		case START:
			action_button.setText("Start");
			break;
		case STOP:
			action_button.setText("Stop");
			break;
		case UPLOAD:
			action_button.setText("Upload");
			break;
		}
		action_button.setEnabled(true);
	}

	/**
	 * turns a 8-byte long representing a timestamp to a 2-byte char
	 * this offers at most 65535 unique entries
	 * translated to available time span in minutes => 65535*(1 minute / 60 seconds)*(1 second / 10 deciseconds) = 109 minutes & 13.5 seconds
	 * @param timestamp
	 * @return
	 */
	private byte[] encode_timestamp(long timestamp) {
		char reduced_timestamp = reduce_timestamp(timestamp);
		return Encoder.encode_char(reduced_timestamp);
	}
	
	private char reduce_timestamp(long timestamp) {
		return (char) ((timestamp-start_time) * TIMESTAMP_REDUCTION_FACTOR);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == GPS_ENABLED_REQUEST_CODE && resultCode == 0) {
			gps_locator.checkIfGPSWasEnabled(data);
		}
	}

	public void debug(String message) {
		if(trace_file != null) {
			debugTextView.setText("trace file: "+trace_file.length()+"b\n"+message);
		}
		else {
			debugTextView.setText("trace file: 0b\n"+message);
		}
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

	
	/**
	 * wifi & gps interfaces are enabled and ready
	 */
	private void hardware_ready() {
		files_dir = this.getFilesDir();
		try {
			trace_file = new File(files_dir, DATA_FILE_NAME);
			data_file = this.openFileOutput(DATA_FILE_NAME, Context.MODE_PRIVATE);
		} catch (FileNotFoundException e) {
			debug(e.getMessage());
		}

		debug("device hardware ready. waiting for location fix");

		gps_locator.useNetworkProvider();

		/* wait for gps fix */
		gps_locator.waitForPositionFix(new GPS_Locator.Position_Fix_Listener() {
			public void onReady() {
				device_ready();
			}
			public void onFail() {

			}
		}, MIN_POSITION_ACCURACY);
	}

	/**
	 * indicate to the user that the device is ready for action, and wait for their approval
	 */
	private void device_ready() {
		set_action_mode(Action_Mode.START);
	}

	private void action_start() {
		set_action_mode(Action_Mode.STOP);

		/* set the start time so we only store the time differences */
		start_time = (new Date()).getTime();

		/* write the file header */
		try {
			data_file.write(Encoder.encode_int(VERSION));
			data_file.write(Encoder.encode_long(unique_id));
			data_file.write(Encoder.encode_long(start_time));
			data_file.write(Encoder.encode_long((long) (GPS_Locator.SOUTHWEST_CAMPUS_CORNER.LATITDUE*GPS_Locator.COORDINATE_PRECISION)));
			data_file.write(Encoder.encode_long((long) (GPS_Locator.SOUTHWEST_CAMPUS_CORNER.LONGITUDE*GPS_Locator.COORDINATE_PRECISION)));
		} catch (IOException e) {
			debug(e.getMessage());
		}

		debug("starting to scan...");

		/* start scanning */
		wap_manager.startScanning(wap_listener());
	}

	private void action_stop() {
		stop_scanning = true;
	}

	private void action_upload() {

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
				debug("location:\n"
						+(Math.round(gps.getLatitude()*10000)/10000.f)+","
						+(Math.round(gps.getLongitude()*10000)/10000.f)+"; "
						+gps.getAltitude()+"m @"
						+gps.getAccuracy()+"m");

				if(stop_scanning) {
					try {
						data_file.write(wap_manager.encodeSSIDs());
						data_file.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					correct_table_length(0);

					debug("saving file...");
					http_uploader.save(trace_file);
					set_action_mode(Action_Mode.UPLOAD);
				}
				else {

					try {
						Thread.sleep(WIFI_SCAN_INTERVAL_MS);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					/* scan for waps again */
					wap_manager.continueScanning();
				}
			}
		};
	}

	private void check_network() {
		ConnectivityManager connection = ((ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE));
		if(connection.getActiveNetworkInfo() == null) {
			debug("No active network.");
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		else {
			if(connection.getActiveNetworkInfo().isConnected()) {
				debug("Network connected.");
			}
			else {
				debug("No active network.");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
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
		i = row_len;
		while(i > size) {
			table.removeViewAt(--i);
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
		if(size == 0) return;
		
		WAP_Manager.AccessPointIncident wap;
		
		ByteBuilder bytes = new ByteBuilder(Encoder.DATA_HEADER_LENGTH + size*Encoder.DATA_ENTRY_LENGTH);

		/**/
		size = Math.min(size, MAX_NUM_WAPS);
		bytes.append(Encoder.encode_byte(size));
		/**/
		bytes.append(encode_timestamp(time));
		/**/
		bytes.append(gps_locator.encode(time));

		int i = size;
		while(i-- != 0) {
			wap = wap_manager.getWAP(i);
			bytes.append(wap.encode());
			System.out.println(wap.toString());
			display_scan_result_to_table_row(i, wap.toString());
		}

		System.out.println("\n");

		try {
			data_file.write(bytes.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		/**/
	}

}