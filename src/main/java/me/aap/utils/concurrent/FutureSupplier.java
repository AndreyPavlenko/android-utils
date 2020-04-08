package me.aap.utils.concurrent;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import me.aap.utils.function.BiConsumer;
import me.aap.utils.function.Consumer;
import me.aap.utils.function.Supplier;

/**
 * @author Andrey Pavlenko
 */
public interface FutureSupplier<T> extends Future<T> {

	void addConsumer(@Nullable BiConsumer<T, Throwable> c, @Nullable Handler handler, boolean runInSameThread);

	default void addConsumer(@Nullable BiConsumer<T, Throwable> c, @Nullable Handler handler) {
		addConsumer(c, handler, true);
	}

	default void addConsumer(@Nullable BiConsumer<T, Throwable> c) {
		addConsumer(c, null, true);
	}

	default void addConsumer(@Nullable Consumer<T> c) {
		addConsumer(c, null, null, true);
	}

	default void addConsumer(@Nullable Consumer<T> c, @Nullable Supplier<T> onError) {
		addConsumer(c, onError, null, true);
	}

	default void addConsumer(@Nullable Consumer<T> c, @Nullable Supplier<T> onError,
													 @Nullable Handler handler) {
		addConsumer(c, onError, handler, true);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	default void addConsumer(@Nullable Consumer<T> c, @Nullable Supplier<T> onError,
													 @Nullable Handler handler, boolean runInSameThread) {
		if (c == null) return;

		if (c instanceof BiConsumer) {
			addConsumer((BiConsumer) c, null, false);
		} else {
			BiConsumer<T, Throwable> consumer = new BiConsumer<T, Throwable>() {

				@Override
				public void accept(T v, Throwable err) {
					if (err == null) {
						c.accept(v);
					} else {
						Log.e(getClass().getName(), err.getMessage(), err);
						c.accept((onError != null) ? onError.get() : null);
					}
				}

				@Override
				public boolean canBlockThread() {
					return c.canBlockThread();
				}
			};

			addConsumer(consumer, handler, runInSameThread);
		}
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
