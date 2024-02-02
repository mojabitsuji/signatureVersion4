package jp.tokyo.lascaux.sv4;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import jp.tokyo.lascaux.sv4.SignatureV4Params.Credential;
import jp.tokyo.lascaux.sv4.SignatureV4Params.KeyValuesMap;
import jp.tokyo.lascaux.sv4.entity.ErrorType;
import jp.tokyo.lascaux.sv4.entity.ParameterType;
import jp.tokyo.lascaux.sv4.entity.SignatureType;

/**
 * HttpServletRequestをSignatureV4のパラメータ形式に変換するクラスです。
 *
 * @author Shunichi Todoroki
 */
public class SignatureV4RequestParser {
	static final Pattern HEADER_AUTHORIZATION_PATTERN = Pattern
			.compile("\\s*(\\S+)\\s+(C|c)redential=([^,\\s]+)\\s*,\\s*(S|s)ignedHeaders=([^,\\s]+)\\s*,\\s*(S|s)ignature=([^,\\s]+)\\s*");

	public SignatureV4RequestParser() {
	}

	/**
	 * <pre>
	 * 最低限SignatureV4オブジェクトにするのに必要な解析を行い、SV4パラメータオブジェクトを作成して返します。
	 * パラメータを扱いやすい形にパースするのが目的で、パラメータの検証などは行いません
	 * 不正なパラメータによりパースできない場合はSignatureV4Exceptionをスローします
	 * </pre>
	 *
	 * @param request Httpサーブレットリクエスト
	 * @return SignatureV4パラメータオブジェクト
	 * @throws SignatureV4Exception
	 */
	public SignatureV4Params parse(SignatureType signatureType, HttpServletRequest request)
			throws SignatureV4Exception {
		try {
			String payload = IOUtils.toString(request.getInputStream());
			return createSignatureV4Params(signatureType, request.getMethod(),
					request.getRequestURI(), createHeaders(new WrappedRequest(request)),
					request.getParameterMap(), payload);
		} catch (IOException ioe) {
			throw new SignatureV4Exception(ErrorType.OTHER, "リクエストボディの読み込みに失敗しました。", ioe);
		}
	}

	/*
	 * テスト用のメソッドのためパッケージプライベート。
	 */
	SignatureV4Params createSignatureV4Params(SignatureType signatureType, String method,
			String requestUri, Headers headers, String queryString, String payload)
			throws SignatureV4Exception {
		Map<String, String[]> queryMap = null;
		if (!StringUtils.isEmpty(queryString)) {
			queryMap = new HashMap<>();
			String[] queries = queryString.split("&");
			for (String query : queries) {
				int index = query.indexOf("=");
				if (index >= 0) {
					String key = query.substring(0, index);
					String value = query.substring(index + 1);
					String[] vs = queryMap.get(key);
					if (vs == null) {
						queryMap.put(key, new String[] { value });
					} else {
						List<String> l = new ArrayList<>(Arrays.asList(vs));
						l.add(value);
						queryMap.put(key, l.toArray(new String[vs.length + 1]));
					}
				}
			}
		}
		return createSignatureV4Params(signatureType, method, requestUri, createHeaders(headers),
				queryMap, payload);
	}

	private SignatureV4Params createSignatureV4Params(SignatureType signatureType, String method,
			String requestUri, KeyValuesMap headers, Map<String, String[]> queryMap, String payload)
			throws SignatureV4Exception {
		ParameterType parameterType = signatureType.getParameterType();
		SV4ParameterImpl ps = new SV4ParameterImpl();
		ps.method = method;
		ps.requestUri = requestUri;
		ps.headers = headers;
		ps.queries = createQueries(queryMap);
		String authorizationString = headers.getSingleByLowerKey("authorization");
		if (!StringUtils.isEmpty(authorizationString)) {
			Matcher m = HEADER_AUTHORIZATION_PATTERN.matcher(authorizationString);
			if (m.matches()) {
				ps.algorithm = m.group(1);
				String credentialString = m.group(3);
				Credential credential = createCredential(credentialString);
				ps.credential = credential;
				String signedHeaders = m.group(5);
				if (!StringUtils.isEmpty(signedHeaders)) {
					ps.lowerSignedHeaders = new TreeSet<>(Arrays.asList(signedHeaders.toLowerCase()
							.split(";")));
				}
				ps.signature = m.group(7);
			}
		} else {
			ps.algorithm = ps.queries.getSingleByLowerKey(parameterType
					.getLowerAlgorithmParamName());
			String credentialString = ps.queries.getSingleByLowerKey(parameterType
					.getLowerCredentialParamName());
			Credential credential = createCredential(credentialString);
			ps.credential = credential;
			String signedHeaders = ps.queries.getSingleByLowerKey(parameterType
					.getLowerSignedHeadersParamName());
			if (!StringUtils.isEmpty(signedHeaders)) {
				ps.lowerSignedHeaders = new TreeSet<>(Arrays.asList(signedHeaders.toLowerCase()
						.split(";")));
			}
			ps.signature = ps.queries.getSingleByLowerKey(parameterType
					.getLowerSignatureParamName());
		}
		ps.payload = payload;
		return ps;
	}

	private static class WrappedRequest implements Headers {
		private HttpServletRequest request;

		private WrappedRequest(HttpServletRequest request) {
			this.request = request;
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return request.getHeaderNames();
		}

		@Override
		public String getHeader(String key) {
			return request.getHeader(key);
		}

		@Override
		public Enumeration<String> getHeaders(String key) {
			return request.getHeaders(key);
		}
	}

	static interface Headers {
		Enumeration<String> getHeaderNames();

		String getHeader(String key);

		Enumeration<String> getHeaders(String key);
	}

	private KeyValuesMap createHeaders(Headers headers) {
		KeyValuesMap keyValuesMap = new KeyValuesMap();
		Enumeration<String> e = headers.getHeaderNames();
		for (; e.hasMoreElements();) {
			String originalKey = e.nextElement();
			String trimKey = originalKey.trim();
			String value = headers.getHeader(originalKey);
			if (value != null) {
				keyValuesMap.put(trimKey, value.trim());
			} else {
				Enumeration<String> hs = headers.getHeaders(originalKey);
				if (hs != null) {
					List<String> l = new ArrayList<>();
					for (; hs.hasMoreElements();) {
						l.add(hs.nextElement().trim());
					}
					keyValuesMap.put(trimKey, l);
				}
			}
		}
		return keyValuesMap;
	}

	private KeyValuesMap createQueries(Map<String, String[]> queryMap) {
		KeyValuesMap keyValuesMap = new KeyValuesMap();
		if (queryMap != null) {
			for (Map.Entry<String, String[]> e : queryMap.entrySet()) {
				String key = e.getKey();
				String[] values = e.getValue();
				for (String value : values) {
					keyValuesMap.put(key, value);
				}
			}
		}
		return keyValuesMap;
	}

	private Credential createCredential(String credentialString) throws SignatureV4Exception {
		if (StringUtils.isEmpty(credentialString)) {
			return null;
		}
		String[] credentials = credentialString.split("/");
		if (credentials.length != 5) {
			throw new SignatureV4Exception(ErrorType.INVALID_PARAMS,
					"credentialは/で区切られた5つの値である必要があります。 [" + credentialString + "]");
		}
		return new Credential(credentials[0], credentials[1], credentials[2], credentials[3],
				credentials[4]);
	}

	private static class SV4ParameterImpl implements SignatureV4Params {
		static final SortedSet<String> EMPTY_SET = new TreeSet<>();
		private String method;
		private String requestUri;
		private KeyValuesMap headers;
		private KeyValuesMap queries;
		private String payload;
		private String algorithm;
		private Credential credential;
		private SortedSet<String> lowerSignedHeaders;
		private String signature;
		private String cachedDateString;

		private SV4ParameterImpl() {
		}

		@Override
		public String getMethod() {
			return method;
		}

		@Override
		public String getRequestUri() {
			return requestUri;
		}

		@Override
		public KeyValuesMap getHeaders() {
			return headers;
		}

		@Override
		public KeyValuesMap getQueries() {
			return queries;
		}

		@Override
		public String getPayload() {
			return payload;
		}

		@Override
		public String getAlgorithm() {
			return algorithm;
		}

		@Override
		public Credential getCredential() {
			return credential;
		}

		@Override
		public String getCredentialScopes() {
			return String.format("%s/%s/%s/%s", credential.getRequestDate(),
					credential.getRegion(), credential.getRequestService(),
					credential.getConstString());
		}

		@Override
		public String getSignature() {
			return signature;
		}

		@Override
		public String getDate(ParameterType parameterType) throws SignatureV4Exception {
			if (cachedDateString != null) {
				return cachedDateString;
			}
			DateFormat iso8601f = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
			iso8601f.setTimeZone(TimeZone.getTimeZone("GMT"));
			String lowerDateParamName = parameterType.getLowerDateParamName();
			String date = headers.getSingleByLowerKey(lowerDateParamName);
			if (date != null) {
				try {
					iso8601f.parse(date);
				} catch (ParseException e) {
					throw new SignatureV4Exception(ErrorType.INVALID_DATE_FORMAT, String.format(
							"\"%s\"ヘッダーのフォーマットが間違っています。 [%s]", lowerDateParamName, date), e);
				}
				cachedDateString = date;
				return date;
			}
			date = queries.getSingleByLowerKey(lowerDateParamName);
			if (date != null) {
				try {
					iso8601f.parse(date);
				} catch (ParseException e) {
					throw new SignatureV4Exception(ErrorType.INVALID_DATE_FORMAT, String.format(
							"\"%s\"クエリーのフォーマットが間違っています。 [%s]", lowerDateParamName, date), e);
				}
				cachedDateString = date;
				return date;
			}
			String dateString = headers.getSingleByLowerKey("date");
			if (StringUtils.isEmpty(dateString)) {
				throw new SignatureV4Exception(ErrorType.INVALID_PARAMS, String.format(
						"\"date\"あるいは\"%s\"パラメータが最低でも１つは必要です。", lowerDateParamName));
			}
			DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
			Date d;
			try {
				d = df.parse(dateString);
			} catch (ParseException e) {
				throw new SignatureV4Exception(ErrorType.INVALID_DATE_FORMAT, String.format(
						"\"date\"ヘッダーのフォーマットが間違っています。 [%s]", dateString), e);
			}
			cachedDateString = iso8601f.format(d);
			return cachedDateString;
		}

		@Override
		public String getExpire(ParameterType parameterType) {
			String lowerExpiresParamName = parameterType.getLowerExpiresParamName();
			String expire = headers.getSingleByLowerKey(lowerExpiresParamName);
			if (expire != null) {
				return expire;
			}
			return queries.getSingleByLowerKey(lowerExpiresParamName);
		}

		/**
		 * Nullは返りません。
		 */
		@Override
		public SortedSet<String> getLowerSignedHeaders(ParameterType parameterType) {
			if (lowerSignedHeaders != null) {
				return lowerSignedHeaders;
			}
			String lowerSignedHeaderParamName = parameterType.getLowerSignedHeadersParamName();
			String signedHeadersString = queries.getSingleByLowerKey(lowerSignedHeaderParamName);
			return StringUtils.isEmpty(signedHeadersString) ? EMPTY_SET : new TreeSet<>(
					Arrays.asList(signedHeadersString.toLowerCase().split(";")));
		}
	}
}
