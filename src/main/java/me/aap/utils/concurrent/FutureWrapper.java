package me.aap.utils.concurrent;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.function.BiConsumer;

/**
 * @author Andrey Pavlenko
 */
public class FutureWrapper<T> implements FutureSupplier<T> {
	private final Future<T> future;

	public FutureWrapper(Future<T> future) {
		this.future = future;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return future.isCancelled();
	}

	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Override
	public T get() throws ExecutionException, InterruptedException {
		return future.get();
	}

	@Override
	public T get(long timeout, @NonNull TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
		return future.get(timeout, unit);
	}

	@Override
	public void addConsumer(@Nullable BiConsumer<T, Throwable> c, @Nullable Handler handler) {
		throw new UnsupportedOperationException();
	}
}
