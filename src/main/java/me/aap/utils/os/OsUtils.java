package me.aap.utils.os;

import me.aap.utils.app.App;

/**
 * @author Andrey Pavlenko
 */
public class OsUtils {

	public static boolean isAndroid() {
		return Android.isAndroid;
	}

	private static final class Android {
		static final boolean isAndroid;

		static {
			boolean android;
			try {
				android = App.get() != null;
			} catch (Throwable ex) {
				android = false;
			}
			isAndroid = android;
		}
	}
}
