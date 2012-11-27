package edu.ucsb.geog.blakeregalia;

import java.io.File;

public abstract class BasicDecoder {

	protected static final int ID_TYPE_TRACE_GPS  = 0xFF01;
	protected static final int ID_TYPE_TRACE_WIFI = 0xFF02;
	protected static final int ID_TYPE_TRACE_WAP  = 0xFF03;

	// 1.25s = 12.5ds = 125cs = 1250ms
	protected static final long CONVERT_DECISECONDS_MILLISECONDS  = (long) 1e+2;
	protected static final long CONVERT_CENTISECONDS_MILLISECONDS = (long) 1e+1;

	protected static final double CONVERT_COORDINATE_PRECISE_DOUBLE = 1e-6;

	protected int propertyVersion;
	protected int propertySensorType;
	protected long propertyTimeStart;
	
	protected long metadataFileSize;
	
	protected File mFile;
	protected ByteDecodingFileReader mFileReader;
	
	public BasicDecoder() {
		
	}
	
	protected void decodeHeader() {
		// version: 4 bytes
		propertyVersion = mFileReader.read_int();
		
		// sensor type: 4 bytes
		propertySensorType = mFileReader.read_int();
		
		// start timestamp: 8 bytes
		propertyTimeStart = mFileReader.read_long();
	}
	
	public abstract void decodePrimary();
	
	public void decode(File file) {
		mFile = file;
		mFileReader = new ByteDecodingFileReader(mFile);
		metadataFileSize = mFileReader.size();
		
		decodeHeader();
		decodePrimary();
	}

}
