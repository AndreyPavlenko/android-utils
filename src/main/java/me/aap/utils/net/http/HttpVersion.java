package me.aap.utils.net.http;

import me.aap.utils.log.Log;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * @author Andrey Pavlenko
 */
public enum HttpVersion {
	HTTP_1_0("HTTP/1.0"), HTTP_1_1("HTTP/1.1"), UNSUPPORTED("UNSUPPORTED");

	private final String str;
	final byte[] bytes;

	HttpVersion(String str) {
		this.str = str;
		bytes = str.getBytes(US_ASCII);
	}

	static HttpVersion get(byte[] buf, int start, int end) {
		if ((end - start) < 8) return null;

		if ((buf[start] == 'H') && (buf[start + 1] == 'T') && (buf[start + 2] == 'T') &&
				(buf[start + 3] == 'P') && (buf[start + 4] == '/') && (buf[start + 5] == '1') &&
				(buf[start + 6] == '.')) {
			char c = (char) buf[start + 7];
			return (c == '1') ? HTTP_1_1 : (c == '0') ? HTTP_1_0 : UNSUPPORTED;
		}

		return UNSUPPORTED;
	}

	int length() {
		return name().length();
	}

	public static HttpVersion fromString(String s) {
		switch (s) {
			case "HTTP/1.0":
				return HTTP_1_0;
			case "HTTP/1.1":
				return HTTP_1_1;
			default:
				Log.w(HttpVersion.class.getName(), "Unsupported HTTP version: " + s);
				return null;
		}
	}

	@Override
	public String toString() {
		return str;
	}
}
