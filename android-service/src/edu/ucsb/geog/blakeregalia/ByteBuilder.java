package edu.ucsb.geog.blakeregalia;

public class ByteBuilder {
	private int index;
	private int length;
	private byte[] bytes;

	public ByteBuilder(long num_bytes) {
		init((int) num_bytes);
	}
	
	public ByteBuilder(int num_bytes) {
		init(num_bytes);
	}
	
	private void init(int num_bytes) {
		index = 0;
		length = num_bytes;
		bytes = new byte[length];
	}
	
	public void append(byte b) {
		bytes[index++] = b;
	}

	public void append_2(byte[] b) {
		bytes[index++] = b[0];
		bytes[index++] = b[1];
	}

	public void append_4(byte[] b) {
		bytes[index++] = b[0];
		bytes[index++] = b[1];
		bytes[index++] = b[2];
		bytes[index++] = b[3];
	}
	
	public void append_6(byte[] b) {
		bytes[index++] = b[0];
		bytes[index++] = b[1];
		bytes[index++] = b[2];
		bytes[index++] = b[3];
		bytes[index++] = b[4];
		bytes[index++] = b[5];
	}

	public void append_8(byte[] b) {
		bytes[index++] = b[0];
		bytes[index++] = b[1];
		bytes[index++] = b[2];
		bytes[index++] = b[3];
		bytes[index++] = b[4];
		bytes[index++] = b[5];
		bytes[index++] = b[6];
		bytes[index++] = b[7];
	}
	
	private void append_n(byte[] b) {
		int i = 0;
		while(i != b.length) {
			bytes[index++] = b[i++];
		}
	}
	
	public void append(byte[] b) {
		if(b.length % 2 == 0) {
			if(b.length < 5) {
				if(b.length == 2) {
					append_2(b);
					return;
				}
				else if(b.length == 4) {
					append_4(b);
					return;
				}
			}
			else {
				if(b.length == 6) {
					append_6(b);
					return;
				}
				else if(b.length == 8) {
					append_8(b);
					return;
				}
			}
		}
		append_n(b);
	}
	
	public byte[] getBytes() {
		return bytes;
	}
}
