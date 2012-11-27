package edu.ucsb.geog.blakeregalia;

public class LocationTrace extends SensorTrace {
	
	private long timeStart;

	public LocationTrace(long _timeStart) {
		timeStart = _timeStart;
	}
	
	@Override
	public Location get(int index) {
		return (Location) incidentList.get(index);
	}
	
	public long getStartTime() {
		return timeStart;
	}
	
	
	public Location interpolatePosition(long timestamp) {
//		System.err.println(timeStart); System.exit(1);
		
		int listSize = incidentList.size();
		int searchSize = listSize;
		int searchOffset = 0;
		
		int midpoint;
		Location test;
		Location other;
		long otherTime;
		
		// binary search for two times on either side of timestamp
		while(true) {
			midpoint = (searchSize / 2) + searchOffset;
//			System.out.println("LocationTrace:: midpoint:"+midpoint+"; searchSize:"+searchSize+"; searchOffset:"+searchOffset);
			test = ((Location) incidentList.get(midpoint));
			long testTime = test.getTime();
			
			if(timestamp < testTime) {
				if(midpoint == 0) {
					System.err.println("Failed to interpolate position. Timestamp preceeds start of LocationTrace"
							+"\n\t"+timestamp+" < "+testTime+" list["+midpoint+"]");
					return test;
				}
				other = ((Location) incidentList.get(midpoint-1));
				otherTime = other.getTime();
				if(timestamp > other.getTime()) {
					// interpolate between test & other
					return new Location(other, test, timestamp);
				}
				searchSize = midpoint - searchOffset;
			}
			
			else if(timestamp > testTime) {
				if(midpoint+1 == listSize) {
					System.err.println("Failed to interpolate position. Timestamp exceeds end of LocationTrace. "
							+"\n\t"+timestamp+" > "+testTime+" list["+midpoint+"]");
					return test;
				}
				other = ((Location) incidentList.get(midpoint+1));
				if(timestamp < other.getTime()) {
					// interpolate between test & other
					return new Location(test, other, timestamp);
				}
				searchSize = (searchSize + searchOffset- midpoint);
				searchOffset = midpoint;
			}
			
			else if(testTime == timestamp) {
				return test;
			}
		}
	}
	
}
