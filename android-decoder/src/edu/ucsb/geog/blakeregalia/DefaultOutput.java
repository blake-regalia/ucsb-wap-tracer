package edu.ucsb.geog.blakeregalia;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

public class DefaultOutput {

	protected boolean pretty = true;
	protected String tab;	
	
	protected List<WAP_Event> events;
	protected Hashtable<String, Integer> ssid_names;
	
	protected long file_size;
	protected long start_time;
	protected float start_latitude;
	protected float start_longitude;

	public DefaultOutput() {
		init();
	}
	
	public DefaultOutput(boolean prettyPrint) {
		pretty = prettyPrint;
		init();
	}
	
	private void init() {
		tab = pretty? "\n": "";
		events = new ArrayList<WAP_Event>();
		ssid_names = new Hashtable<String, Integer>(64);
	}
	
	public void initialize(long size, long time, float lat, float lon) {
		file_size = size;
		start_time = time;
		start_latitude = lat;
		start_longitude = lon;
	}

	public void addEvent(WAP_Event event) {
		events.add(event);
	}

	public void setSSID(int key, String ssid) {
		ssid_names.put(ssid, new Integer(key));
	}
	
	private String getSSIDs() {
		StringBuilder list = new StringBuilder();
		list.append("=========================\n");

		Iterator<Entry<String, Integer>> set = ssid_names.entrySet().iterator();
		while(set.hasNext()) {
			Entry<String, Integer> e = set.next();
			int v = ((Integer) e.getValue()).intValue();
			String ssid = (String) e.getKey();
			list.append(v+": "+ssid+"\n");
		}
		
		return list.toString();
	}

	protected void tab_pop() {
		if(pretty)
			tab = tab.substring(0, tab.length() - 1);
	}
	
	protected void tab_push() {
		if(pretty)
			tab += "\t";
	}


	public String dump() {
		StringBuilder out = new StringBuilder();
		ListIterator<WAP_Event> it = events.listIterator();
		while(it.hasNext()) {
			WAP_Event event = it.next();
			out.append(
				(new Date(event.getTime()))+": "
				+ event.getNumWAPs()+" WAP(s); "
				+ "["+event.getLat()+", "+event.getLon()+"] "
				+ "@"+event.getAccuracy()+"m; "
				+ "GPS is "+event.getStaleness()+"s old"
				+"\n"
			);
			int num_waps = event.getNumWAPs();
			int i=num_waps;
			while(i-- != 0) {
				WAP wap = event.getWAP(i);
				out.append(
					"\t"+((int) (((float) wap.getRSSI())/2.55f))+"% "
					+ wap.getMAC()+" "
					+ wap.getSSID()
					+ (wap.getFrequency()==0?"":"  "+wap.getFrequency()+"mHz")
					+ "\n"
				);
			}
			out.append("\n");
		}
		out.append(getSSIDs());
		return out.toString();
	}

}
