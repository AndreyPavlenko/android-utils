package me.aap.utils.text;

import android.util.Log;

import me.aap.utils.concurrent.ConcurrentUtils;

/**
 * @author Andrey Pavlenko
 */
public class TextUtils {

	public static StringBuilder getSharedStringBuilder() {
		if (!ConcurrentUtils.isMainThread()) return new StringBuilder();
		StringBuilderHolder.sharedStringBuilder.setLength(0);
		return StringBuilderHolder.sharedStringBuilder;
	}

	public static StringBuilder getSharedStringBuilder(int minCapacity) {
		if (!ConcurrentUtils.isMainThread()) return new StringBuilder(minCapacity);
		StringBuilder sb = StringBuilderHolder.sharedStringBuilder;
		sb.ensureCapacity(minCapacity);
		sb.setLength(0);
		return sb;
	}

	public static int indexOfChar(CharSequence seq, int from, int to, CharSequence chars) {
		for (int len = chars.length(); from < to; from++) {
			char c = seq.charAt(from);
			for (int n = 0; n < len; n++) {
				if (chars.charAt(n) == c) return from;
			}
		}
		return -1;
	}

	public static long toLong(CharSequence seq, int from, int to, long defaultValue) {
		try {
			return Long.parseLong(seq.subSequence(from, to).toString());
		} catch (NumberFormatException ignore) {
			return defaultValue;
		}
	}

	public static String timeToString(int seconds) {
		StringBuilder sb = getSharedStringBuilder();
		timeToString(sb, seconds);
		return sb.toString();
	}

	public static void timeToString(StringBuilder sb, int seconds) {
		if (seconds < 60) {
			sb.append("00:");
			appendTime(sb, seconds);
		} else if (seconds < 3600) {
			int m = seconds / 60;
			appendTime(sb, m);
			sb.append(':');
			appendTime(sb, seconds - (m * 60));
		} else {
			int h = seconds / 3600;
			appendTime(sb, h);
			sb.append(':');
			timeToString(sb, seconds - (h * 3600));
		}
	}

	private static void appendTime(StringBuilder sb, int time) {
		if (time < 10) sb.append(0);
		sb.append(time);
	}

	public static int stringToTime(String s) {
		String[] values = s.split(":");

		try {
			if (values.length == 2) {
				return Integer.parseInt(values[0]) * 60 + Integer.parseInt(values[1]);
			} else if (values.length == 3) {
				return Integer.parseInt(values[0]) * 3600 + Integer.parseInt(values[1]) * 60
						+ Integer.parseInt(values[2]);
			}
		} catch (NumberFormatException ex) {
			Log.w("Utils", "Invalid time string: " + s, ex);
		}

		return -1;
	}

	public static boolean containsWord(String s, String word) {
		int idx = s.indexOf(word);

		if (idx != -1) {
			if ((idx == 0) || isSepChar(s.charAt(idx - 1))) {
				idx += word.length();
				return (idx == s.length()) || ((idx < s.length()) && isSepChar(s.charAt(idx)));
			}
		}

		return false;
	}

	private static boolean isSepChar(char c) {
		switch (c) {
			case '.':
			case ',':
			case '\'':
			case ';':
			case ':':
			case ')':
			case '(':
				return true;
			default:
				return c <= '"';
		}
	}

	public static String toHexString(byte[] bytes) {
		return appendHexString(getSharedStringBuilder(bytes.length * 2), bytes).toString();
	}

	public static StringBuilder appendHexString(StringBuilder sb, byte[] bytes) {
		for (final byte b : bytes) {
			int v = b & 0xFF;
			sb.append(HexTable._table[v >>> 4]).append(HexTable._table[v & 0xF]);
		}
		return sb;
	}

	private static final class HexTable {
		static final char[] _table = {'0', '1', '2', '3', '4', '5', '6', '7',
				'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	}

	private interface StringBuilderHolder {
		StringBuilder sharedStringBuilder = new StringBuilder();
	}
}
