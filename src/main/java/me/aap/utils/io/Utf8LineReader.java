package me.aap.utils.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Andrey Pavlenko
 */
public class Utf8LineReader extends Utf8Reader {
	private int prev = -1;

	public Utf8LineReader(InputStream in) {
		super(in);
	}

	@Override
	public int read() throws IOException {
		if (prev != -1) {
			int c = prev;
			prev = -1;
			return c;
		}
		return super.read();
	}

	public int readLine(Appendable a) throws IOException {
		for (int n = 0; ; n++) {
			int c = read();
			if (c == -1) return (n == 0) ? -1 : n;

			switch (c) {
				case '\n':
					return n;
				case '\r':
					c = read();
					if (c != '\n') prev = c;
					return n;
				default:
					a.append((char) c);
			}
		}
	}

	public int skipLine() throws IOException {
		return readLine(IoUtils.nullWriter());
	}
}
