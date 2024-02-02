package jp.tokyo.lascaux.sv4;

import jp.tokyo.lascaux.sv4.entity.ErrorType;

/**
 * Signature Version4 検証エラーを表す例外クラスです。
 *
 * @author Shunichi Todoroki
 */
public class SignatureV4Exception extends Exception {
	private static final long serialVersionUID = -1873512495803142025L;
	private ErrorType errorType;

	public SignatureV4Exception(ErrorType errorType, String message) {
		super(message);
		if (errorType == null) {
			throw new NullPointerException();
		}
		this.errorType = errorType;
	}

	public SignatureV4Exception(ErrorType errorType, String message, Throwable cause) {
		super(message, cause);
		if (errorType == null) {
			throw new NullPointerException();
		}
		this.errorType = errorType;
	}

	public ErrorType getErrorType() {
		return errorType;
	}
}
