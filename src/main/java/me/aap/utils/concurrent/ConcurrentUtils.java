package me.aap.utils.concurrent;

import android.os.Looper;

import me.aap.utils.BuildConfig;

/**
 * @author Andrey Pavlenko
 */
public class ConcurrentUtils {

	public static boolean isMainThread() {
		return (Thread.currentThread() == Looper.getMainLooper().getThread());
	}

	public static void ensureMainThread(boolean debug) {
		if (debug && !BuildConfig.DEBUG) return;
		if (!isMainThread()) throw new AssertionError("Not the main thread");
	}

	public static void ensureNotMainThread(boolean debug) {
		if (debug && !BuildConfig.DEBUG) return;
		if (isMainThread()) throw new AssertionError("Main thread");
	}
}
