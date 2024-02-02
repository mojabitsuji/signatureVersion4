package jp.tokyo.lascaux.sv4.entity;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

/**
 * SHAアルゴリズムのハッシュメソッドを提供するクラスです。
 *
 * @author Shunichi Todoroki
 */
public class SHAAlgorithm implements Hash {
	private HashType type;

	public SHAAlgorithm(HashType type) {
		this.type = type;
	}

	@Override
	public String hashedString(String value) {
		if (value == null) {
			throw new RuntimeException("valueはnullではなく空文字に変換してからこのメソッドを使用してください。");
		}
		final String shaAlgorithmName = type.getShaAlgorithmName();
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance(shaAlgorithmName);
		} catch (NoSuchAlgorithmException e) {
			//HashTypeで定義された名前のものしか使用しないので無視
			throw new RuntimeException(e);
		}
		md.update(value.getBytes());
		byte[] valueArray = md.digest();
		return new String(Hex.encodeHex(valueArray));
	}

	@Override
	public byte[] hashHmac(String data, byte[] key) throws NoSuchAlgorithmException,
			InvalidKeyException {
		final String hmacAlgorithmName = type.getHmacAlgorithmName();
		Mac mac = Mac.getInstance(hmacAlgorithmName);
		mac.init(new SecretKeySpec(key, hmacAlgorithmName));
		byte[] bytes = null;
		try {
			bytes = mac.doFinal(data.getBytes("UTF8"));
		} catch (IllegalStateException e) {
			//initializeはしてあるので起こりえない
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			//UTF-8がないはずはない
			throw new RuntimeException(e);
		}
		return bytes;
	}
}
