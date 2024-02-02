package jp.tokyo.lascaux.sv4.entity;

import jp.tokyo.lascaux.sv4.SignatureV4Exception;

/**
 * SHAアルゴリズムを表す列挙型。
 *
 * @author Shunichi Todoroki
 */
public enum HashType {
	/** SHA1 */
	SHA1("SHA1", "SHA-1", "HmacSHA1"),
	/** SHA256 */
	SHA256("SHA256", "SHA-256", "HmacSHA256");
	private String algorithmName;
	private String shaAlgorithmName;
	private String hmacAlgorithmName;

	private HashType(String algorithmName, String shaAlgorithmName, String hmacAlgorithmName) {
		this.algorithmName = algorithmName;
		this.shaAlgorithmName = shaAlgorithmName;
		this.hmacAlgorithmName = hmacAlgorithmName;
	}

	public String getAlgorithmName() {
		return algorithmName;
	}

	public String getShaAlgorithmName() {
		return shaAlgorithmName;
	}

	public String getHmacAlgorithmName() {
		return hmacAlgorithmName;
	}

	/**
	 * 引数のSignatureTypeとシステムで使用するアルゴリズム名から
	 * 検証に使用するハッシュアルゴリズムのタイプを決定して返します。
	 *
	 * @param signatureType シグネチャータイプ
	 * @param systemAlgorithmName システムアルゴリズム名
	 * @return ハッシュアルゴリズムタイプ
	 * @throws SignatureV4Exception 使用可能なアルゴリズム名が見つからない時にスローされる
	 */
	public static HashType getHashType(SignatureType signatureType, String systemAlgorithmName)
			throws SignatureV4Exception {
		String prefix = signatureType.getPrefix();
		for (HashType h : values()) {
			String name = String.format("%s-HMAC-%s", prefix, h.getAlgorithmName());
			if (name.equals(systemAlgorithmName)) {
				return h;
			}
		}
		throw new SignatureV4Exception(ErrorType.INVALID_ALGORITHM_NAME, String.format(
				"アルゴリズム名に間違いがあります。[%s]", systemAlgorithmName));
	}
}
