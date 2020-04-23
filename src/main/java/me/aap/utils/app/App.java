package me.aap.utils.app;

import android.annotation.SuppressLint;
import android.os.Handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.concurrent.ThreadPool;
import me.aap.utils.function.CheckedRunnable;
import me.aap.utils.function.CheckedSupplier;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.failed;
import static me.aap.utils.concurrent.ConcurrentUtils.isMainThread;

/**
 * @author Andrey Pavlenko
 */
public class App extends android.app.Application {
	@SuppressLint("StaticFieldLeak")
	private static App instance;
	private volatile Handler handler;
	private volatile ThreadPool executor;

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

	public ThreadPool getExecutor() {
		ThreadPool e = executor;

		if (e == null) {
			synchronized (this) {
				if ((e = executor) == null) {
					executor = e = createExecutor();
				}
			}
		}

		return e;
	}

	public FutureSupplier<?> execute(CheckedRunnable task) {
		return execute(task, null);
	}

	public <T> FutureSupplier<T> execute(CheckedRunnable<Throwable> task, T result) {
		if (isMainThread()) {
			return getExecutor().submit(task, result);
		} else {
			try {
				task.run();
				return completed(result);
			} catch (Throwable ex) {
				return failed(ex);
			}
		}
	}

	public <T> FutureSupplier<T> execute(CheckedSupplier<T, Throwable> task) {
		if (isMainThread()) {
			return getExecutor().submit(task);
		} else {
			try {
				return completed(task.get());
			} catch (Throwable ex) {
				return failed(ex);
			}
		}
	}

	protected ThreadPool createExecutor() {
		return new ThreadPool(getNumberOfCoreThreads(), getMaxNumberOfThreads(), 60L, TimeUnit.SECONDS);
	}

	protected int getNumberOfCoreThreads() {
		return getMaxNumberOfThreads();
	}

	protected int getMaxNumberOfThreads() {
		return 1;
	}
}
