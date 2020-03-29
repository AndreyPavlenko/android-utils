package me.aap.utils.concurrent;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import me.aap.utils.function.BiConsumer;

/**
 * @author Andrey Pavlenko
 */
public class CompletedFuture<T> implements FutureSupplier<T> {
	private final T result;

	public CompletedFuture(T result) {
		this.result = result;
	}

	@SuppressWarnings("unchecked")
	public static <V> CompletedFuture<V> nullResult() {
		return NullResultFutureHolder.instance;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public T get() {
		return result;
	}

	@Override
	public T get(long timeout, @NonNull TimeUnit unit) {
		return result;
	}

	public void addConsumer(@Nullable BiConsumer<T, Throwable> c, @Nullable Handler handler) {
		if (c == null) return;
		if ((handler == null) || (c instanceof CompletableFuture) || handler.getLooper().isCurrentThread()) {
			c.accept(result, null);
		} else {
			handler.post(() -> c.accept(result, null));
		}
	}

	@SuppressWarnings("unchecked")
	private interface NullResultFutureHolder {
		CompletedFuture instance = new CompletedFuture(null);
	}
}
