package sapphire.policy.interfaces.dht;

import java.io.Serializable;

import sapphire.policy.dht.DHTHash;

public class DHTKey implements Comparable, Serializable {

	String identifier;
	byte[] key;

	public DHTKey(byte[] key) {
		this.key = key;
	}

	public DHTKey(String identifier) {
		this.identifier = identifier;
		this.key = DHTHash.hash(identifier);
	}

	public boolean isBetween(DHTKey fromKey, DHTKey toKey) {
		if (fromKey.compareTo(toKey) < 0) {
			if (this.compareTo(fromKey) > 0 && this.compareTo(toKey) < 0) {
				return true;
			}
		} else if (fromKey.compareTo(toKey) > 0) {
			if (this.compareTo(toKey) < 0 || this.compareTo(fromKey) > 0) {
				return true;
			}
		}
		return false;
	}

	public DHTKey createStartKey(int index) {
		byte[] newKey = new byte[key.length];
		System.arraycopy(key, 0, newKey, 0, key.length);
		int carry = 0;
		for (int i = (DHTHash.KEY_LENGTH - 1) / 8; i >= 0; i--) {
			int value = key[i] & 0xff;
			value += (1 << (index % 8)) + carry;
			newKey[i] = (byte) value;
			if (value <= 0xff) {
				break;
			}
			carry = (value >> 8) & 0xff;
		}
		return new DHTKey(newKey);
	}

	@Override
	public boolean equals(Object obj){
		DHTKey another = (DHTKey) obj;

		if (obj == null)
			return false;

		return another.getIdentifier().equals(identifier);
	}

	@Override
	public int hashCode(){
		return identifier.hashCode();
	}

	@Override
	public int compareTo(Object obj) {
		DHTKey targetKey = (DHTKey) obj;
		for (int i = 0; i < key.length; i++) {
			int loperand = (this.key[i] & 0xff);
			int roperand = (targetKey.getKey()[i] & 0xff);
			if (loperand != roperand) {
				return (loperand - roperand);
			}
		}
		return 0;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (key.length > 4) {
			for (int i = 0; i < key.length; i++) {
				sb.append(Integer.toString(((int) key[i]) & 0xff) + ".");
			}
		} else {
			long n = 0;
			for (int i = key.length-1,j=0; i >= 0 ; i--, j++) {
				n |= ((key[i]<<(8*j)) & (0xffL<<(8*j)));
			}
			sb.append(Long.toString(n));
		}
		return sb.substring(0, sb.length() - 1).toString();
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public byte[] getKey() {
		return key;
	}

	public void setKey(byte[] key) {
		this.key = key;
	}
}

