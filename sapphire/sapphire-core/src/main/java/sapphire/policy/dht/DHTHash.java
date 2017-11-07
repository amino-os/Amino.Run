package sapphire.policy.dht;

import java.util.zip.CRC32;

public class DHTHash {

	public static String function = "CRC32";
	public static int KEY_LENGTH = 32;

	public static byte[] hash(String identifier) {
		if (function.equals("CRC32")) {
			CRC32 crc32 = new CRC32();
			crc32.reset();
			crc32.update(identifier.getBytes());
			long code = crc32.getValue();
			code &= (0xffffffffffffffffL >>> (64 - KEY_LENGTH));
			byte[] value = new byte[KEY_LENGTH / 8];
			for (int i = 0; i < value.length; i++) {
				value[value.length - i - 1] = (byte) ((code >> 8 * i) & 0xff);
			}
			return value;
		}
		if (function.equals("Java")) {
			int code = identifier.hashCode();
			code &= (0xffffffff >>> (32 - KEY_LENGTH));
			byte[] value = new byte[KEY_LENGTH / 8];
			for (int i = 0; i < value.length; i++) {
				value[value.length - i - 1] = (byte) ((code >> 8 * i) & 0xff);
			}
			return value;
		}
		return null;
	}

	public static String getFunction() {
		return function;
	}

	public static void setFunction(String function) {
		if (function.equals("CRC32")) {
			DHTHash.KEY_LENGTH = 64;
		}
		if (function.equals("Java")) {
			DHTHash.KEY_LENGTH = 32;
		}
		DHTHash.function = function;
	}

	public static int getKeyLength() {
		return KEY_LENGTH;
	}

	public static void setKeyLength(int keyLength) {
		KEY_LENGTH = keyLength;
	}
}

