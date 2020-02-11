package me.aap.utils.misc;

import android.os.Looper;

import me.aap.utils.BuildConfig;

/**
 * @author Andrey Pavlenko
 */
public class MiscUtils {
	public static void assertTrue(boolean b) {
		if (BuildConfig.DEBUG && !b) throw new AssertionError();
	}
}
