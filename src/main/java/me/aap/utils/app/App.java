package me.aap.utils.app;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.aap.utils.concurrent.AppThread;
import me.aap.utils.function.Consumer;
import me.aap.utils.function.Supplier;

import static me.aap.utils.concurrent.ConcurrentUtils.consumeInMainThread;
import static me.aap.utils.concurrent.ConcurrentUtils.isMainThread;

/**
 * @author Andrey Pavlenko
 */
public class App extends android.app.Application implements ThreadFactory,
		Thread.UncaughtExceptionHandler {
	@SuppressLint("StaticFieldLeak")
	private static App instance;
	private int threadCounter;
	private volatile Handler handler;
	private volatile ExecutorService executor;

	@SuppressWarnings("unchecked")
	public static <C extends App> C get() {
		return (C) instance;
	}

	@Override
	public void onCreate() {
		instance = this;
		super.onCreate();
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		instance = null;
		ExecutorService e = executor;
		if (e != null) e.shutdown();
	}

	public Handler getHandler() {
		Handler h = handler;

		if (h == null) {
			synchronized (this) {
				if ((h = handler) == null) {
					handler = h = new Handler(getApplicationContext().getMainLooper());
				}
			}
		}

		return h;
	}

	public ExecutorService getExecutor() {
		ExecutorService e = executor;

		if (e == null) {
			synchronized (this) {
				if ((e = executor) == null) {
					executor = e = createExecutor();
				}
			}
		}

		return e;
	}

	public void execute(Runnable run) {
		if (isMainThread()) getExecutor().submit(run);
		else run.run();
	}

	public <T> void execute(Supplier<T> resultSupplier, @Nullable Consumer<T> resultConsumer) {
		if (isMainThread()) {
			getExecutor().submit(() -> {
				T result = resultSupplier.get();
				consumeInMainThread(resultConsumer, result);
			});
		} else {
			T result = resultSupplier.get();
			consumeInMainThread(resultConsumer, result);
		}
	}

	protected ExecutorService createExecutor() {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(getNumberOfCoreThreads(),
				getMaxNumberOfThreads(),
				60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>());
		executor.setThreadFactory(this);
		executor.allowCoreThreadTimeOut(true);
		return executor;
	}

	@Override
	public Thread newThread(@NonNull Runnable r) {
		int n;

		synchronized (this) {
			n = ++threadCounter;
		}

		AppThread t = new AppThread(r, getPackageName() + '-' + n);
		t.setUncaughtExceptionHandler(this);
		return t;
	}

	@Override
	public void uncaughtException(@NonNull Thread t, @NonNull Throwable ex) {
		Log.e(getPackageName(), "Exception in thread " + t, ex);
	}

	protected int getNumberOfCoreThreads() {
		return 1;
	}

	protected int getMaxNumberOfThreads() {
		return 1;
	}
}
