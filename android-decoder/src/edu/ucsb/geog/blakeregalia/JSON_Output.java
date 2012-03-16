package edu.ucsb.geog.blakeregalia;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

public class JSON_Output extends DefaultOutput {
	
	private StringBuilder out;
	
	private Hashtable<String, Integer> mac_addrs;
	
	public JSON_Output(boolean prettyPrint) {
		super(prettyPrint);
		mac_addrs = new Hashtable<String, Integer>(128);
		tab = pretty? "\n": "";
	}
	
	@Override
	public void addEvent(WAP_Event event) {
		int i = event.getNumWAPs();
		while(i-- != 0) {
			WAP wap = event.getWAP(i);
			String mac = wap.getMAC();
			if(!mac_addrs.containsKey(mac)) {
				mac_addrs.put(wap.getMAC(), new Integer(mac_addrs.size()));
			}
		}
		events.add(event);
	}
	
	private void json(String key, int value) {
		out.append(tab+"\""+key+"\":"+value+",");
	}

	private void json(String key, long value) {
		out.append(tab+"\""+key+"\":"+value+",");
	}
	
	private void json(String key, float value) {
		out.append(tab+"\""+key+"\":"+value+",");
	}

	private void json(String key, String value) {
		out.append(tab+"\""+key+"\":\""+value+"\",");
	}
	
	private void begin(int id) {
		begin(id+"");
	}
	
	private void begin(String id) {
		out.append(tab+"\""+id+"\":{");
		tab_push();
	}
	
	private void end() {
		out.deleteCharAt(out.length()-1);
		tab_pop();
		out.append(tab+"},");
	}
	
	private void start() {
		out.append(tab+"{");
		tab_push();
	}
	
	private void finish() {
		out.deleteCharAt(out.length()-1);
	}
	
	private void jsonIDs(Hashtable<String, Integer> list) {
		json("length", list.size());
		
		Iterator<Entry<String, Integer>> set = list.entrySet().iterator();
		while(set.hasNext()) {
			Entry<String, Integer> e = set.next();
			int v = ((Integer) e.getValue()).intValue();
			String id = (String) e.getKey();
			json(""+v, id.replaceAll("\"", "\\\\\""));
		}
	}

	@Override
	public String dump() {
		out = new StringBuilder();
		
		start();
		
		json("file_size", file_size);
		json("start_time", start_time);
		json("start_latitude", start_latitude);
		json("start_longitude", start_longitude);

		// BSSIDs
		begin("bssids");
			jsonIDs(mac_addrs);
		end();

		// SSIDs
		begin("ssids");
			jsonIDs(ssid_names);
		end();
		
		json("length", events.size());

		int index = 0;
		Iterator<WAP_Event> it = events.iterator();
		while(it.hasNext()) {
			WAP_Event event = it.next();

			begin(index);
			
			int i = event.getNumWAPs();
			json("time", event.getTime());
			json("latitude", event.getLat());
			json("longitude", event.getLon());
			json("accuracy", event.getAccuracy());
			json("length", i);
			
			while(i-- != 0) {
				WAP wap = event.getWAP(i);

				begin(i);
				
				json("mac", mac_addrs.get(wap.getMAC()).intValue());
				json("rssi", wap.getRSSI());
				json("ssid", wap.getSSID());
				
				end();
			}
			end();
			
			index += 1;
		}
		end();
		
		finish();
		
		return out.toString();
	}
	
}
