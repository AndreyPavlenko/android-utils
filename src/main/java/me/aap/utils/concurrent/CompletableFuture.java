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
import me.aap.utils.function.CheckedSupplier;
import me.aap.utils.function.Consumer;

import static me.aap.utils.misc.Assert.assertTrue;

/**
 * @author Andrey Pavlenko
 */
public class CompletableFuture<T> implements FutureSupplier<T>, Consumer<T>, BiConsumer<T, Throwable> {
	private final long defaultTimeout;
	private Object value = State.INITIAL;
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

	public boolean canBlockThread() {
		return true;
	}

	@Override
	public void accept(T value) {
		complete(value);
	}

	@Override
	public void accept(T value, Throwable ex) {
		if (ex != null) completeExceptionally(ex);
		else complete(value);
	}

	@SuppressWarnings("unchecked")
	public void addConsumer(@Nullable BiConsumer<T, Throwable> c, @Nullable Handler handler, boolean runInSameThread) {
		if (c == null) return;

		Object value;
		BiConsumer<T, Throwable> consumer;

		if ((handler == null) || c.canBlockThread()) {
			consumer = c;
		} else {
			consumer = (v, e) -> {
				if (runInSameThread && handler.getLooper().isCurrentThread()) c.accept(v, e);
				else handler.post(() -> c.accept(v, e));
			};
		}

		synchronized (this) {
			value = this.value;

			if ((value == State.INITIAL) || (value == State.RUNNING)) {
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

		if (value == State.CANCELLED) {
			consumer.accept(null, new CancellationException());
		} else if (value instanceof ExceptionWrapper) {
			consumer.accept(null, new ExecutionException(((ExceptionWrapper) value).ex));
		} else {
			consumer.accept((T) value, null);
		}
	}

	public synchronized boolean complete(T value) {
		if (isDone()) return false;
		this.value = value;
		notifyAll();
		supplyValue();
		return true;
	}

	public synchronized boolean completeExceptionally(Throwable ex) {
		if (isDone()) return false;
		value = new ExceptionWrapper(ex);
		notifyAll();
		supplyValue();
		return true;
	}

	@Override
	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		if (isDone()) return false;
		value = State.CANCELLED;
		notifyAll();
		supplyValue();
		return true;
	}


	public synchronized boolean setRunning() {
		if (value != State.INITIAL) return false;
		value = State.RUNNING;
		return true;
	}

	public boolean run(CheckedSupplier<T, Throwable> task) {
		return run(task, null);
	}

	public boolean run(CheckedSupplier<T, Throwable> task, @Nullable Runnable ifAlreadyRunning) {
		if (!setRunning()) {
			if (ifAlreadyRunning != null) ifAlreadyRunning.run();
			return false;
		}

		try {
			return complete(task.get());
		} catch (Throwable ex) {
			return completeExceptionally(ex);
		}
	}

	@Override
	public synchronized boolean isCancelled() {
		return value == State.CANCELLED;
	}

	@Override
	public synchronized boolean isDone() {
		return ((value != State.INITIAL) && (value != State.RUNNING));
	}

	public synchronized boolean isRunning() {
		return value == State.RUNNING;
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

		while (!isDone()) {
			ConcurrentUtils.wait(this);
		}

		return getValue();
	}

	@Override
	public synchronized T get(long timeout, @NonNull TimeUnit unit) throws ExecutionException,
			InterruptedException, TimeoutException {
		if (isDone()) return getValue();

		long startTime = System.currentTimeMillis();
		long waitTime = unit.toMillis(timeout);

		for (; ; ) {
			ConcurrentUtils.wait(this, waitTime);
			if (isDone()) return getValue();

			waitTime -= System.currentTimeMillis() - startTime;
			if (waitTime <= 0) throw new TimeoutException();
		}
	}

	@SuppressWarnings("unchecked")
	private T getValue() throws ExecutionException {
		assertTrue(isDone());

		if (value == State.CANCELLED) {
			throw new CancellationException();
		} else if (value instanceof ExceptionWrapper) {
			throw new ExecutionException(((ExceptionWrapper) value).ex);
		} else {
			return (T) value;
		}
	}

	@SuppressWarnings("unchecked")
	private void supplyValue() {
		assertTrue(isDone());
		if (consumers.isEmpty()) return;

		if (value == State.CANCELLED) {
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

	private enum State {
		INITIAL, RUNNING, CANCELLED
	}

	private static final class ExceptionWrapper {
		final Throwable ex;

		public ExceptionWrapper(Throwable ex) {
			this.ex = ex;
		}
	}
}
