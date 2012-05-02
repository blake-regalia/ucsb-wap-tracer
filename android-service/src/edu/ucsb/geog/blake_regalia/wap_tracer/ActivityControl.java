package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ActivityControl extends Activity {

	private Activity self;

	private final String CONTROL_START_SERVICE = "Start Service";
	private final String CONTROL_STOP_SERVICE = "Stop Service";
	
	private final String CONTROL_CHECK_FORCE = "Begin Tracing";
	
	private ListView listView;
	private ControlsAdapter mControlsAdapter;

	private TextView checkForceTitleTextView;
	private TextView checkForceSubtitleTextView;

	private TextView infoTextView;
	private TextView appStatus;
	private Registration registration;

	private Runnable registrationSuccess = new Runnable() {
		public void run() {
			appStatus.setText("Registered: "+registration.getAndroidId()+"; "+registration.getPhoneNumber());
		}
	};
	private Runnable registrationFailure = new Runnable() {
		public void run() {
			appStatus.setText("An internet connection is recommended for automatic uploads");
		}
	};
	
	@Override
	public void onCreate(Bundle savedInst) {
		super.onCreate(null);

		self = this;

		this.setContentView(R.layout.controls);
		
		infoTextView = (TextView) this.findViewById(R.id.infoTextView);
		appStatus = (TextView) this.findViewById(R.id.appStatus);
		registration = new Registration(this);
		appStatus.setText("Registering device...");
		if(registration.register(registrationSuccess, registrationFailure)) {
			appStatus.setText("Registered: "+registration.getAndroidId()+"; "+registration.getPhoneNumber());
		}
		else {
			appStatus.setText("Registering device with server...");
		}
		
		ArrayList<Control> controlList = new ArrayList<Control>();

		controlList.add(
				new Control(
						getControlSwitchTitle(CONTROL_SWITCH_READ),
						getControlSwitchSubtitle(CONTROL_SWITCH_READ)
						)
				);
		
		controlList.add(
				new Control(
						CONTROL_CHECK_FORCE,
						"Touch here to have the app attempt tracing"
						)
				);
		

		listView = (ListView) this.findViewById(R.id.controlsListView);
		mControlsAdapter = new ControlsAdapter(this, R.layout.controls_list_item, controlList);
		listView.setAdapter(mControlsAdapter);

		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final TextView titleText = (TextView) ((LinearLayout) view).getChildAt(1);
				final TextView subtitleText = (TextView) ((LinearLayout) view).getChildAt(2);

				String text = (String) titleText.getText();

				if(text.equals(CONTROL_START_SERVICE)) {
					System.out.println("Starting service");
					startMainService(MainService.INTENT_OBJECTIVE.START);

					mControlsAdapter.editItem(0, getControlSwitchTitle(CONTROL_SWITCH_ON), getControlSwitchSubtitle(CONTROL_SWITCH_ON));
					mControlsAdapter.enableItem(1);
				}
				else if(text.equals(CONTROL_STOP_SERVICE)) {
					System.out.println("Stopping service. "+isServiceStarted());
					startMainService(MainService.INTENT_OBJECTIVE.KILL_SERVICE);


					mControlsAdapter.editItem(0, getControlSwitchTitle(CONTROL_SWITCH_OFF), getControlSwitchSubtitle(CONTROL_SWITCH_OFF));
					mControlsAdapter.disableItem(1);
					
				}
				else if(text.equals(CONTROL_CHECK_FORCE)) {
					if(titleText.isEnabled()) {
						startMainService(MainService.INTENT_OBJECTIVE.FORCE_CHECK);
					}
				}

			}
		});
		
	}
	
	@Override
	public void onResume() {
		this.registerReceiver(mReceiver, new IntentFilter(MainService.BROADCAST_UPDATES));
		super.onResume();
	}
	
	@Override
	public void onPause() {
		this.unregisterReceiver(mReceiver);
		super.onPause();
	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			boolean enableTraceButton = false;
			
			String type = intent.getStringExtra("type");

			Log.i("Broadcast", type+":"+intent.getStringExtra(type));
			
			if(type.equals(MainService.UPDATES.TRACING)) {
				StringBuilder info = new StringBuilder();
				List<ScanResult> list = MainService.getLastScan();
				Location location = MainService.getLastLocation();
				int len = list.size();
				info.append(String.format("%.5f", location.getLatitude())+", "+String.format("%.3f", location.getLongitude())+" @"+location.getAccuracy()+"m");
				for(int i=0; i<len; i++) {
					ScanResult scan = list.get(i);
					int signal_level = (int) Math.round((WifiManager.calculateSignalLevel(scan.level, TraceManager.WIFI_SIGNAL_NUM_PRECISION_LEVELS) * 2.2222));
					String t = (signal_level < 10)? " "+signal_level: ""+signal_level;
					String s = (scan.SSID.equals("UCSB Wireless Web"))? " *": "";
					info.append("\n"+t+"%  "+scan.BSSID+s);
				}
				infoTextView.setText(info.toString());
			}
			else if(type.equals(MainService.UPDATES.SIMPLE)) {
				String simple = intent.getStringExtra(MainService.UPDATES.SIMPLE);
				infoTextView.setText(simple);
			}
			else if(type.equals(MainService.UPDATES.SLEEPING)) {
				String text = intent.getStringExtra(MainService.UPDATES.SLEEPING);
				infoTextView.setText(text);
				enableTraceButton = true;
			}
			
			mControlsAdapter.setItemEnabled(1, enableTraceButton);
		}		
	};

	private int CONTROL_SWITCH_READ = 0x01;
	private int CONTROL_SWITCH_ON   = 0x02;
	private int CONTROL_SWITCH_OFF  = 0x04; 

	private String getControlSwitchTitle(int forceMode) {
		boolean on = ((forceMode == CONTROL_SWITCH_READ) && isServiceStarted()) 
				|| (forceMode == CONTROL_SWITCH_ON);
		return on? CONTROL_STOP_SERVICE: CONTROL_START_SERVICE;
	}

	private String getControlSwitchSubtitle(int forceMode) {
		boolean on = ((forceMode == CONTROL_SWITCH_READ) && isServiceStarted()) 
				|| (forceMode == CONTROL_SWITCH_ON);
		return "Service is "+(on? "running": "stopped")+". Press here to control";
	}
	
	private void setSwitchState(int forceMode) {
		boolean on = ((forceMode == CONTROL_SWITCH_READ) && isServiceStarted()) 
				|| (forceMode == CONTROL_SWITCH_ON);
		checkForceTitleTextView.setEnabled(on);
		checkForceSubtitleTextView.setEnabled(on);
		mControlsAdapter.notifyDataSetChanged();
	}

	private boolean isServiceStarted() {
		return MainService.serviceRunning;
	}

	private void startMainService(String objective) {
		Intent intent = new Intent(this, MainService.class);
		intent.putExtra("objective", objective);
		startService(intent);
	}

	private class Control {
		String mTitle;
		String mSubtitle;
		int mEnabled;

		public Control(String title, String subtitle) {
			mTitle = title;
			mSubtitle = subtitle;
		}

		public String getTitle() {
			return mTitle;
		}

		public String getSubtitle() {
			return mSubtitle;
		}
	}


	private class ControlsAdapter extends ArrayAdapter<Control> {
		private ArrayList<Control> items;
		private ControlHolder controlHolder;

		private class ControlHolder {
			public TextView title;
			public TextView subtitle;
		}

		public ControlsAdapter(Context context, int tvResId, ArrayList<Control> items) {
			super(context, tvResId, items);
			this.items = items;
		}
		
		public void editItem(int index, String title, String subtitle) {
			Control control = items.get(index);
			control.mTitle = title;
			control.mSubtitle = subtitle;
			control.mEnabled = CONTROL_SWITCH_READ;
			notifyDataSetChanged();
		}
		
		public void setItemEnabled(int index, boolean enabled) {
			Control control = items.get(index);
			control.mEnabled = enabled? CONTROL_SWITCH_ON: CONTROL_SWITCH_OFF;
			notifyDataSetChanged();
		}

		public void disableItem(int index) {
			Control control = items.get(index);
			control.mEnabled = CONTROL_SWITCH_OFF;
			notifyDataSetChanged();
		}
		
		public void enableItem(int index) {
			Control control = items.get(index);
			control.mEnabled = CONTROL_SWITCH_ON;
			notifyDataSetChanged();
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.controls_list_item, null);

				controlHolder = new ControlHolder();
				controlHolder.title = (TextView) v.findViewById(R.id.titleTextView);
				controlHolder.subtitle = (TextView) v.findViewById(R.id.subtitleTextView);
				v.setTag(controlHolder);
			} else {
				controlHolder = (ControlHolder) v.getTag(); 
			}

			Control control = items.get(pos);

			if (control != null) {
				controlHolder.title.setText(control.getTitle());
				controlHolder.subtitle.setText(control.getSubtitle());

				if(controlHolder.title.getText().equals(CONTROL_CHECK_FORCE)) {
					checkForceTitleTextView = controlHolder.title;
					checkForceSubtitleTextView = controlHolder.subtitle;
					
					boolean started = ((control.mEnabled == CONTROL_SWITCH_READ) && isServiceStarted())
								|| (control.mEnabled == CONTROL_SWITCH_ON);
					controlHolder.title.setEnabled(started);
					controlHolder.subtitle.setEnabled(started);
				}
				else {
					controlHolder.title.setEnabled(true);
					controlHolder.subtitle.setEnabled(false);
				}
			}

			return v;
		}
	}
}
