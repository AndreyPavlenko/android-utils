package me.aap.utils.misc;

import me.aap.utils.BuildConfig;

/**
 * @author Andrey Pavlenko
 */
public class Assert {

	public static void assertTrue(boolean b) {
		if (BuildConfig.DEBUG && !b) throw new AssertionError();
	}

	public static void assertFalse(boolean b) {
		if (BuildConfig.DEBUG && b) throw new AssertionError();
	}

	public static void assertSame(Object o1, Object o2) {
		if (BuildConfig.DEBUG && (o1 != o2)) throw new AssertionError();
	}

	public static void assertNotSame(Object o1, Object o2) {
		if (BuildConfig.DEBUG && (o1 == o2)) throw new AssertionError();
	}
}
