package me.aap.utils.text;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

/**
 * @author Andrey Pavlenko
 */
public class TextUtils {

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
		try (SharedTextBuilder tb = SharedTextBuilder.get()) {
			timeToString(tb, seconds);
			return tb.toString();
		}
	}

	public static void timeToString(TextBuilder tb, int seconds) {
		timeToString(tb.getStringBuilder(), seconds);
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

	public static byte[] toByteArray(CharSequence s) {
		return toByteArray(s, Charset.defaultCharset());
	}

	public static byte[] toByteArray(CharSequence s, Charset cs) {
		return toByteArray(s, cs, 0, s.length());
	}

	public static byte[] toByteArray(CharSequence s, Charset cs,
																	 int fromIndex, int toIndex) {
		if (s.getClass() == String.class) {
			return ((String) s).getBytes(cs);
		} else {
			CharsetEncoder ce = cs.newEncoder();
			int len = toIndex - fromIndex;
			int maxLen = (int) (len * (double) ce.maxBytesPerChar());
			byte[] ba = new byte[maxLen];
			ByteBuffer bb = ByteBuffer.wrap(ba);
			CharBuffer cb = CharBuffer.wrap(s, fromIndex, toIndex);
			ce.onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);

			try {
				CoderResult cr = ce.encode(cb, bb, true);

				if (!cr.isUnderflow()) {
					cr.throwException();
				}

				if (!(cr = ce.flush(bb)).isUnderflow()) {
					cr.throwException();
				}
			} catch (CharacterCodingException x) {
				throw new Error(x);
			}

			return (bb.position() == ba.length) ? ba : Arrays.copyOf(ba, bb.position());
		}
	}

	public static String toHexString(byte[] bytes) {
		try (SharedTextBuilder tb = SharedTextBuilder.get(bytes.length * 2)) {
			return appendHexString(tb, bytes).toString();
		}
	}

	public static TextBuilder appendHexString(TextBuilder tb, byte[] bytes) {
		appendHexString(tb.getStringBuilder(), bytes);
		return tb;
	}

	public static StringBuilder appendHexString(StringBuilder sb, byte[] bytes) {
		for (byte b : bytes) {
			int v = b & 0xFF;
			sb.append(HexTable._table[v >>> 4]).append(HexTable._table[v & 0xF]);
		}
		return sb;
	}

	private static class HexTable {
		static char[] _table = {'0', '1', '2', '3', '4', '5', '6', '7',
				'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	}
}
