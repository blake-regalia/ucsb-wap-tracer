package edu.ucsb.geog.blakeregalia;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

public class JSON_Output extends DefaultOutput {
	
	private String tab;
	private StringBuilder out;
	
	private Hashtable<String, Integer> mac_addrs;
	
	private boolean pretty = true;
	
	public JSON_Output(boolean prettyPrint) {
		pretty = prettyPrint;
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
	
	private void tab_pop() {
		if(pretty)
			tab = tab.substring(0, tab.length() - 1);
	}
	
	private void tab_push() {
		if(pretty)
			tab += "\t";
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
	
	private void end() {
		out.deleteCharAt(out.length()-1);
	}
	
	private void jsonIDs(Hashtable<String, Integer> list) {
		json("length", list.size());
		
		Iterator<Entry<String, Integer>> set = list.entrySet().iterator();
		while(set.hasNext()) {
			Entry<String, Integer> e = set.next();
			int v = ((Integer) e.getValue()).intValue();
			String id = (String) e.getKey();
			json(""+v, id);
		}
	}

	@Override
	public String dump() {
		out = new StringBuilder();
		
		out.append(tab+"{");
		tab_push();
		json("file_size", file_size);
		json("start_time", start_time);
		json("start_latitude", start_latitude);
		json("start_longitude", start_longitude);

		// BSSIDs
		out.append(tab+"\"bssids\":{");
			tab_push();
			jsonIDs(mac_addrs);
			tab_pop();
			end();
		out.append(tab+"},");

		// SSIDs
		out.append(tab+"\"ssids\":{");
			tab_push();
			jsonIDs(ssid_names);
			tab_pop();
			end();
		out.append(tab+"},");
		
		json("length", events.size());

		int index = 0;
		Iterator<WAP_Event> it = events.iterator();
		while(it.hasNext()) {
			WAP_Event event = it.next();

			out.append(tab+"\""+index+"\":{");
			
			tab_push();
			int i = event.getNumWAPs();
			json("time", event.getTime());
			json("latitude", event.getLat());
			json("longitude", event.getLon());
			json("accuracy", event.getAccuracy());
			json("length", i);
			
			while(i-- != 0) {
				WAP wap = event.getWAP(i);
				
				out.append(tab+"\""+i+"\":{");
				tab_push();
				// wap.getMAC()
				json("mac", mac_addrs.get(wap.getMAC()).intValue());
				json("rssi", wap.getRSSI());
				json("ssid", wap.getSSID());
				end();
				tab_pop();
				out.append(tab+"},");
			}
			end();
			
			tab_pop();
			
			out.append(tab+"},");
			
			index += 1;
		}
		end();
		
		tab_pop();
		out.append(tab+"}");
		return out.toString();
	}
	
}
