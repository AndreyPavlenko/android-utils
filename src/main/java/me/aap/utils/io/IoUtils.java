package me.aap.utils.io;

import java.nio.ByteBuffer;

import me.aap.utils.log.Log;

/**
 * @author Andrey Pavlenko
 */
public class IoUtils {

	public static void close(AutoCloseable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Exception ex) {
				Log.d(ex, "Failed to close ", c);
			}
		}
	}

	public static void close(AutoCloseable... closeables) {
		if (closeables != null) {
			for (AutoCloseable c : closeables) {
				close(c);
			}
		}
	}

	public static ByteBuffer emptyByteBuffer() {
		return EmptyByteBuf.instance;
	}

	private interface EmptyByteBuf {
		ByteBuffer instance = ByteBuffer.allocate(0);
	}
}
