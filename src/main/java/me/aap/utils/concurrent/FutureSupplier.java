package me.aap.utils.concurrent;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Andrey Pavlenko
 */
public interface FutureSupplier<T> extends Future<T> {

	void addConsumer(@Nullable BiConsumer<T, Throwable> c, @Nullable Handler handler);

	default void addConsumer(@Nullable BiConsumer<T, Throwable> c) {
		addConsumer(c, null);
	}

	default void addConsumer(@Nullable Consumer<T> c) {
		addConsumer(c, null, null);
	}

	default void addConsumer(@Nullable Consumer<T> c, @Nullable Supplier<T> onError) {
		addConsumer(c, onError, null);
	}

	@SuppressWarnings("unchecked")
	default void addConsumer(@Nullable Consumer<T> c, @Nullable Supplier<T> onError, @Nullable Handler handler) {
		if (c == null) return;
		if (c instanceof CompletableFuture) addConsumer((BiConsumer) c);
		else addConsumer((v, err) -> {
			if (err == null) {
				c.accept(v);
			} else {
				Log.e(getClass().getName(), err.getMessage(), err);
				c.accept((onError != null) ? onError.get() : null);
			}
		}, handler);
	}


	default T get(@Nullable Supplier<T> onError) {
		try {
			return get();
		} catch (Exception ex) {
			Log.e(getClass().getName(), ex.getMessage(), ex);
			return (onError != null) ? onError.get() : null;
		}
	}

	default T get(@Nullable Supplier<T> onError, long timeout, @NonNull TimeUnit unit) {
		try {
			return get(timeout, unit);
		} catch (Exception ex) {
			Log.e(getClass().getName(), ex.getMessage(), ex);
			return (onError != null) ? onError.get() : null;
		}
	}
}
