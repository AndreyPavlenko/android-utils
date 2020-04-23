package me.aap.utils.concurrent;

import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.concurrent.locks.LockSupport;

import me.aap.utils.BuildConfig;
import me.aap.utils.app.App;
import me.aap.utils.function.Consumer;
import me.aap.utils.misc.MiscUtils;

/**
 * @author Andrey Pavlenko
 */
public class ConcurrentUtils {

	public static boolean isMainThread() {
		return !MiscUtils.isTestMode() && Looper.getMainLooper().isCurrentThread();
	}

	public static void ensureMainThread(boolean debug) {
		if (debug && !BuildConfig.DEBUG) return;
		if (!isMainThread()) throw new AssertionError("Not the main thread");
	}

	public static void ensureNotMainThread(boolean debug) {
		if (debug && !BuildConfig.DEBUG) return;
		if (isMainThread()) throw new AssertionError("Main thread");
	}

	public static <T> void consumeInMainThread(@Nullable Consumer<T> c, T t) {
		if (c != null) {
			if (isMainThread()) c.accept(t);
			else App.get().getHandler().post(() -> c.accept(t));
		}
	}

	public static void wait(Object monitor) throws InterruptedException {
		if (BuildConfig.DEBUG && isMainThread()) {
			Log.w(ConcurrentUtils.class.getName(), "Waiting on the main thread!", new Throwable());
		}
		monitor.wait();
	}

	public static void wait(Object monitor, long timeout) throws InterruptedException {
		if (BuildConfig.DEBUG && isMainThread()) {
			Log.w(ConcurrentUtils.class.getName(), "Waiting on the main thread!", new Throwable());
		}
		monitor.wait(timeout);
	}

	public static void park() {
		if (BuildConfig.DEBUG && isMainThread()) {
			Log.w(ConcurrentUtils.class.getName(), "Parking the main thread!", new Throwable());
		}

		LockSupport.park();
	}

	public static void parkNanos(long nanos) {
		if (BuildConfig.DEBUG && isMainThread()) {
			Log.w(ConcurrentUtils.class.getName(), "Parking the main thread!", new Throwable());
		}

		LockSupport.parkNanos(nanos);
	}
}
