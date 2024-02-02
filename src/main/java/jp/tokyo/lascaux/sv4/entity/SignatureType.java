package jp.tokyo.lascaux.sv4.entity;

import com.typesafe.config.Config;

/**
 * Signatureの種類を表す列挙型です。
 *
 * @author Shunichi Todoroki
 */
public enum SignatureType {
	/** Amazon（サンプルテスト用） */
	AMAZON(ParameterType.AMAZON, "AWS4") {
		@Override
		public String getCredentialRegion(Config config) {
			return "us-east-1";
		}

		@Override
		public String getCredentialService(Config config) {
			return "iam";
		}

		@Override
		public String getCredentialEndString() {
			return "aws4_request";
		}
	},
	/** Amazon（テストスイート用、amazonが用意してくれているテストケース） */
	AMAZON_TEST_SUITE(ParameterType.AMAZON, "AWS4") {
		@Override
		public String getCredentialRegion(Config config) {
			return "us-east-1";
		}

		@Override
		public String getCredentialService(Config config) {
			return "host";
		}

		@Override
		public String getCredentialEndString() {
			return "aws4_request";
		}
	},
	/** Lascaux */
	LASCAUX(ParameterType.LASCAUX, "LSCX") {
		@Override
		public String getCredentialRegion(Config config) {
			return config.getString("credential.region");
		}

		@Override
		public String getCredentialService(Config config) {
			return config.getString("credential.service");
		}

		@Override
		public String getCredentialEndString() {
			return "apis_request";
		}
	};
	private ParameterType parameterType;
	private String prefix;

	private SignatureType(ParameterType parameterType, String prefix) {
		this.parameterType = parameterType;
		this.prefix = prefix;
	}

	public ParameterType getParameterType() {
		return parameterType;
	}

	public String getPrefix() {
		return prefix;
	}

	public abstract String getCredentialRegion(Config config);

	public abstract String getCredentialService(Config config);

	public abstract String getCredentialEndString();
}
