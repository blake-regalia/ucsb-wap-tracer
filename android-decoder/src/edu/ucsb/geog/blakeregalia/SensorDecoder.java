package edu.ucsb.geog.blakeregalia;

import java.io.File;

public abstract class SensorDecoder extends BasicDecoder {

	public SensorDecoder() {
		super();
	}

	public abstract String output(OutputRenderer outputRenderer, LocationTrace locationTrace);
	
}
