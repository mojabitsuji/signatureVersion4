package jp.tokyo.lascaux.sv4;

import static org.junit.Assert.assertEquals;

import java.util.regex.Matcher;

import org.junit.Test;

public class SignatureV4RequestParserTest {
	public void patternTest() {

	}

	/**
	 * ヘッダーのAuthorizationのパースパターンをテストします。
	 */
	@Test
	public void parseHeaderAuthDelimiterPattern() {
		verify(" AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/iam/aws4_request, SignedHeaders=content-type;host;x-amz-date, Signature=ced6826de92d2bdeed8f846f0bf508e8559e98e4b0199114b84c54174deb456c",
				true);
		//先頭に空白がない
		verify("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/iam/aws4_request, SignedHeaders=content-type;host;x-amz-date, Signature=ced6826de92d2bdeed8f846f0bf508e8559e98e4b0199114b84c54174deb456c",
				true);
		//最後に空白がある
		verify("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/iam/aws4_request, SignedHeaders=content-type;host;x-amz-date, Signature=ced6826de92d2bdeed8f846f0bf508e8559e98e4b0199114b84c54174deb456c ",
				true);
		//カンマの前に空白
		verify("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/iam/aws4_request , SignedHeaders=content-type;host;x-amz-date , Signature=ced6826de92d2bdeed8f846f0bf508e8559e98e4b0199114b84c54174deb456c ",
				true);
		//カンマの前後に複数の空白
		verify("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/iam/aws4_request  ,  SignedHeaders=content-type;host;x-amz-date  ,  Signature=ced6826de92d2bdeed8f846f0bf508e8559e98e4b0199114b84c54174deb456c  ",
				true);
	}

	private static void verify(String target, boolean expected) {
		Matcher m = SignatureV4RequestParser.HEADER_AUTHORIZATION_PATTERN.matcher(target);
		boolean b = m.matches();
		assertEquals(expected, b);
		if (b) {
			assertEquals("AWS4-HMAC-SHA256", m.group(1));
			assertEquals("AKIDEXAMPLE/20110909/us-east-1/iam/aws4_request", m.group(3));
			assertEquals("content-type;host;x-amz-date", m.group(5));
			assertEquals("ced6826de92d2bdeed8f846f0bf508e8559e98e4b0199114b84c54174deb456c",
					m.group(7));
		}
	}
}
