package jp.tokyo.lascaux.sv4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import jp.tokyo.lascaux.sv4.entity.ParameterType;

/**
 * <pre>
 * Signature Version4 検証で使用するパラメータの集合クラスです。
 * 変換などはしていないオリジナルのままの値を返します
 * </pre>
 *
 * @author Shunichi Todoroki
 */
public interface SignatureV4Params {
	public String getMethod();

	public String getRequestUri();

	public KeyValuesMap getHeaders();

	public KeyValuesMap getQueries();

	public String getPayload();

	//以下はAuthorizationを分解したもの
	public String getAlgorithm();

	public Credential getCredential();

	/**
	 * <pre>
	 * CredentialScope文字列を作成して返します。
	 * CredentialScope文字列とは、以下の文字を"/"スラッシュで連結したものです
	 * ・Credential.requestDate
	 * ・Credential.region
	 * ・Credential.requestService
	 * ・Credential.constString
	 * </pre>
	 *
	 * @return
	 */
	public String getCredentialScopes();

	public String getSignature();

	/**
	 * <pre>
	 * 以下の順番で値を確認し値が先にあったものを返します。
	 * ・ヘッダーの"X-Amz-Date"
	 * ・クエリーの"X-Amz-Date"
	 * ・ヘッダーの"date"
	 * "date"ヘッダーの場合は、iso8601形式にフォーマットされて返されます
	 * ※Amz部分はSignatureタイプにより変わる
	 * </pre>
	 *
	 * @param parameterType パラメータタイプ
	 * @return
	 * @throws SignatureV4Exception
	 */
	public String getDate(ParameterType parameterType) throws SignatureV4Exception;

	/**
	 * <pre>
	 * 以下の順番で値を確認し値が先にあったものを返します。
	 * ・ヘッダーの"X-Amz-Expire"
	 * ・クエリーの"X-Amz-Expire"
	 * ※Amz部分はSignatureタイプにより変わる
	 * </pre>
	 *
	 * @param parameterType パラメータタイプ
	 * @return
	 */
	public String getExpire(ParameterType parameterType);

	/**
	 * <pre>
	 * 以下の順番で値を確認し値が先にあったものを返します。
	 * ・ヘッダーの"X-Amz-SignedHeaders"
	 * ・クエリーの"X-Amz-SignedHeaders"
	 * ※Amz部分はSignatureタイプにより変わる
	 * 　すべて小文字に変換され、キー名でソートされたSetで返されます
	 * </pre>
	 *
	 * @param parameterType パラメータタイプ
	 * @return
	 */
	public SortedSet<String> getLowerSignedHeaders(ParameterType parameterType);

	//以下はCredentialを分解したもの
	public static class Credential {
		private String accessKey;
		private String requestDate;
		private String region;
		private String requestService;
		private String constString;

		public Credential(String accessKey, String requestDate, String region,
				String requestService, String constString) {
			this.accessKey = accessKey;
			this.requestDate = requestDate;
			this.region = region;
			this.requestService = requestService;
			this.constString = constString;
		}

		public String getAccessKey() {
			return accessKey;
		}

		public String getRequestDate() {
			return requestDate;
		}

		public String getRegion() {
			return region;
		}

		public String getRequestService() {
			return requestService;
		}

		public String getConstString() {
			return constString;
		}
	}

	public static class KeyValuesMap {
		private Map<String, List<String>> originalMap;
		private Map<String, List<String>> lowerKeyMap;

		KeyValuesMap() {
			originalMap = new TreeMap<>();
			lowerKeyMap = new TreeMap<>();
		}

		public void put(String key, String value) {
			List<String> olist = originalMap.get(key);
			if (olist == null) {
				olist = new ArrayList<>();
				originalMap.put(key, olist);
			}
			olist.add(value);
			String lowerKey = key.toLowerCase();
			List<String> llist = lowerKeyMap.get(lowerKey);
			if (llist == null) {
				llist = new ArrayList<>();
				lowerKeyMap.put(lowerKey, llist);
			}
			llist.add(value);
		}

		public void put(String key, List<String> ls) {
			List<String> ovalues = originalMap.get(key);
			List<String> lvalues = lowerKeyMap.get(key);
			if (ovalues == null) {
				ovalues = new ArrayList<>();
				originalMap.put(key, ovalues);
			}
			if (lvalues == null) {
				lvalues = new ArrayList<>();
				lowerKeyMap.put(key.toLowerCase(), lvalues);
			}
			for (String v : ls) {
				ovalues.add(v);
				lvalues.add(v);
			}
		}

		public String getSingle(String key) {
			List<String> values = originalMap.get(key);
			if (values == null || values.isEmpty()) {
				return null;
			}
			return values.get(0);
		}

		public String getSingleByLowerKey(String key) {
			List<String> values = lowerKeyMap.get(key);
			if (values == null || values.isEmpty()) {
				return null;
			}
			return values.get(0);
		}

		public Map<String, List<String>> getOriginalMap() {
			return originalMap;
		}

		public Map<String, List<String>> getLowerKeyMap() {
			return lowerKeyMap;
		}

		public List<String> getOriginalValues(String originalKey) {
			return originalMap.get(originalKey);
		}

		public Map<String, String> getSingleValueOriginalMap() {
			return convertSingleValue(originalMap);
		}

		public Map<String, String> getSingleValueLowerKeyMap() {
			return convertSingleValue(lowerKeyMap);
		}

		private Map<String, String> convertSingleValue(Map<String, List<String>> map) {
			Map<String, String> returnMap = new HashMap<>();
			for (Map.Entry<String, List<String>> e : map.entrySet()) {
				String key = e.getKey();
				List<String> values = map.get(key);
				if (values != null && !values.isEmpty()) {
					returnMap.put(key, values.get(0));
				}
			}
			return returnMap;
		}

		public Set<String> sortedOriginalKeyset() {
			return originalMap.keySet();
		}
	}

	//	public static class QueryMap {
	//		private Map<String, List<String>> originalKeyMap;
	//		private Map<String, List<String>> lowerKeyMap;
	//
	//		QueryMap() {
	//			originalKeyMap = new TreeMap<>();
	//			lowerKeyMap = new TreeMap<>();
	//		}
	//
	//		public void put(String key, String value) {
	//			List<String> olist = originalKeyMap.get(key);
	//			if (olist == null) {
	//				olist = new ArrayList<>();
	//				originalKeyMap.put(key, olist);
	//			}
	//			olist.add(value);
	//			String lowerKey = key.toLowerCase();
	//			List<String> llist = lowerKeyMap.get(lowerKey);
	//			if (llist == null) {
	//				llist = new ArrayList<>();
	//				lowerKeyMap.put(lowerKey, llist);
	//			}
	//			llist.add(value);
	//		}
	//
	//		/**
	//		 * 同じキーで複数見つかった場合は、一番最初の値を返します。
	//		 *
	//		 * @param lowerKey
	//		 * @return
	//		 */
	//		public String getSingleByLowerKey(String lowerKey) {
	//			List<String> values = lowerKeyMap.get(lowerKey);
	//			return values == null || values.isEmpty() ? null : values.get(0);
	//		}
	//
	//		public List<String> keyList() {
	//			return keyList;
	//		}
	//	}
}
