package jp.tokyo.lascaux.sv4.entity;

/**
 * リクエストパラメータ名のタイプを表す列挙型です。
 *
 * @author Shunichi Todoroki
 */
public enum ParameterType {
	/** Amazon（テスト用） */
	AMAZON("Amz"),
	/** Lascaux */
	LASCAUX("Lascaux");
	private String typeName;

	private ParameterType(String typeName) {
		this.typeName = typeName;
	}

	public String getAlgorithmParamName() {
		return String.format("X-%s-Algorithm", typeName);
	}

	public String getLowerAlgorithmParamName() {
		return getAlgorithmParamName().toLowerCase();
	}

	public String getCredentialParamName() {
		return String.format("X-%s-Credential", typeName);
	}

	public String getLowerCredentialParamName() {
		return getCredentialParamName().toLowerCase();
	}

	public String getSignedHeadersParamName() {
		return String.format("X-%s-SignedHeaders", typeName);
	}

	public String getLowerSignedHeadersParamName() {
		return getSignedHeadersParamName().toLowerCase();
	}

	public String getSignatureParamName() {
		return String.format("X-%s-Signature", typeName);
	}

	public String getLowerSignatureParamName() {
		return getSignatureParamName().toLowerCase();
	}

	public String getDateParamName() {
		return String.format("X-%s-Date", typeName);
	}

	public String getLowerDateParamName() {
		return getDateParamName().toLowerCase();
	}

	public String getExpiresParamName() {
		return String.format("X-%s-Expires", typeName);
	}

	public String getLowerExpiresParamName() {
		return getExpiresParamName().toLowerCase();
	}
}
