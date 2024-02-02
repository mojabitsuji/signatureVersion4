package jp.tokyo.lascaux.sv4.util;

import java.net.URI;
import java.net.URL;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.StringUtils;

import jp.tokyo.lascaux.sv4.SignatureV4Params.KeyValuesMap;
import jp.tokyo.lascaux.sv4.entity.ParameterType;

/**
 * Signature Version4 で使用するユーティリティを集めたクラスです。
 *
 * @author Shunichi Todoroki
 */
public class SignatureV4Util {
	public static final String LINE_SEPARATOR = "\n";

	private SignatureV4Util() {
	}

	/**
	 * 引数のURIから正規化されたURIを返します。
	 *
	 * @param originalUri 変換対象のURI
	 * @return 正規化されたURI
	 */
	public static String normalizeUri(String originalUri) {
		if (StringUtils.isEmpty(originalUri)) {
			return "/";
		}
		try {
			//仮のドメインを使用して変換だけ行う
			URL url = new URI(String.format("http://localhost.com%s", originalUri)).normalize()
					.toURL();
			return url.getPath();
		} catch (Exception e) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * <pre>
	 * 引数のキーバリューマップの、キーと値をエンコードします。
	 * 引数のオブジェクトの内容は変更しません、新しくマップオブジェクトを生成して返します
	 * エンコード方式は以下を参照（半角スペースは%20）
	 * {@see http://docs.aws.amazon.com/ja_jp/general/latest/gr/sigv4-create-canonical-request.html}
	 * </pre>
	 *
	 * @param queryMap クエリーの内容を保持するマップ
	 * @param parameterType パラメータタイプ
	 * @return キーと値がエンコードされた新しいマップオブジェクト
	 * @return 正規化されたクエリー文字列
	 */
	public static String createCanonicalQueries(KeyValuesMap queryMap, ParameterType parameterType) {
		if (queryMap == null) {
			return null;
		}
		BitSet bitset = new BitSet();
		bitset.set(0x30, 0x3a);
		bitset.set(0x41, 0x5b);
		bitset.set(0x61, 0x7b);
		bitset.set(0x2d, 0x2f);
		bitset.set(0x5f, 0x60);
		bitset.set(0x7e, 0x7f);
		bitset.set(0x20, 0x21);
		String lowerSignatureName = parameterType.getLowerSignatureParamName();
		boolean b = false;
		StringBuilder esb = new StringBuilder();
		for (String key : queryMap.sortedOriginalKeyset()) {
			if (lowerSignatureName.equals(key.toLowerCase())) {
				continue;
			}
			List<String> values = queryMap.getOriginalValues(key);
			if (values != null) {
				Collections.sort(values);
				for (String value : values) {
					//Bitsetを指定しても半角スペースは強引に+に変換されてしまうので、ここで元に戻す。というかなんでそんな実装なんだバカなのか。Bitset指定した意味がないじゃないか。
					String encodedKey = new String(URLCodec.encodeUrl(bitset, key.getBytes()));
					encodedKey = encodedKey.replaceAll("\\+", "%20");
					String encodedValue = new String(URLCodec.encodeUrl(bitset, value.getBytes()));
					encodedValue = encodedValue.replaceAll("\\+", "%20");
					esb.append(encodedKey).append("=").append(encodedValue).append("&");
					b = true;
				}
			}
		}
		return b ? esb.substring(0, esb.length() - 1) : null;
	}

	/**
	 * ヘッダーの値を正規化されたヘッダーを構築するためのトリムされた値に変更します。
	 *
	 * @param headerValue ヘッダーの値
	 * @return トリムされた値
	 */
	public static String trimHeaderValue(String headerValue) {
		//トリムは以下の仕様に沿って行われる
		//値の両端の空白は取り除かれる
		//値の中に含まれる空白は、連続したものを１つの空白に変換する
		//しかし、引用符に囲まれている場合は連続した空白を含んでいてもトリムの対象にはしない
		if (StringUtils.isEmpty(headerValue)) {
			return headerValue;
		}
		headerValue = headerValue.trim();
		if (!(headerValue.startsWith("\"") || headerValue.startsWith("\'")
				|| headerValue.endsWith("\"") || headerValue.endsWith("\'"))) {
			headerValue = headerValue.replaceAll(" +", " ");
		}
		return headerValue;
	}

	//アルゴリズム抽出
	//CanonicalRequest作成
	//クエリの正規化
	//ヘッダーの正規化
	//CanonicalRequestのハッシュ化
	//HexEncode(小文字で返す)
}
