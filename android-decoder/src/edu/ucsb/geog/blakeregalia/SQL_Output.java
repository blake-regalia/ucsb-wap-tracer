package edu.ucsb.geog.blakeregalia;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

public class SQL_Output extends DefaultOutput {
	
	private static final int INITIAL_HASH_TABLE_SIZE = 192;

	private Hashtable<String, UWAP> waps;
	private Hashtable<Integer, String> ssids;
	
	private String table;
	
	StringBuilder out;
	
	private class UWAP {
		ArrayList<Pair<WAP_Event, WAP>> events;
		Pair<WAP_Event, WAP> pair;
		private int ssid;
		
		public UWAP(WAP_Event evt, WAP wap) {
			events = new ArrayList<Pair<WAP_Event, WAP>>();
			events.add(new Pair<WAP_Event, WAP>(evt, wap));
			ssid = wap.getSSID();
		}
		
		public void add(WAP_Event evt, WAP wap) {
			events.add(new Pair<WAP_Event, WAP>(evt, wap));
		}
		
		public int getSSID() {
			return ssid;
		}
		
		public WAP_Event getEvent(int index) {
			pair = events.get(index);
			return pair.getLeft();
		}
		
		public int getRSSI(int index) {
			pair = events.get(index);
			return pair.getRight().getRSSI();
		}
		
		public int size() {
			return events.size();
		}
	}

	public SQL_Output(boolean prettyPrint, String sql_table) {
		super(prettyPrint);
		waps = new Hashtable<String, UWAP>(INITIAL_HASH_TABLE_SIZE);
		ssids = new Hashtable<Integer, String>();
		table = sql_table;
	}
	
	@Override
	public void addEvent(WAP_Event event) {
		int i = event.getNumWAPs();
		while(i-- != 0) {
			WAP wap = event.getWAP(i);
			String mac = wap.getMAC();
			UWAP uwap = waps.get(mac);
			if(uwap == null) {
				uwap = new UWAP(event, wap);
				waps.put(wap.getMAC(), uwap);
			}
			else {
				uwap.add(event, wap);
			}
		}
		events.add(event);
	}
	
	@Override
	public void setSSID(int key, String ssid) {
		ssids.put(new Integer(key), ssid);
	}
	
	private void end() {
		out.append(";");
	}
	
	private void create_table() {
		out.append(tab+"CREATE TABLE IF NOT EXISTS `"+table+"` (");
		tab_push();
		out.append(tab+"`bssid` varchar(17) NOT NULL,");
		out.append(tab+"`time` varchar(13) NOT NULL,");
		out.append(tab+"`accuracy` tinyint(3) unsigned NOT NULL,");
		out.append(tab+"`latitude` double NOT NULL,");
		out.append(tab+"`longitude` double NOT NULL,");
		out.append(tab+"`rssi` tinyint(3) unsigned NOT NULL");
		tab_pop();
		out.append(tab+")");
		end();
		out.append(tab);
	}
	
	private void create_wap_table() {
		out.append(tab+"CREATE TABLE IF NOT EXISTS `waps` (");
		tab_push();
		out.append(tab+"`bssid` varchar(17) NOT NULL,");
		out.append(tab+"`ssid` varchar(32) NOT NULL");
		tab_pop();
		out.append(tab+")");
		end();
		out.append(tab);
	}
	
	private void declare_ssid(String bssid, String ssid) {
		
		out.append(tab+"INSERT INTO `waps` (");
		tab_push();
		out.append(tab+"`bssid`,");
		out.append(tab+"`ssid`");
		tab_pop();
		
		out.append(tab+") VALUES (");
		tab_push();
		out.append(tab+"'"+bssid+"',");
		out.append(tab+"'"+ssid+"'");
		tab_pop();
		out.append(tab+")");
		end();
		
		out.append(tab);
	}
	
	@Override
	public String dump() {
		out = new StringBuilder();
		
		create_table();
		
		create_wap_table();
		

		Iterator<Entry<String, UWAP>> set = waps.entrySet().iterator();
		while(set.hasNext()) {
			Entry<String, UWAP> entry = set.next();
			UWAP uwap = entry.getValue();
			
			String bssid = entry.getKey();
			String ssid = ssids.get(new Integer(uwap.getSSID()));

			declare_ssid(bssid, ssid);
			
			for(int i=0; i<uwap.size(); i++) {
				WAP_Event evt = uwap.getEvent(i);
				
				out.append(tab+"INSERT INTO `"+table+"` (");
				
				/**/
				tab_push();
				out.append(tab+"`bssid`,");
				out.append(tab+"`time`,");
				out.append(tab+"`accuracy`,");
				out.append(tab+"`latitude`,");
				out.append(tab+"`longitude`,");
				out.append(tab+"`rssi`");
				tab_pop();
				out.append(tab+") VALUES (");
				/**/
				
				//out.append("`ssid`, `time`, `accuracy`, `latitude`, `longitude`) VALUES (");

				tab_push();
				out.append(tab+"'"+bssid+"',");
				out.append(tab+"'"+evt.getTime()+"',");
				out.append(tab+"'"+evt.getAccuracy()+"',");
				out.append(tab+"'"+evt.getLat()+"',");
				out.append(tab+"'"+evt.getLon()+"',");
				out.append(tab+"'"+uwap.getRSSI(i)+"'");
				tab_pop();
				out.append(tab+")");
				
				end();
			}
			
			out.append(tab);
		}
		
		return out.toString();
	}
}
