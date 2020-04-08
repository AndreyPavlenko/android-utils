package me.aap.utils.io;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * @author Andrey Pavlenko
 */
public class MemOutputStream extends ByteArrayOutputStream {

	public MemOutputStream() {
	}

	public MemOutputStream(int size) {
		super(size);
	}

	public byte[] getBuffer() {
		return buf;
	}

	public byte[] trimBuffer() {
		if (buf.length != count) buf = Arrays.copyOf(buf, count);
		return buf;
	}

	public int getCount() {
		return count;
	}
}