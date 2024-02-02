package jp.tokyo.lascaux.sv4.entity;

/**
 * <pre>
 * 例外に付与されるエラーのタイプです。
 * 例外にはメッセージも含まれますが、プログラムでエラーのタイプを判定する場合などに使用してください
 * テストなどでも必要になると思います
 * </pre>
 *
 * @author Shunichi Todoroki
 */
public enum ErrorType {
	/** 不正なパラメータ */
	INVALID_PARAMS,
	/** サポートしていないアルゴリズム */
	INVALID_ALGORITHM_NAME,
	/** 日付フォーマット間違い */
	INVALID_DATE_FORMAT,
	/** ハッシュ化に失敗 */
	FAIL_HASHED,
	/** 要求日付の不整合 */
	WRONG_DATE,
	/** 期限切れ */
	EXPIRE,
	/** シグネチャーが間違っている */
	WRONG_SIGNATURE,
	/** それ以外 */
	OTHER;
}
