package me.aap.utils.net;

/**
 * @author Andrey Pavlenko
 */
public class NetUtils {

	public static StringBuilder decodeUrl(CharSequence encoded, StringBuilder sb, boolean fail) {
		int i = 0;
		int len = encoded.length();

		loop:
		for (; i < len; i++) {
			char c = encoded.charAt(i);

			if ((c >= '0') && (c <= '9')
					|| ((c >= 'a') && (c <= 'z'))
					|| (c >= 'A') && (c <= 'Z')) {
				sb.append(c);
			} else if (c == '+') {
				sb.append(' ');
			} else if (c == '%') {
				if (i < len - 2) {
// TODO: implement
				} else {
					break loop;
				}
			} else {
				switch (c) {
					case '*':
					case '-':
					case '.':
					case '_':
						sb.append(' ');
						break;
					default:
						break loop;
				}
			}
		}

		if (i != len) {
			if (fail) throw new IllegalArgumentException("Invalid encoded URL: " + encoded);
			sb.append(encoded, i, len);
		}

		return sb;
	}
}
