package edu.ucsb.geog.blakeregalia;

import java.util.Date;
import java.util.ListIterator;

public class CSV_Output extends DefaultOutput {
	
	public CSV_Output() {
		super(false);
	}

	public String dump() {
		StringBuilder out = new StringBuilder();
		out.append("Latitude,Longitude,Time,Signal_Strength,Accuracy");

		ListIterator<WAP_Event> it = events.listIterator();
		while(it.hasNext()) {
			WAP_Event event = it.next();
			out.append(
				event.getLat()+","+event.getLon()+","+(new Date(event.getTime()))+","+
				
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
					+ "\n"
				);
			}
			out.append("\n");
		}
		out.append(getSSIDs());
		return out.toString();
	}
}
