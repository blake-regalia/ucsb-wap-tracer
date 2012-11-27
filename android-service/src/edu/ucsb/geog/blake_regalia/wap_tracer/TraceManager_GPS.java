package edu.ucsb.geog.blake_regalia.wap_tracer;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.util.Pair;

public class TraceManager_GPS extends TraceManager {

	private static final String TAG = "TraceManager(GPS)";

	private long[] coordinatePair; 
	private String traceType;

	public TraceManager_GPS(Context context, long timeStart, double[] coordinates, String _traceType) {
		super(context, timeStart);
		coordinatePair = new long[2];
		coordinatePair[0] = (long) (coordinates[0] * CONVERT_COORDINATE_PRECISION_LONG);
		coordinatePair[1] = (long) (coordinates[1] * CONVERT_COORDINATE_PRECISION_LONG);
		traceType = _traceType;
	}

	@Override
	public boolean openFile() {

		/* generate the file secondary header */
		ByteBuilder byteBuilder = new ByteBuilder(8 + 8);

		// latitude basis as long: 8 bytes
		byteBuilder.append_8(
				Encoder.encode_long(coordinatePair[0])
				);

		// longitude basis as long: 8 bytes
		byteBuilder.append_8(
				Encoder.encode_long(coordinatePair[1])
				);


		/* create file & write file header */
		String traceFileName = getDateString()+"."
				+traceType
				+(traceType.equals("location")? "": "-gps")
				+".bin";
		if(openTraceFile(traceFileName, ID_TYPE_TRACE_GPS, sensorLocationProviderTimeStart)) {
			return writeToTraceFile(byteBuilder.getBytes());
		}

		return false;
	}

	@Override
	public boolean closeFile() {
		Log.d(TAG, "closing trace file");

		byte[] negativeOne = {-1};
		return 
				// signify end of entry block
				writeToTraceFile(negativeOne)

				// close file
				&& closeTraceFile();
	}

	public int recordEvent(Location location) {
		ByteBuilder byteBuilder = new ByteBuilder(1 + 2 + 4 + 4 + 4);

		// accuracy [0, 255]: 1 byte
		int accuracy = Math.round(location.getAccuracy());
		if(accuracy < 0) accuracy = 0;
		else if(accuracy > 255) accuracy = 255;
		byteBuilder.append(
				Encoder.encode_byte(
						accuracy
						)
				);

		// altitude [-32,768, -32,767]: 2 bytes
		byteBuilder.append_2(
				Encoder.encode_short(
						(short) Math.round(location.getAltitude())
						)
				);

		// positive time offset (centi-seconds): 4 bytes
		byteBuilder.append_4(
				Encoder.encode_int(
						(int) ((location.getTime()-sensorLocationProviderTimeStart) * CONVERT_MILLISECONDS_CENTISECONDS)
						)
				);

		// latitude: 4 bytes
		byteBuilder.append_4(
				Encoder.encode_int(
						(int) ((location.getLatitude()*CONVERT_COORDINATE_PRECISION_LONG) - coordinatePair[0])
						)
				);

		// longitude: 4 bytes
		byteBuilder.append_4(
				Encoder.encode_int(
						(int) ((location.getLongitude()*CONVERT_COORDINATE_PRECISION_LONG) - coordinatePair[1])
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
