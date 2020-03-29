package me.aap.utils.concurrent;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import me.aap.utils.function.BiConsumer;
import me.aap.utils.function.Consumer;

/**
 * @author Andrey Pavlenko
 */
public class CompletableFuture<T> implements FutureSupplier<T>, Consumer<T>, BiConsumer<T, Throwable> {
	private static final Object NULL = new Object();
	private static final Object CANCELLED = new Object();
	private final long defaultTimeout;
	private Object value = NULL;
	private List<BiConsumer<T, Throwable>> consumers = Collections.emptyList();

	public CompletableFuture() {
		this(0);
	}

	public CompletableFuture(long defaultTimeoutMs) {
		this.defaultTimeout = defaultTimeoutMs;
	}

	public CompletableFuture(long defaultTimeout, TimeUnit unit) {
		this(unit.toMillis(defaultTimeout));
	}

	@Override
	public final void accept(T value) {
		complete(value);
	}

	@Override
	public final void accept(T value, Throwable ex) {
		if (ex != null) completeExceptionally(ex);
		else complete(value);
	}

	@SuppressWarnings("unchecked")
	public void addConsumer(@Nullable BiConsumer<T, Throwable> c, @Nullable Handler handler) {
		if (c == null) return;

		Object value;
		BiConsumer<T, Throwable> consumer;

		if ((handler == null) || (c instanceof CompletableFuture)) {
			consumer = c;
		} else {
			consumer = (v, e) -> {
				if (handler.getLooper().isCurrentThread()) c.accept(v, e);
				else handler.post(() -> c.accept(v, e));
			};
		}

		synchronized (this) {
			if ((value = this.value) == NULL) {
				if (consumers == Collections.EMPTY_LIST) {
					consumers = Collections.singletonList(consumer);
					return;
				} else if (consumers.size() == 1) {
					consumers = new ArrayList<>(consumers);
				}

				consumers.add(consumer);
				return;
			}
		}

		if (value == CANCELLED) {
			consumer.accept(null, new CancellationException());
		} else if (value instanceof ExceptionWrapper) {
			consumer.accept(null, new ExecutionException(((ExceptionWrapper) value).ex));
		} else {
			consumer.accept((T) value, null);
		}
	}

	public synchronized boolean complete(T value) {
		if (this.value != NULL) return false;
		this.value = value;
		notifyAll();
		supplyValue();
		return true;
	}

	public synchronized boolean completeExceptionally(Throwable ex) {
		if (this.value != NULL) return false;
		this.value = new ExceptionWrapper(ex);
		notifyAll();
		supplyValue();
		return true;
	}

	@Override
	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		if (this.value != NULL) return false;
		this.value = CANCELLED;
		notifyAll();
		supplyValue();
		return true;
	}

	@Override
	public synchronized boolean isCancelled() {
		return value == CANCELLED;
	}

	@Override
	public synchronized boolean isDone() {
		return value != NULL;
	}

	@Override
	public synchronized T get() throws ExecutionException, InterruptedException {
		if (defaultTimeout > 0) {
			try {
				return get(defaultTimeout, TimeUnit.MILLISECONDS);
			} catch (TimeoutException ex) {
				throw new InterruptedException("Interrupted due to timeout: " + defaultTimeout);
			}
		}

		while (value == NULL) {
			ConcurrentUtils.wait(this);
		}

		return getValue();
	}

	@Override
	public synchronized T get(long timeout, @NonNull TimeUnit unit) throws ExecutionException,
			InterruptedException, TimeoutException {
		if (value != NULL) return getValue();

		long startTime = System.currentTimeMillis();
		long waitTime = unit.toMillis(timeout);

		for (; ; ) {
			ConcurrentUtils.wait(this, waitTime);
			if (value != NULL) return getValue();

			waitTime -= System.currentTimeMillis() - startTime;
			if (waitTime <= 0) throw new TimeoutException();
		}
	}

	@SuppressWarnings("unchecked")
	private T getValue() throws ExecutionException {
		if (value == CANCELLED) {
			throw new CancellationException();
		} else if (value instanceof ExceptionWrapper) {
			throw new ExecutionException(((ExceptionWrapper) value).ex);
		} else {
			return (T) value;
		}
	}

	@SuppressWarnings("unchecked")
	private void supplyValue() {
		if (consumers.isEmpty()) return;

		if (value == CANCELLED) {
			CancellationException ex = new CancellationException();
			for (BiConsumer<T, Throwable> c : consumers) {
				c.accept(null, ex);
			}
		} else if (value instanceof ExceptionWrapper) {
			ExecutionException ex = new ExecutionException(((ExceptionWrapper) value).ex);
			for (BiConsumer<T, Throwable> c : consumers) {
				c.accept(null, ex);
			}
		} else {
			T v = (T) value;
			for (BiConsumer<T, Throwable> c : consumers) {
				c.accept(v, null);
			}
		}
	}

	private static final class ExceptionWrapper {
		final Throwable ex;

		public ExceptionWrapper(Throwable ex) {
			this.ex = ex;
		}
	}
}
