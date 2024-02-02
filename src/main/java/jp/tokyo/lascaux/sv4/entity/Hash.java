package jp.tokyo.lascaux.sv4.entity;

public interface Hash {
	public String hashedString(String value) throws Exception;

	public byte[] hashHmac(String data, byte[] key) throws Exception;
}
