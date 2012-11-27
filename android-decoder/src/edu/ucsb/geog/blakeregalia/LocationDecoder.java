package edu.ucsb.geog.blakeregalia;

import java.io.File;

public class LocationDecoder extends BasicDecoder {

	private double basisLatitude;
	private double basisLongitude;
	private LocationTrace mTrace;

	public LocationDecoder(File file) {
		super();
		decode(file);
	}

	@Override
	public void decodePrimary() {
		
		System.out.println("LocationDecoder::decode()");

		// declare a new trace
		mTrace = new LocationTrace(propertyTimeStart);

		// store basis coordinates
		basisLatitude = mFileReader.read_long() * CONVERT_COORDINATE_PRECISE_DOUBLE;
		basisLongitude = mFileReader.read_long() * CONVERT_COORDINATE_PRECISE_DOUBLE;

		System.out.println("basis latitude: "+basisLatitude);
		System.out.println("basis longitude: "+basisLongitude);
		
		while(true) {
			// accuracy in meters
			float accuracy = (float) mFileReader.read_byte();
			
			// break if end of position block reached
			if(accuracy < 0) {
				break;
			}
			
			// altitude in meters
			float altitude = (float) mFileReader.read_char();
			
			// time offset from start in centi-seconds
			long timestamp = (long) (mFileReader.read_int() * CONVERT_CENTISECONDS_MILLISECONDS) + propertyTimeStart;

			// latitude & longitude
			double latitude = basisLatitude + ((float) mFileReader.read_int()) * CONVERT_COORDINATE_PRECISE_DOUBLE;
			double longitude = basisLongitude + ((float) mFileReader.read_int()) * CONVERT_COORDINATE_PRECISE_DOUBLE;

			// commit this incident to the trace
			mTrace.add(
					new Location(latitude, longitude, timestamp, accuracy, altitude)
				);
		}

		// close the trace file
		mFileReader.close();
	}
	
	public LocationTrace getTrace() {
		return mTrace;
	}
	
}
