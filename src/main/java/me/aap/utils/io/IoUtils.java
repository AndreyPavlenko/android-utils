package me.aap.utils.io;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Andrey Pavlenko
 */
public class IoUtils {

	public static void close(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (IOException ex) {
				Log.d(IoUtils.class.getName(), "Failed to close " + c, ex);
			}
		}
	}

	public static void close(Closeable... closeables) {
		if (closeables != null) {
			for (Closeable c : closeables) {
				close(c);
			}
		}
	}
}
