package edu.ucsb.geog.blakeregalia;

import edu.ucsb.geog.blakeregalia.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WAP_Tracer_Activity extends Activity implements OnClickListener {
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);

	    Button button_start = (Button) findViewById(R.id.start_service);
	    Button button_stop = (Button) findViewById(R.id.stop_service);

	    button_start.setOnClickListener(this);
	    button_stop.setOnClickListener(this);
	}

	public void onClick(View view) {
		switch(view.getId()) {
		case R.id.start_service:
			start_service();
			break;
		case R.id.stop_service:
			stop_service();
			break;
		}
		
		finish();
	}

	private void start_service() {
		Intent intent = new Intent(this, WAP_Tracer_Service.class);
		startService(intent);
	}
	
	private void stop_service() {
		Intent intent = new Intent(this, WAP_Tracer_Service.class);
		intent.putExtra("objective", "shutdown");
		stopService(intent);
	}
}
