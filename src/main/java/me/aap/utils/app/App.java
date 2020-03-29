package me.aap.utils.app;

import android.annotation.SuppressLint;
import android.os.Handler;

import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.aap.utils.function.Consumer;
import me.aap.utils.function.Supplier;

import static me.aap.utils.concurrent.ConcurrentUtils.consumeInMainThread;
import static me.aap.utils.concurrent.ConcurrentUtils.isMainThread;

/**
 * @author Andrey Pavlenko
 */
public class App extends android.app.Application {
	@SuppressLint("StaticFieldLeak")
	private static App instance;
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
					executor = e = Executors.newSingleThreadExecutor();
				}
			}
		}

		return e;
	}

	public <T> void execute(Runnable run) {
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
}
