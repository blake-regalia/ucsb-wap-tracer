package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

public class Timeout {
	
	private static final int DEFAULT_SIZE = 16;
	private static int mSize = DEFAULT_SIZE;
	
	private static Timeout[] timeouts = new Timeout[mSize];
	private static boolean[] indicies = new boolean[mSize];

	private static int index = 0;

	
	private int mId;
	private Timer mTimer;
	private Runnable mTask;
	private boolean cancelled = false;
	
	private Timeout(int id, Runnable task, final long delay) {
		mId = id;
		mTimer = new Timer();
		mTask = task;
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if(cancelled) {
					Log.e("Timeout","task was cancelled but it's still running!");		
				}
				else {
					indicies[mId] = false;
					mTask.run();
				}
			}
		}, delay);
	}
	
	private void cancel() {
		cancelled = true;
		mTimer.cancel();
	}
	
	public static int setTimeout(Runnable task, long delay) {
		int began = index;
		while(indicies[index] == true) {
			index += 1;
			index %= mSize;
			if(index == began) {
				System.err.println("ERROR: RAN OUT OF TIMEOUTS");
			}
		}
		timeouts[index] = new Timeout(index, task, delay);
		indicies[index] = true;
		return index;
	}
	
	public static void clearTimeout(int id) {
		Timeout timeout = timeouts[id];
		timeout.cancel();
		timeouts[id] = null;
		indicies[id] = false;
	}
}
