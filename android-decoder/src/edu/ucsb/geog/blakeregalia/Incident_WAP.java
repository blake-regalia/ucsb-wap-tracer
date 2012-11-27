package edu.ucsb.geog.blakeregalia;


public class Incident_WAP extends SensorIncident {
	
	private int wapSizeMax;
	private int wapIndex;
	private Wap[] wapList;
	
	public Incident_WAP(int _wapSizeMax, long _timeOffset) {
		super(_timeOffset);
		wapSizeMax = _wapSizeMax;
		wapIndex = 0;
		wapList = new Wap[wapSizeMax];
	}
	
	public void add(Wap wap) {
		wapList[wapIndex++] = wap;
	}
	
	public int getSize() {
		return wapIndex;
	}
	
	public Wap get(int index) {
		return wapList[index];
	}

}
