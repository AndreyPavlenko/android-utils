package me.aap.utils.app;

import android.annotation.SuppressLint;
import android.os.Handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
}
