package me.aap.utils.misc;

import java.util.Objects;

import me.aap.utils.BuildConfig;

import static me.aap.utils.concurrent.ConcurrentUtils.isMainThread;

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

	public static void assertNull(Object o) {
		if (BuildConfig.DEBUG && (o != null)) throw new AssertionError();
	}

	public static void assertNotNull(Object o) {
		if (BuildConfig.DEBUG && (o == null)) throw new AssertionError();
	}

	public static void assertSame(Object o1, Object o2) {
		if (BuildConfig.DEBUG && (o1 != o2)) throw new AssertionError();
	}

	public static void assertNotSame(Object o1, Object o2) {
		if (BuildConfig.DEBUG && (o1 == o2)) throw new AssertionError();
	}

	public static void assertEquals(Object o1, Object o2) {
		if (BuildConfig.DEBUG && !Objects.equals(o1, o2)) throw new AssertionError();
	}

	public static void assertNotEquals(Object o1, Object o2) {
		if (BuildConfig.DEBUG && Objects.equals(o1, o2)) throw new AssertionError();
	}

	public static void assertEquals(int i1, int i2) {
		if (BuildConfig.DEBUG && (i1 != i2)) throw new AssertionError();
	}

	public static void assertNotEquals(int i1, int i2) {
		if (BuildConfig.DEBUG && (i1 == i2)) throw new AssertionError();
	}

	public static void assertEquals(long l1, long l2) {
		if (BuildConfig.DEBUG && (l1 != l2)) throw new AssertionError();
	}

	public static void assertNotEquals(long l1, long l2) {
		if (BuildConfig.DEBUG && (l1 == l2)) throw new AssertionError();
	}

	public static void assertMainThread() {
		if (BuildConfig.DEBUG && !isMainThread()) throw new AssertionError();
	}

	public static void assertNotMainThread() {
		if (BuildConfig.DEBUG && isMainThread()) throw new AssertionError();
	}
}
