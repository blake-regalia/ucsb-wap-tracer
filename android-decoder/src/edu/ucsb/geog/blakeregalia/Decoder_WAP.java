package edu.ucsb.geog.blakeregalia;

import sun.io.Converters;


public class Decoder_WAP extends SensorDecoder {
	
	private static final boolean DEBUG = true;
	
	private SensorTrace_WAP mTrace;
	
	public Decoder_WAP() {
		super();
	}

	@Override
	public void decodePrimary() {
		
		// declare a new trace
		mTrace = new SensorTrace_WAP();

		// iterate through incidents
		while(true) {
			
			// number of wap entries: 1 byte
			byte wapSize = mFileReader.read_byte();

			if(DEBUG) {
				System.out.println("@"+mFileReader.bytes_read+": "+wapSize+" wap(s)");
			}
			
			// check if there are no more wap entries to read
			if(wapSize == 0) {
				break;
			}
			
			// time offset (centi-seconds): 2 bytes
			long timeOffset = (long) (mFileReader.read_char() * CONVERT_CENTISECONDS_MILLISECONDS);

			if(DEBUG) {
				System.out.println("@"+mFileReader.bytes_read+": +"+(timeOffset)+" ms");
			}
	
			// prepare a new record of this wap incident
			Incident_WAP incident = new Incident_WAP(wapSize, timeOffset);
			
			for(int i=0; i<wapSize; i++) {
				// BSSID hardware address of wap: 6 bytes
				byte[] bssid = new byte[6];
				mFileReader.read(bssid);
				
				// SSID name identifier: 1 byte
				byte ssid = mFileReader.read_byte();
				
				// frequency of channel: 2 bytes
				char frequency = mFileReader.read_char();
				
				// RSSI signal strength: 1 byte
				byte rssi = mFileReader.read_byte();
				
				// commit this wireless access point to the incident
				incident.add(
						new Wap(bssid, ssid, frequency, rssi)
						);
				
				if(DEBUG) {
					System.out.println("@"+mFileReader.bytes_read+": "+rssi+"dBm / "+((int)frequency)+"mHz");
				}
			}
			
			// commit this incident to the trace
			mTrace.add(incident);
		}
		
		// deocde the lookup table of ssid names
		while(mFileReader.bytes_read != metadataFileSize) {
			int ssidKey = mFileReader.read_int();
			if(DEBUG) System.out.println("@"+mFileReader.bytes_read+": ssid-key => "+ssidKey);
			
			String ssidName = mFileReader.read_string();
			if(DEBUG) System.out.println("@"+mFileReader.bytes_read+": \tssid-name => "+ssidName);
			
			mTrace.setSsidName(ssidKey, ssidName);
		}
		
		// close the open file
		mFileReader.close();

		// return the trace
		//return mTrace;
	}
	

	@Override
	public String output(OutputRenderer outputRenderer, LocationTrace locationTrace) {
		
		long startTime = propertyTimeStart;
		
		// iterate over each incident
		int incidentSize = mTrace.getIncidentSize();
		
		System.out.println("# incidents: "+incidentSize);
		for(int i=0; i<incidentSize; i++) {
			Incident_WAP incident = mTrace.get(i);
			
			// get absolute time that incident occurred
			long timestamp = incident.getTime(startTime);

			// get the precise location of the device at that time, given this locationTrace
			Location resolvedLocation = locationTrace.interpolatePosition(timestamp);
			
			// set the location for given number of subsequent entities
			outputRenderer.setLocation(resolvedLocation, incident.getSize());
			
			System.out.println("incident at +"+incident.getTime(0)+"ms");
			System.out.println("# wap(s): "+incident.getSize());
			
			// for each wap, output as entity
			for(int entry=0; entry<incident.getSize(); entry++) {
				Wap wap = incident.get(entry);
				
				outputRenderer.openEntity();
				outputRenderer.set("bssid", wap.getBSSID());
				outputRenderer.set("ssid", mTrace.ssidKeyToName(wap.getSSID()));
				outputRenderer.set("frequency", wap.getFrequency());
				outputRenderer.set("rssi", wap.getRSSI());
				outputRenderer.closeEntity();
				
			}
			
///			System.exit(1);
		}
		
		// return the output dump as string
		return outputRenderer.dump();
	}
	
	public long getStartTime() {
		return propertyTimeStart;
	}

}
