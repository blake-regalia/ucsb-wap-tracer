package edu.ucsb.geog.blakeregalia;

import java.util.Iterator;
import java.util.Map.Entry;

public class JSON_Output extends DefaultOutput {
	
	private String tab;
	private StringBuilder out;
	
	private boolean pretty = true;
	
	private void tab_pop() {
		if(pretty)
			tab = tab.substring(0, tab.length() - 1);
	}
	
	private void tab_push() {
		if(pretty)
			tab += "\t";
	}
	
	public JSON_Output(boolean prettyPrint) {
		pretty = prettyPrint;
		tab = pretty? "\n": "";
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
	
	private void jsonSSIDs() {
		json("length", ssid_names.size());
		
		Iterator<Entry<String, Integer>> set = ssid_names.entrySet().iterator();
		while(set.hasNext()) {
			Entry<String, Integer> e = set.next();
			int v = ((Integer) e.getValue()).intValue();
			String ssid = (String) e.getKey();
			json(""+v, ssid);
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
		
		out.append(tab+"\"ssids\":{");
			tab_push();
			jsonSSIDs();
			tab_pop();
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
				json("mac", wap.getMAC());
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
