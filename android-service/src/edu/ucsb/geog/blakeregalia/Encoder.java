package edu.ucsb.geog.blakeregalia;

public class Encoder {


	//											        v6
	public final static long DATA_HEADER_META_LENGTH  = 3;
	public final static long DATA_HEADER_GPS_LENGTH   = 11;
	public final static long DATA_HEADER_LENGTH       = DATA_HEADER_META_LENGTH + DATA_HEADER_GPS_LENGTH;
	public final static long DATA_ENTRY_LENGTH        = 10;
	public final static long TERMINATING_FIELD_LENGTH = 1;
	public final static long STRING_MAPPING_LENGTH    = 2;

	/** encodes 1-byte of data */
	public static byte encode_byte(int u) {
		return (byte) (u & 0xff);
	}

	/** convert from an unsigned 16-bit char to a 2-byte array */
	public static byte[] encode_char (char u) {
		byte[] array = new byte[2];

		array[0] = (byte) ((u >> 8) & 0xFF);
		array[1] = (byte) (u & 0xFF);

		return array;
	}

	/** convert from a signed 16-bit short to a 2-byte array */
	public static byte[] encode_short (short u) {
		byte[] array = new byte[2];

		array[0] = (byte) ((u >> 8) & 0xFF);
		array[1] = (byte) (u & 0xFF);

		return array;
	}

	/** convert from a 32-bit int to a 4-byte array */
	public static byte[] encode_int (int u) {
		byte[] array = new byte[4];

		array[0] = (byte) ((u >> 24) & 0xFF);
		array[1] = (byte) ((u >> 16) & 0xFF);
		array[2] = (byte) ((u >> 8) & 0xFF);
		array[3] = (byte) (u & 0xFF);

		return array;
	}

	/** convert from an unsigned 64-bit long int to a 8-byte array */
	public static byte[] encode_long(long u) {
		byte[] array = new byte[8];

		array[0] = (byte) ((u >> 56) & 0xFF);
		array[1] = (byte) ((u >> 48) & 0xFF);
		array[2] = (byte) ((u >> 40) & 0xFF);
		array[3] = (byte) ((u >> 32) & 0xFF);
		array[4] = (byte) ((u >> 24) & 0xFF);
		array[5] = (byte) ((u >> 16) & 0xFF);
		array[6] = (byte) ((u >> 8) & 0xFF);
		array[7] = (byte) (u & 0xFF);

		return array;
	}
	
	public static byte[] encode_double_to_4_bytes(double u, int p) {
		int ui = (int) (u * p);
		System.out.println("edt4b: "+ui);
		return encode_int(ui & 0xffffffff);
	}
	
	public static String bytes_to_string(byte[] b) {
		String ret = new String();
		for(int i=0; i<b.length; i++) {
			int hb = b[i] & 0xff;
			ret += Integer.toHexString(hb)+'.';
		}
		return ret;
	}

}
