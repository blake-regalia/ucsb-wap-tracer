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
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ActivityControl extends Activity {

	private Activity self;

	private final String CONTROL_START_SERVICE = "Start Service";
	private final String CONTROL_STOP_SERVICE = "Stop Service";

	@Override
	public void onCreate(Bundle savedInst) {
		super.onCreate(null);

		self = this;

		this.setContentView(R.layout.controls);
		ArrayList<Control> controlList = new ArrayList<Control>();

		controlList.add(
				new Control(
						getControlSwitchTitle(CONTROL_SWITCH_READ),
						getControlSwitchSubtitle(CONTROL_SWITCH_READ)
						)
				);

		ListView lv = (ListView) this.findViewById(R.id.controlsListView);
		lv.setAdapter(new ControlsAdapter(this, R.layout.controls_list_item, controlList));

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final TextView titleText = (TextView) ((LinearLayout) view).getChildAt(0);
				final TextView subtitleText = (TextView) ((LinearLayout) view).getChildAt(1);

				String text = (String) titleText.getText();

				boolean updateControlSwitch = false;
				if(text.equals(CONTROL_START_SERVICE)) {
					System.out.println("Starting service");
					startMainService(MainService.INTENT_OBJECTIVE.START);

					titleText.setText(getControlSwitchTitle(CONTROL_SWITCH_ON));
					subtitleText.setText(getControlSwitchSubtitle(CONTROL_SWITCH_ON));
				}
				else if(text.equals(CONTROL_STOP_SERVICE)) {
					System.out.println("Stopping service. "+isServiceStarted());
					startMainService(MainService.INTENT_OBJECTIVE.KILL_SERVICE);

					titleText.setText(getControlSwitchTitle(CONTROL_SWITCH_OFF));
					subtitleText.setText(getControlSwitchSubtitle(CONTROL_SWITCH_OFF));
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
		return "Service is currently "+(on? "running": "stopped")+". Press here to control";
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
			}

			return v;
		}
	}
}
