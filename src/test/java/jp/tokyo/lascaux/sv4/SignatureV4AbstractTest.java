package jp.tokyo.lascaux.sv4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import jp.tokyo.lascaux.sv4.SignatureV4RequestParser.Headers;

public abstract class SignatureV4AbstractTest {
	protected static SignatureV4RequestParser PARSER;

	@BeforeClass
	public static void before() throws Exception {
		PARSER = new SignatureV4RequestParser();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@AfterClass
	public static void after() throws Exception {
	}

	protected static class TestHeaders implements Headers {
		private Map<String, Object> headers;

		protected TestHeaders() {
			headers = new HashMap<>();
		}

		protected void put(String key, String value) {
			Object obj = headers.get(key);
			if (obj == null) {
				headers.put(key, value);
			} else {
				if (obj instanceof List) {
					@SuppressWarnings("unchecked")
					List<Object> list = (List<Object>) obj;
					list.add(value);
					headers.put(key, list);
				} else {
					List<Object> list = new ArrayList<>();
					list.add(obj);
					list.add(value);
					headers.put(key, list);
				}
			}
		}

		@Override
		public Enumeration<String> getHeaderNames() {
			return Collections.enumeration(headers.keySet());
		}

		@Override
		public String getHeader(String key) {
			Object obj = headers.get(key);
			return obj instanceof List ? null : (String) obj;
		}

		@Override
		public Enumeration<String> getHeaders(String key) {
			Object obj = headers.get(key);
			@SuppressWarnings({ "unchecked", "rawtypes" })
			List<String> list = obj instanceof List ? (List) obj : null;
			return Collections.enumeration(list);
		}
	}
}
