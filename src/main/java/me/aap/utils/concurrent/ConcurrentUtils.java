package me.aap.utils.concurrent;

import android.os.Looper;
import android.util.Log;

import me.aap.utils.BuildConfig;
import me.aap.utils.app.App;
import me.aap.utils.function.Consumer;

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

	public static <T> void consumeInMainThread(Consumer<T> c, T t) {
		if (c != null) {
			if ((c instanceof CompletableFuture) || isMainThread()) c.accept(t);
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
}
