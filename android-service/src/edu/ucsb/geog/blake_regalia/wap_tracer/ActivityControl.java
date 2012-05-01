package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

	private TextView appStatus;
	private Registration registration;

	private Runnable registrationSuccess = new Runnable() {
		public void run() {
			appStatus.setText("Registered: "+registration.getAndroidId()+"; "+registration.getPhoneNumber());
		}
	};
	private Runnable registrationFailure = new Runnable() {
		public void run() {
			appStatus.setText("Failed to register at this time. An internet connection is required");
		}
	};
	
	@Override
	public void onCreate(Bundle savedInst) {
		super.onCreate(null);

		self = this;

		this.setContentView(R.layout.controls);
		
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
					
					/*
					titleText.setText(getControlSwitchTitle(CONTROL_SWITCH_OFF));
					subtitleText.setText(getControlSwitchSubtitle(CONTROL_SWITCH_OFF));

					setSwitchState(CONTROL_SWITCH_OFF);*/
				}
				else if(text.equals(CONTROL_CHECK_FORCE)) {
					if(titleText.isEnabled()) {
						startMainService(MainService.INTENT_OBJECTIVE.FORCE_CHECK);
					}
				}

			}
		});
	}

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
