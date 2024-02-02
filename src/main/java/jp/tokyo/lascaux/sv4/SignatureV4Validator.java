package jp.tokyo.lascaux.sv4;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import jp.tokyo.lascaux.sv4.SignatureV4Params.Credential;
import jp.tokyo.lascaux.sv4.SignatureV4Params.KeyValuesMap;
import jp.tokyo.lascaux.sv4.entity.ErrorType;
import jp.tokyo.lascaux.sv4.entity.HashType;
import jp.tokyo.lascaux.sv4.entity.ParameterType;
import jp.tokyo.lascaux.sv4.entity.SHAAlgorithm;
import jp.tokyo.lascaux.sv4.entity.SignatureType;
import jp.tokyo.lascaux.sv4.util.SignatureV4Util;

/**
 * Signature Version4 を検証するクラスです。
 *
 * @author Shunichi Todoroki
 */
public class SignatureV4Validator {
	private static final SignatureV4RequestParser REQUEST_PARSER = new SignatureV4RequestParser();
	private static final Config CONFIG = ConfigFactory.load("application-sv4.conf");

	private SignatureV4Validator() {
	}

	/**
	 * HttpServletRequestを引数にとりSignatureVersion4の検証を行います。
	 *
	 * @param signatureType シグネチャーのタイプ
	 * @param request サーブレットリクエスト
	 * @param privateKey 秘密鍵
	 * @throws SignatureV4Exception 検証に失敗した時にスローされる例外
	 */
	public static void validate(SignatureType signatureType, HttpServletRequest request,
			String privateKey) throws SignatureV4Exception {
		validate(signatureType, REQUEST_PARSER.parse(signatureType, request), privateKey);
	}

	/**
	 * SignatureVersion4パラメータを引数にとりSignatureVersion4の検証を行います。
	 *
	 * @param signatureType シグネチャーのタイプ
	 * @param params SignatureVersion4パラメータ
	 * @param privateKey 秘密鍵
	 * @throws SignatureV4Exception 検証に失敗した時にスローされる例外
	 */
	public static void validate(SignatureType signatureType, SignatureV4Params params,
			String privateKey) throws SignatureV4Exception {
		try {
			//まずパラメータのチェック
			validateParams(signatureType, params);
			if (StringUtils.isEmpty(privateKey)) {
				throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "秘密鍵の指定は必ず必要です。");
			}
			//アルゴリズム名からアルゴリズムコンポーネント抽出
			HashType hashType = HashType.getHashType(signatureType, params.getAlgorithm());
			ParameterType parameterType = signatureType.getParameterType();
			SHAAlgorithm shaAlgorithm = new SHAAlgorithm(hashType);
			//CanonicalRequest作成
			String canonicalRequest = createCanonicalRequest(params, shaAlgorithm, parameterType);
			//CanonicalRequestのハッシュ化
			String hashedCanonicalRequest = shaAlgorithm.hashedString(canonicalRequest);
			//署名文字列作成
			String stringToSign = createStringToSign(parameterType, params, hashedCanonicalRequest);
			//シークレットアクセスキーを取り出す
			byte[] kSecret = (signatureType.getPrefix() + privateKey).getBytes("UTF8");
			//  署名計算
			Credential credential = params.getCredential();
			byte[] kDate = shaAlgorithm.hashHmac(credential.getRequestDate(), kSecret);
			byte[] kRegion = shaAlgorithm.hashHmac(credential.getRegion(), kDate);
			byte[] kService = shaAlgorithm.hashHmac(credential.getRequestService(), kRegion);
			byte[] kSigning = shaAlgorithm.hashHmac(credential.getConstString(), kService);
			byte[] bytes = shaAlgorithm.hashHmac(stringToSign, kSigning);
			bytes = new Hex().encode(bytes);
			String signatureKey = new String(bytes, "UTF-8");
			if (!signatureKey.equals(params.getSignature())) {
				throw new SignatureV4Exception(ErrorType.WRONG_SIGNATURE, String.format(
						"シグネチャーが一致しませんでした。 signed signature [%s], calculated signature [%s]",
						params.getSignature(), signatureKey));
			}
			String expireString = params.getExpire(parameterType);
			if (!StringUtils.isEmpty(expireString)) {
				//ここに来たときには、日付文字列は必ずあるはず
				Calendar cal = Calendar.getInstance();
				Date currentTime = cal.getTime();
				String dateString = params.getDate(parameterType);
				DateFormat iso8601f = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
				Date date;
				try {
					date = iso8601f.parse(dateString);
				} catch (ParseException e) {
					throw new SignatureV4Exception(ErrorType.INVALID_DATE_FORMAT, String.format(
							"日付文字列のパースに失敗しました。 [%s]", dateString));
				}
				cal.setTime(date);
				cal.add(Calendar.SECOND, Integer.valueOf(expireString));
				Date expireTime = cal.getTime();
				if (expireTime.getTime() < currentTime.getTime()) {
					throw new SignatureV4Exception(ErrorType.EXPIRE, String.format(
							"有効期限が切れていました。expireDate [%s], currentDate [%s]", expireTime,
							currentTime));
				}
			}
		} catch (SignatureV4Exception e) {
			throw e;
		} catch (UnsupportedEncodingException e) {
			//UTF-8がないことはない
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			//環境移行などでしか起こりえない、起きた場合はアプリで対応はしない
			throw new RuntimeException(e);
		} catch (InvalidKeyException e) {
			throw new SignatureV4Exception(ErrorType.FAIL_HASHED,
					"SignatureVersion4の検証中ハッシュ化処理で例外が発生しました。", e);
		} catch (Exception e) {
			throw new SignatureV4Exception(ErrorType.OTHER,
					"SignatureVersion4の検証中に予期しない例外が発生しました。", e);
		}
	}

	/**
	 * SignatureVersion4検証で必要なパラメータチェックを行います。
	 *
	 * @param signatureType シグネチャータイプ
	 * @param params SV4パラメータ
	 * @throws SignatureV4Exception SV4例外
	 */
	static void validateParams(SignatureType signatureType, SignatureV4Params params)
			throws SignatureV4Exception {
		ParameterType parameterType = signatureType.getParameterType();
		if (StringUtils.isEmpty(params.getMethod())) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "メソッドは必ず必要です。");
		}
		KeyValuesMap headers = params.getHeaders();
		if (StringUtils.isEmpty(headers.getSingleByLowerKey("host"))) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "ヘッダーにhostは必ず必要です。");
		}
		if (StringUtils.isEmpty(params.getAlgorithm())) {
			throw new SignatureV4Exception(ErrorType.INVALID_ALGORITHM_NAME, "アルゴリズム名が取得できませんでした。");
		}
		Credential credential = params.getCredential();
		if (credential == null) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "資格情報（credential）が取得できませんでした。");
		}
		if (StringUtils.isEmpty(credential.getAccessKey())) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "資格情報から\"アクセスキー\"が取得できませんでした。");
		}
		if (StringUtils.isEmpty(credential.getRequestDate())) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "資格情報から\"要求日付\"が取得できませんでした。");
		}
		if (StringUtils.isEmpty(credential.getRegion())) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "資格情報から\"地域\"が取得できませんでした。");
		}
		if (StringUtils.isEmpty(credential.getRequestService())) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "資格情報から\"要求サービス\"が取得できませんでした。");
		}
		if (StringUtils.isEmpty(credential.getConstString())) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "資格情報から\"終了文字列\"が取得できませんでした。");
		}
		if (!credential.getRegion().equals(signatureType.getCredentialRegion(CONFIG))) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "資格情報の\"地域\"が間違っています。");
		}
		if (!credential.getRequestService().equals(signatureType.getCredentialService(CONFIG))) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "資格情報の\"要求サービス\"が間違っています。");
		}
		if (!credential.getConstString().equals(signatureType.getCredentialEndString())) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "資格情報の\"終了文字列\"が間違っています。");
		}
		SortedSet<String> lowerSignedHeaders = params.getLowerSignedHeaders(parameterType);
		//nullはない
		if (lowerSignedHeaders.isEmpty()) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "署名付きヘッダーが取得できませんでした。");
		}
		if (!lowerSignedHeaders.contains("host")) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "署名付きヘッダーには\"host\"項目が必須です。");
		}
		String dateString = params.getDate(parameterType);
		if (StringUtils.isEmpty(dateString)) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, "署名付きヘッダーには\"日付\"項目が必須です。");
		}
		String xDateKeyName = parameterType.getLowerDateParamName();
		if (!StringUtils.isEmpty(headers.getSingleByLowerKey(xDateKeyName))) {
			if (!lowerSignedHeaders.contains(xDateKeyName)) {
				throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, String.format(
						"%sをヘッダーに含む場合には、署名付きヘッダーに\"%s\"項目が必須です。", xDateKeyName, xDateKeyName));
			}
		} else if (!StringUtils.isEmpty(headers.getSingleByLowerKey("date"))) {
			if (!lowerSignedHeaders.contains("date")) {
				throw new SignatureV4Exception(ErrorType.INVALID_PARAMS,
						"dateをヘッダーに含む場合には、署名付きヘッダーに\"date\"項目が必須です。");
			}
		}
		if (StringUtils.isEmpty(params.getSignature())) {
			throw new SignatureV4Exception(ErrorType.WRONG_SIGNATURE, "シグネチャーが取得できませんでした。");
		}
		//スコープの日付とリクエストの日付が一致しているか
		String requestDate = credential.getRequestDate();
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		try {
			df.parse(requestDate);
		} catch (ParseException e) {
			throw new SignatureV4Exception(ErrorType.INVALID_DATE_FORMAT,
					"資格情報の\"要求日付\"のフォーマットが間違っています。", e);
		}
		if (!dateString.startsWith(requestDate)) {
			throw new SignatureV4Exception(ErrorType.WRONG_DATE,
					"資格情報の\"要求日付\"とヘッダーあるいはクエリーの日付が違っています。");
		}
	}

	/**
	 * <pre>
	 * 正規化されたリクエスト文字列を作成して返します。
	 * テスト用にも使える用のパッケージプライベートメソッド
	 * </pre>
	 *
	 * @param params SV4パラメータ
	 * @param shaAlgorithm SHAアルゴリズム
	 * @param parameterType ヘッダータイプ
	 * @return 正規化されたリクエスト文字列
	 * @throws SignatureV4Exception
	 * @see http://docs.aws.amazon.com/ja_jp/general/latest/gr/sigv4-create-canonical-request.html
	 */
	static String createCanonicalRequest(SignatureV4Params params, SHAAlgorithm shaAlgorithm,
			ParameterType parameterType) throws SignatureV4Exception {
		//  メソッド抽出
		//  URI抽出と正規化
		//  クエリ抽出と正規化
		//  ヘッダーの抽出と正規化
		//  署名付きヘッダーの抽出
		//  ペイロードのハッシュ化
		StringBuilder sb = new StringBuilder();
		sb.append(params.getMethod()).append(SignatureV4Util.LINE_SEPARATOR);
		sb.append(SignatureV4Util.normalizeUri(params.getRequestUri())).append(
				SignatureV4Util.LINE_SEPARATOR);
		String encodedKeySortedQueries = SignatureV4Util.createCanonicalQueries(
				params.getQueries(), parameterType);
		if (!StringUtils.isEmpty(encodedKeySortedQueries)) {
			sb.append(encodedKeySortedQueries);
		}
		sb.append(SignatureV4Util.LINE_SEPARATOR);
		SortedSet<String> lowerSignedHeaders = params.getLowerSignedHeaders(parameterType);
		sb.append(createCanonicalHeaders(params, lowerSignedHeaders)).append(
				SignatureV4Util.LINE_SEPARATOR);
		sb.append(SignatureV4Util.LINE_SEPARATOR);
		boolean notFirst = false;
		for (String signedHeader : lowerSignedHeaders) {
			if (notFirst) {
				sb.append(";");
			}
			sb.append(signedHeader);
			notFirst = true;
		}
		sb.append(SignatureV4Util.LINE_SEPARATOR);
		sb.append(shaAlgorithm.hashedString(params.getPayload() == null ? "" : params.getPayload()));
		return sb.toString();
	}

	static String createCanonicalHeaders(SignatureV4Params params,
			SortedSet<String> lowerSignedHeaders) {
		KeyValuesMap headers = params.getHeaders();
		StringBuilder canonicalHeaders = new StringBuilder();
		for (Map.Entry<String, List<String>> e : headers.getLowerKeyMap().entrySet()) {
			String key = e.getKey();
			if ("authorization".equals(key)) {
				continue;
			}
			if (!lowerSignedHeaders.contains(key)) {
				continue;
			}
			canonicalHeaders.append(key).append(":");
			List<String> values = e.getValue();
			Collections.sort(values);
			boolean b = false;
			for (String s : values) {
				if (b) {
					canonicalHeaders.append(",");
				}
				b = true;
				canonicalHeaders.append(SignatureV4Util.trimHeaderValue(s));
			}
			canonicalHeaders.append(SignatureV4Util.LINE_SEPARATOR);
		}
		return canonicalHeaders.substring(0, canonicalHeaders.length() - 1);
	}

	static String createStringToSign(ParameterType parameterType, SignatureV4Params params,
			String hashedCanonicalRequest) throws SignatureV4Exception {
		StringBuilder br = new StringBuilder();
		br.append(params.getAlgorithm());
		br.append(SignatureV4Util.LINE_SEPARATOR);
		br.append(params.getDate(parameterType));
		br.append(SignatureV4Util.LINE_SEPARATOR);
		br.append(params.getCredentialScopes());
		br.append(SignatureV4Util.LINE_SEPARATOR);
		br.append(hashedCanonicalRequest);
		return br.toString();
	}
}
