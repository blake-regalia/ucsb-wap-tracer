package edu.ucsb.geog.blake_regalia.wap_tracer;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.util.Pair;

public class TraceManager_GPS extends TraceManager {
	
	private static final String TAG = "TraceManager(GPS)";

	private long[] coordinatePair; 

	public TraceManager_GPS(Context context, long timeStart, long[] coordinates) {
		super(context, timeStart);
		coordinatePair = coordinates;
	}

	@Override
	public boolean openFile() {

		/* generate the file header */
		ByteBuilder byteBuilder = new ByteBuilder(4 + 8 + 8 + 8);

		// version: 4 bytes
		byteBuilder.append_4(
				Encoder.encode_int(Registration.VERSION)
				);

		// start time [offset basis]: 8 bytes
		byteBuilder.append_8(
				Encoder.encode_long(locationSensorProviderTimeStart)
				);

		// latitude basis as long: 8 bytes
		byteBuilder.append_8(
				Encoder.encode_long((long) (coordinatePair[0] * CONVERT_COORDINATE_PRECISION_LONG))
				);

		// longitude basis as long: 8 bytes
		byteBuilder.append_8(
				Encoder.encode_long((long) (coordinatePair[1] * CONVERT_COORDINATE_PRECISION_LONG))
				);


		/* create file & write file header */
		String traceFileName = getDateString()+".location-gps.bin";
		if(openTraceFile(traceFileName)) {
			return writeToTraceFile(byteBuilder.getBytes());
		}

		return false;
	}

	@Override
	public boolean closeFile() {
		Log.d(TAG, "closing trace file");
		
		if(closeTraceFile()) {
			return true;
		}

		return false;
	}

	public int recordEvent(Location location) {
		ByteBuilder byteBuilder = new ByteBuilder(4 + 4);

		// positive time offset (centi-seconds): 4 bytes
		byteBuilder.append_4(
				Encoder.encode_int(
						(int) ((location.getTime()-locationSensorProviderTimeStart) * CONVERT_MILLISECONDS_CENTISECONDS)
						)
				);

		// latitude: 4 bytes
		byteBuilder.append_4(
				Encoder.encode_double_to_4_bytes(
						location.getLatitude()-coordinatePair[0], CONVERT_COORDINATE_PRECISION_LONG
						)
				);

		// longitude: 4 bytes
		byteBuilder.append_4(
				Encoder.encode_double_to_4_bytes(
						location.getLongitude()-coordinatePair[1], CONVERT_COORDINATE_PRECISION_LONG
						)
				);


		// altitude [-32,768, -32,767]: 2 bytes
		byteBuilder.append_4(
				Encoder.encode_short(
						(short) Math.round(location.getAltitude())
						)
				);

		// accuracy [0, 255]: 1 byte
		int accuracy = Math.round(location.getAccuracy());
		if(accuracy < 0) accuracy = 0;
		else if(accuracy > 255) accuracy = 255;
		byteBuilder.append(
				Encoder.encode_byte(
						accuracy
						)
				);
		
		// for some reason, this looper is shutting down
		if(shutdownReason != REASON_NONE) {
			switch(shutdownReason) {
			case REASON_IO_ERROR:
				return SensorLooper.REASON_IO_ERROR;
				
			case REASON_TIME_EXCEEDING:
				return SensorLooper.REASON_DATA_FULL;
			
			default:
				return SensorLooper.REASON_UNKNOWN;
			}
		}

		// attempt to write the current loop data to the trace file
		if(writeToTraceFile(byteBuilder.getBytes())) {
			return SensorLooper.REASON_NONE;
		}
		else {
			return SensorLooper.REASON_UNKNOWN;
		}
	}

}
