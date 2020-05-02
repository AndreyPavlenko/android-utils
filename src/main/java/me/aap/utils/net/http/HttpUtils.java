package me.aap.utils.net.http;

/**
 * @author Andrey Pavlenko
 */
class HttpUtils {

	public static long parseLong(byte[] bytes, int start, int end, String endChars, long invalid) {
		if ((start < 0) || (start >= end)) return invalid;

		long value = 0;
		boolean negative = false;

		if (bytes[start] == '-') {
			negative = true;
			start++;
		}

		for (; (start < end) && (bytes[start] == '0'); start++) {
			// Skip padding zero
		}

		for (; (start < end); start++) {
			char c = (char) bytes[start];

			if ((c >= '0') && (c <= '9')) {
				value = value * 10 + ((long) (c - '0'));
			} else if (endChars.indexOf(c) != -1) {
				return negative ? -value : value;
			} else {
				return invalid;
			}
		}

		return negative ? -value : value;
	}

	public static boolean starts(byte[] bytes, int start, int end, CharSequence seq) {
		int len = seq.length();
		if (len > (end - start)) return false;

		for (int i = 0; i < len; i++, start++) {
			if (bytes[start] != seq.charAt(i)) return false;
		}

		return true;
	}

	public static int indexOfChar(byte[] bytes, int start, int end, CharSequence chars) {
		for (int len = chars.length(); start < end; start++) {
			char c = (char) bytes[start];
			for (int n = 0; n < len; n++) {
				if (chars.charAt(n) == c) return start;
			}
		}
		return -1;
	}
}
