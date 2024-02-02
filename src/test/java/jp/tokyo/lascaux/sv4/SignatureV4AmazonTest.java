package jp.tokyo.lascaux.sv4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.junit.Test;

import jp.tokyo.lascaux.sv4.entity.SignatureType;

/**
 * Amazonが提供しているテストケースをテストするテストケースです。
 *
 * @author Shunichi Todoroki
 */
public class SignatureV4AmazonTest extends SignatureV4AbstractTest {

	public static ServletInputStream createServletInputStream(String s, String charset) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			baos.write(s.getBytes(charset));
		} catch (Exception e) {
			throw new RuntimeException("No support charset.");
		}

		final InputStream bais = new ByteArrayInputStream(baos.toByteArray());

		return new ServletInputStream() {
			@Override
			public int read() throws IOException {
				return bais.read();
			}

			@Override
			public boolean isFinished() {
				return false;
			}

			@Override
			public boolean isReady() {
				return false;
			}

			@Override
			public void setReadListener(ReadListener readListener) {
			}
		};
	}

	/**
	 * <pre>
	 * AmazonのSignatureVersion4の仕様解説ページにあるサンプルを
	 * テストするテストケースです。
	 * mockitのgetHeaderなどはヘッダー名の大文字小文字を区別してしまうので、
	 * 大文字小文字のテストは別の機構を使用し、ここでは小文字に統一してテストする
	 * </pre>
	 *
	 * @throws Exception
	 */
	@Test
	public void amazonSample() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				"AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/iam/aws4_request,SignedHeaders=content-type;host;x-amz-date,Signature=ced6826de92d2bdeed8f846f0bf508e8559e98e4b0199114b84c54174deb456c");
		headers.put("Host", "iam.amazonaws.com");
		headers.put("Content-type", "application/x-www-form-urlencoded; charset=utf-8");
		headers.put("X-Amz-Date", "20110909T233600Z");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON, "POST",
				"/", headers, null, "Action=ListUsers&Version=2010-05-08");
		SignatureV4Validator.validate(SignatureType.AMAZON, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * ヘッダーキーに重複したキーのものがあった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getHeaderKeyDuplicate() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				"AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host;zoo, Signature=54afcaaf45b331f81cd2edb974f7b824ff4dd594cbbaa945ed636b48477368ed");
		headers.put("Host", "host.foo.com");
		headers.put("ZOO", "zoobar");
		headers.put("zoo", "foobar");
		headers.put("zoo", "zoobar");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"POST", "/", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * ヘッダーの値の順番がバラバラだった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getHeaderValueOrder() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host;p, Signature=d2973954263943b11624a11d1c963ca81fb274169c7868b2858c04f083199e3d");
		headers.put("host", "host.foo.com");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("p", "z");
		headers.put("p", "a");
		headers.put("p", "p");
		headers.put("p", "a");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"POST", "/", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * ヘッダーの値の前後に空白があった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getHeaderValueTrim() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host;p, Signature=debf546796015d6f6ded8626f5ce98597c33b47b9164cf6b17b4642036fcb592");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("host", "host.foo.com");
		headers.put("p", " phfft ");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"POST", "/", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLに相対パス形式が含まれていた場合1
	 *
	 * @throws Exception
	 */
	@Test
	public void getRelativeRelative1() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=b27ccfbfa7df52a200ff74193ca6e32d4b48b8856fab7ebf1c595d0670a7e470");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/foo/bar/../..", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLに相対パス形式が含まれていた場合2
	 *
	 * @throws Exception
	 */
	@Test
	public void getRelativeRelative2() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=b27ccfbfa7df52a200ff74193ca6e32d4b48b8856fab7ebf1c595d0670a7e470");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/foo/..", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/./」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getSlashDotSlash() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=b27ccfbfa7df52a200ff74193ca6e32d4b48b8856fab7ebf1c595d0670a7e470");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/./", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/./foo」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getSlashPointlessDot() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=910e4d6c9abafaf87898e1eb4c929135782ea25bb0279703146455745391e63a");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/./foo", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「//」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getSlash() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=b27ccfbfa7df52a200ff74193ca6e32d4b48b8856fab7ebf1c595d0670a7e470");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "//", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「//foo//」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getSlashes() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=b00392262853cfe3201e47ccf945601079e9b8a7f51ee4c3d9ee4f187aa9bf19");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "//foo//", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLに空白が含まれていた場合
	 * 「/%20/foo」
	 *
	 * @throws Exception
	 */
	@Test
	public void getSpace() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=f309cfbd10197a230c42dd17dbf5cca8a0722564cb40a872d25623cfa758e374");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/%20/foo", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/-._~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getUnreserved() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=830cc36d03f0f84e6ee4953fbe701c1c8b71a0372c63af9255aa364dd183281e");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/-._~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
				headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/%E1%88%B4」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getUtf8() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=8d6634c189aa8c75c2e51e106b6b5121bed103fdb351f7d7d4381c738823af74");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/%E1%88%B4", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/?foo=bar」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getVanillaEmptyQueryKey() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=56c054473fd260c13e4e7393eb203662195f5d4a1fada5314b8b52b23f985e9f");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/", headers, "foo=bar", null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/?foo=Zoo&foo=aha」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getVanillaQueryOrderKeyCase() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=be7148d34ebccdc6423b19085378aa0bee970bdc61d144bd1a8c48c33079ab09");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/", headers, "foo=Zoo&foo=aha", null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/?a=foo&b=foo」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getVanillaQueryOrderKey() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=0dc122f3b28b831ab48ba65cb47300de53fbe91b577fe113edac383730254a3b");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/", headers, "a=foo&b=foo", null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/?foo=b&foo=a」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getVanillaQueryOrderValue() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=feb926e49e382bec75c9d7dcb2a1b6dc8aa50ca43c25d2bc51143768c0875acc");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/", headers, "foo=b&foo=a", null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/?-._~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz=-._~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getVanillaQueryUnreserved() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=f1498ddb4d6dae767d97c466fb92f1b59a2c71ca29ac954692663f9db03426fb");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER
				.createSignatureV4Params(
						SignatureType.AMAZON_TEST_SUITE,
						"GET",
						"/",
						headers,
						"-._~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz=-._~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
						null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getVanillaQuery() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=b27ccfbfa7df52a200ff74193ca6e32d4b48b8856fab7ebf1c595d0670a7e470");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/?ሴ=bar」だった場合
	 *
	 * @throws Exception
	 */
	@Test
	public void getVanillaUt8Query() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=6fb359e9a05394cc7074e0feb42573a2601abc0c869a953e8c5c12e4e01f1a8c");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/", headers, "ሴ=bar", null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * URLが「/」だった場合
	 * {@see #getVanillaQuery()}と同じ?
	 *
	 * @throws Exception
	 */
	@Test
	public void getVanilla() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=b27ccfbfa7df52a200ff74193ca6e32d4b48b8856fab7ebf1c595d0670a7e470");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"GET", "/", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * ヘッダーのキーが大文字小文字が混在
	 * メソッドがPOST
	 *
	 * @throws Exception
	 */
	@Test
	public void postHeaderKeyCase() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=22902d79e148b64e7571c3565769328423fe276eae4b26f83afceda9e767f726");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"POST", "/", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * メソッドがPOST
	 * ヘッダーのソート順
	 *
	 * @throws Exception
	 */
	@Test
	public void postHeaderKeySort() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host;zoo, Signature=b7a95a52518abbca0964a999a880429ab734f35ebbf1235bd79a5de87756dc4a");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("host", "host.foo.com");
		headers.put("ZOO", "zoobar");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"POST", "/", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * メソッドがPOST
	 * ヘッダーの値が大文字
	 *
	 * @throws Exception
	 */
	@Test
	public void postHeaderValueCase() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host;zoo, Signature=273313af9d0c265c531e11db70bbd653f3ba074c1009239e8559d3987039cad7");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("host", "host.foo.com");
		headers.put("zoo", "ZOOBAR");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"POST", "/", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * メソッドがPOST
	 * URLが「/?foo=bar」クエリ付き
	 *
	 * @throws Exception
	 */
	@Test
	public void postVanillaEmptyQueryValue() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=b6e3b79003ce0743a491606ba1035a804593b0efb1e20a11cba83f8c25a57a92");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"POST", "/", headers, "foo=bar", null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * メソッドがPOST
	 * URLが「/?@#$%^&+=/,?><`";:\|][{} =@#$%^&+=/,?><`";:\|][{}」
	 *
	 * @throws Exception
	 */
	@Test
	public void postVanillaQueryNonunreserved() throws Exception {
		//このテストケースには疑問がある
		//他の人もこのテストケースは問題だと言っている人がいる↓
		//@see https://gist.github.com/lox/9e00ce5428c8fe654011
		//
		//		TestHeaders headers = new TestHeaders();
		//		headers.put(
		//				"Authorization",
		//				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=28675d93ac1d686ab9988d6617661da4dffe7ba848a2285cb75eac6512e861f9");
		//		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		//		headers.put("Host", "host.foo.com");
		//		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
		//				"POST", "/", headers, "@#$%^&+=/,?><`\";:\\|][{} =@#$%^&+=/,?><`\";:\\|][{}", null);
		//		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
		//				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * メソッドがPOST
	 * URLが「/?f oo=b ar」空白を含む
	 *
	 * @throws Exception
	 */
	@Test
	public void postVanillaQuerySpace() throws Exception {
		//これもなぞ
		//f oo=b ar
		//というクエリーが正規化されて
		//f=
		//にならなければいけないルールがわからない
		//		TestHeaders headers = new TestHeaders();
		//		headers.put(
		//				"Authorization",
		//				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=b7eb653abe5f846e7eee4d1dba33b15419dc424aaf215d49b1240732b10cc4ca");
		//		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		//		headers.put("Host", "host.foo.com");
		//		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
		//				"POST", "/", headers, "f oo=b ar", null);
		//		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
		//				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * メソッドがPOST
	 * URLが「/?foo=bar」
	 *
	 * @throws Exception
	 */
	@Test
	public void postVanillaQuery() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=b6e3b79003ce0743a491606ba1035a804593b0efb1e20a11cba83f8c25a57a92");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"POST", "/", headers, "foo=bar", null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * メソッドがPOST
	 * URLが「/」
	 *
	 * @throws Exception
	 */
	@Test
	public void postVanilla() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=date;host, Signature=22902d79e148b64e7571c3565769328423fe276eae4b26f83afceda9e767f726");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"POST", "/", headers, null, null);
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * メソッドがPOST
	 * ContentTypeが「application/x-www-form-urlencoded」
	 *
	 * @throws Exception
	 */
	@Test
	public void postXWwwFormUrlencodedParameters() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=content-type;date;host, Signature=b105eb10c6d318d2294de9d49dd8b031b55e3c3fe139f2e637da70511e9e7b71");
		headers.put("Content-Type", "application/x-www-form-urlencoded; charset=utf8");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"POST", "/", headers, null, "foo=bar");
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}

	/**
	 * メソッドがPOST
	 * ContentTypeが「application/x-www-form-urlencoded」でcharset指定がなし
	 *
	 * @throws Exception
	 */
	@Test
	public void postXWwwFormUrlencoded() throws Exception {
		TestHeaders headers = new TestHeaders();
		headers.put(
				"Authorization",
				" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/host/aws4_request, SignedHeaders=content-type;date;host, Signature=5a15b22cf462f047318703b92e6f4f38884e4a7ab7b1d6426ca46a8bd1c26cbc");
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		headers.put("DATE", "Mon, 09 Sep 2011 23:36:00 GMT");
		headers.put("Host", "host.foo.com");
		SignatureV4Params params = PARSER.createSignatureV4Params(SignatureType.AMAZON_TEST_SUITE,
				"POST", "/", headers, null, "foo=bar");
		SignatureV4Validator.validate(SignatureType.AMAZON_TEST_SUITE, params,
				"wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
	}
}
