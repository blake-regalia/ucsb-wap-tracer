package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.util.Timer;
import java.util.TimerTask;

public class Timeout {
	
	private static final int DEFAULT_SIZE = 16;
	
	private int mId;
	private Timer mTimer;
	private Runnable mTask;
	private boolean cancelled = false;
	
	private static Timeout[] timeouts = new Timeout[DEFAULT_SIZE];
	private static boolean[] indicies = new boolean[DEFAULT_SIZE];

	private static int index = 0;
	
	private Timeout(int id, Runnable task, final long delay) {
		mId = id;
		mTimer = new Timer();
		mTask = task;
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				indicies[mId] = false;
				if(cancelled) {
					System.out.println("task was cancelled but it's still running!");					
				}
				if(!cancelled) mTask.run();
			}
		}, delay);
	}
	
	private void cancel() {
		cancelled = true;
		mTimer.cancel();
	}
	
	public static void setSize(int size) {
		timeouts = new Timeout[size];
		indicies = new boolean[size];
	}
	
	public static int setTimeout(Runnable task, long delay) {
		int began = index;
		while(indicies[index] == true) {
			index += 1;
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
