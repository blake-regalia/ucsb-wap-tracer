package edu.ucsb.geog.blakeregalia;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class _FileReader {
	private File file;
	private FileInputStream fis;

	public _FileReader(String file_name) {
		file = new File(file_name);
		if(!file.isFile()) {
			System.err.println("\""+file_name+"\" is not a file.");
			System.exit(1);
		}
		else if(!file.canRead()) {
			System.err.println("i don't have permission to read file \""+file_name+"\".");
			System.exit(1);
		}

		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}

	public int read() {
		try {
			return fis.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		return -1;
	}

	public void read(byte[] b) {
		try {
			fis.read(b);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public char read_char() {
		byte[] b = new byte[2];
		read(b);
		return (char) ((((char) (b[0] & 0xff)) << 8) | ((char) (b[1] & 0xff)));
	}
	
	public int read_int() {
		byte[] b = new byte[4];
		read(b);
		return (int) (((int) (b[0] & 0xff) << 24) | ((int) (b[1] & 0xff) << 16) | ((int) (b[2] &  0xff) << 8) | ((int) (b[3] & 0xff)));
	}

	public long read_long() {
		byte[] b = new byte[8];
		read(b);
		return ((long) (b[0] & 0xff) << 56) | ((long) (b[1] & 0xff) << 48) | ((long) (b[2] & 0xff) << 40) | ((long) (b[3] & 0xff) << 32)
				| ((long) (b[4] & 0xff) << 24) | ((long) (b[5] & 0xff) << 16) | ((long) (b[6] & 0xff) << 8) | ((long) (b[7] & 0xff));
	}

	public long size() {
		return file.length();
	}
	
	public void close() {
		try {
			fis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
}
