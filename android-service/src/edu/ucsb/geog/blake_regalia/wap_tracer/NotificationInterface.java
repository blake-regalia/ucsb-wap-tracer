package edu.ucsb.geog.blake_regalia.wap_tracer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotificationInterface {

	private static String NOTIFY_TAG = "notify";

	Context mContext;	
	NotificationManager mNotificationManager;

	public NotificationInterface(Context context) {
		mContext = context;
		mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void clear() {
		mNotificationManager.cancelAll();
	}
	
	public void post(Class activity, String action, boolean launchNow, String tickerText, String statusText) {
		post(mContext, activity, action, launchNow, tickerText, statusText);
	}
	
	public static void clear(Context context) {
		((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
	}

	public static void post(Context context, Class activity, String action, boolean launchNow, String tickerText, String statusText) {
		post(context, activity, action, launchNow, tickerText, statusText, Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);
	}

	public static void post(Context context, Class activity, String action, boolean launchNow, String tickerText, String statusText, int notificationFlags) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		long now = System.currentTimeMillis(); 
		Notification notification = new Notification(R.drawable.earth, tickerText, now);

		notification.flags = notificationFlags;

		Intent notificationIntent = new Intent(context, activity)
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			.setAction(action);

		if(!action.equals("nothing")) {
			if(launchNow) {
				context.startActivity(notificationIntent);
			}
		}

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, tickerText, statusText, contentIntent);

		notificationManager.notify(0, notification);
	}
}
