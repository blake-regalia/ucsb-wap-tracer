package edu.ucsb.geog.blake_regalia.wap_tracer;

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

	public void postNotification(Class activity, String uiPurpose, boolean launchNow, String tickerText, String statusText) {
		long now = System.currentTimeMillis(); 
		Notification notification = new Notification(R.drawable.earth, tickerText, now);

		CharSequence contentTitle = tickerText;
		CharSequence contentText = statusText;

		notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

		if(!uiPurpose.equals(MainService.ACTIVITY_INTENT.DO_NOTHING)) {
			Intent notificationIntent = new Intent(mContext, activity)
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			.setAction(this.getClass().getPackage().getName()+":"+uiPurpose)
			.putExtra("ui-purpose", uiPurpose);

			PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

			notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);

			if(launchNow) {
				mContext.startActivity(notificationIntent);
			}
		}
		else {
			Intent notificationIntent = new Intent(mContext, activity)
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			.setAction(this.getClass().getPackage().getName()+":"+uiPurpose)
			.putExtra("ui-purpose", "do-nothing");

			PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

			notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);
		}

		mNotificationManager.notify(0, notification);
	}
}
