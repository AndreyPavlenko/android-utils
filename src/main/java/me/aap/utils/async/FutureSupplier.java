package me.aap.utils.async;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import me.aap.utils.app.App;
import me.aap.utils.concurrent.HandlerExecutor;
import me.aap.utils.function.Cancellable;
import me.aap.utils.function.CheckedFunction;
import me.aap.utils.function.CheckedSupplier;
import me.aap.utils.function.Function;
import me.aap.utils.function.ProgressiveResultConsumer;
import me.aap.utils.function.Supplier;
import me.aap.utils.log.Log;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.failed;
import static me.aap.utils.concurrent.ConcurrentUtils.isMainThread;
import static me.aap.utils.function.ResultConsumer.Cancel.isCancellation;

/**
 * @author Andrey Pavlenko
 */
public interface FutureSupplier<T> extends Future<T>, CheckedSupplier<T, Throwable>, Cancellable {

	FutureSupplier<T> addConsumer(@NonNull ProgressiveResultConsumer<? super T> consumer);

	default FutureSupplier<T> onCompletion(@NonNull ProgressiveResultConsumer.Completion<? super T> consumer) {
		return addConsumer(consumer);
	}

	default FutureSupplier<T> onSuccess(@NonNull ProgressiveResultConsumer.Success<? super T> consumer) {
		return addConsumer(consumer);
	}

	default FutureSupplier<T> onFailure(@NonNull ProgressiveResultConsumer.Failure<? super T> consumer) {
		return addConsumer(consumer);
	}

	default FutureSupplier<T> onCancel(@NonNull ProgressiveResultConsumer.Cancel<? super T> consumer) {
		return addConsumer(consumer);
	}

	default FutureSupplier<T> onProgress(@NonNull ProgressiveResultConsumer.Progress<? super T> consumer) {
		return addConsumer(consumer);
	}

	@Nullable
	Throwable getFailure();

	/**
	 * Returns true if completed exceptionally or cancelled
	 */
	default boolean isFailed() {
		return getFailure() != null;
	}

	@Nullable
	default Executor getExecutor() {
		return null;
	}

	default FutureSupplier<T> withExecutor(Executor executor) {
		return withExecutor(executor, true);
	}

	default FutureSupplier<T> withExecutor(Executor executor, boolean ignoreIfDone) {
		if ((executor == getExecutor()) || (ignoreIfDone && isDone())) return this;

		ProxySupplier<T, T> p = new ProxySupplier<T, T>() {
			private final Executor exec;
			private volatile boolean cancelled;

			{
				if (executor instanceof HandlerExecutor) {
					HandlerExecutor handler = (HandlerExecutor) executor;
					exec = task -> {
						if (handler.getLooper().isCurrentThread()) {
							task.run();
						} else {
							boolean canceled = isCancelled();
							handler.post(() -> {
								if (canceled || !isCancelled()) task.run();
							});
						}
					};
				} else {
					exec = task -> {
						boolean canceled = isCancelled();
						executor.execute(() -> {
							if (canceled || !isCancelled()) task.run();
						});
					};
				}
			}

			@Nullable
			@Override
			public Executor getExecutor() {
				return exec;
			}

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				cancelled = true;
				if (!super.cancel(mayInterruptIfRunning)) return false;
				FutureSupplier.this.cancel();
				return true;
			}

			@Override
			public boolean isCancelled() {
				return cancelled;
			}

			@Override
			public T map(T t) {
				return t;
			}
		};

		addConsumer(p);
		return p;
	}

	default FutureSupplier<T> withMainHandler() {
		if (isDone() && isMainThread()) return this;
		return withExecutor(App.get().getHandler(), false);
	}

	default T get(@Nullable Supplier<? extends T> onError) {
		try {
			return get();
		} catch (Throwable ex) {
			Log.e(ex);
			return (onError != null) ? onError.get() : null;
		}
	}

	default T get(@Nullable Supplier<? extends T> onError, long timeout, @NonNull TimeUnit unit) {
		try {
			return get(timeout, unit);
		} catch (Throwable ex) {
			Log.e(ex);
			return (onError != null) ? onError.get() : null;
		}
	}

	default T peek() {
		return peek((Supplier<? extends T>) null);
	}

	default T peek(@Nullable T ifNotDone) {
		return peek(() -> ifNotDone);
	}

	default T peek(@Nullable Supplier<? extends T> ifNotDone) {
		if (isDone() && !isFailed()) {
			return getOrThrow();
		} else {
			return (ifNotDone != null) ? ifNotDone.get() : null;
		}
	}


	default T getOrThrow() throws RuntimeException {
		return getOrThrow(RuntimeException::new);
	}

	default <E extends Throwable> T getOrThrow(Function<Throwable, E> map) throws E {
		try {
			return get();
		} catch (Throwable ex) {
			throw map.apply(ex);
		}
	}

	@Override
	default boolean cancel() {
		return cancel(true);
	}

	@SuppressWarnings("unchecked")
	default <R> FutureSupplier<R> map(CheckedFunction<? super T, ? extends R, Throwable> map) {
		if (isDone()) {
			if (isFailed()) return (FutureSupplier<R>) this;

			try {
				return completed(map.apply(get()));
			} catch (Throwable ex) {
				Log.e(ex);
				return failed(ex);
			}
		}

		return ProxySupplier.create(this, map);
	}

	default FutureSupplier<T> ifFail(Function<Throwable, ? extends T> onFail) {
		if (isDone()) {
			if (isFailed()) return completed(onFail.apply(getFailure()));
			else return this;
		}

		return ProxySupplier.create(this, t -> t, onFail);
	}

	@SuppressWarnings("unchecked")
	default <R> FutureSupplier<R> then(CheckedFunction<? super T, FutureSupplier<R>, Throwable> then) {
		if (isDone()) {
			if (isFailed()) return (FutureSupplier<R>) this;

			try {
				return then.apply(get());
			} catch (Throwable ex) {
				Log.e(ex);
				return failed(ex);
			}
		}

		Promise<R> p = new Promise<>();

		onCompletion((result, fail) -> {
			if (fail == null) {
				try {
					then.apply(result).onCompletion(p::complete);
				} catch (Throwable ex) {
					p.completeExceptionally(ex);
				}
			} else if (isCancellation(fail)) {
				p.cancel();
			} else {
				p.completeExceptionally(fail);
			}
		});

		return p;
	}

	@SuppressWarnings("unchecked")
	default <R> FutureSupplier<R> closeableMap(CheckedFunction<? super T, ? extends R, Throwable> map) {
		if (isDone()) {
			if (isFailed()) return (FutureSupplier<R>) this;

			try (AutoCloseable closeable = (AutoCloseable) get()) {
				return completed(map.apply((T) closeable));
			} catch (Throwable ex) {
				Log.e(ex);
				return failed(ex);
			}
		}

		return ProxySupplier.create(this, result -> {
			try (@SuppressWarnings("unused") AutoCloseable closeable = (AutoCloseable) result) {
				return map.apply(result);
			}
		});
	}

	@SuppressWarnings("unchecked")
	default <R> FutureSupplier<R> closeableThen(CheckedFunction<? super T, FutureSupplier<R>, Throwable> then) {
		if (isDone()) {
			if (isFailed()) return (FutureSupplier<R>) this;

			try (AutoCloseable closeable = (AutoCloseable) get()) {
				return then.apply((T) closeable);
			} catch (Throwable ex) {
				Log.e(ex);
				return failed(ex);
			}
		}

		Promise<R> p = new Promise<>();

		onCompletion((result, fail) -> {
			if (fail == null) {
				try (@SuppressWarnings("unused") AutoCloseable closeable = (AutoCloseable) result) {
					then.apply(result).onCompletion(p::complete);
				} catch (Throwable ex) {
					p.completeExceptionally(ex);
				}
			} else if (isCancellation(fail)) {
				p.cancel();
			} else {
				p.completeExceptionally(fail);
			}
		});

		return p;
	}

	default FutureSupplier<T> thenIterate(CheckedFunction<FutureSupplier<T>, FutureSupplier<T>, Throwable> next) {
		return Async.iterate(this, next);
	}

	default FutureSupplier<T> thenIterate(FutureSupplier<T>... next) {
		return Async.iterate(this, next);
	}

	default FutureSupplier<T> thenIterate(Iterable<FutureSupplier<T>> next) {
		return Async.iterate(this, next);
	}

	default FutureSupplier<T> thenIterate(Iterator<FutureSupplier<T>> next) {
		return Async.iterate(this, next);
	}

	default FutureSupplier<T> thenIterate(CheckedSupplier<FutureSupplier<T>, Throwable> next) {
		return Async.iterate(this, next);
	}

	default FutureSupplier<T> thenComplete(Completable<T> complete) {
		return onCompletion(complete::complete);
	}

	default FutureSupplier<T> thenComplete(Completable<T>... complete) {
		return onCompletion(((result, fail) -> {
			for (Completable<T> c : complete) {
				c.complete(result, fail);
			}
		}));
	}

	default FutureSupplier<T> thenRun(Runnable... run) {
		return onCompletion(((result, fail) -> {
			for (Runnable r : run) {
				r.run();
			}
		}));
	}

	@SuppressWarnings("rawtypes")
	default FutureSupplier<T> thenReplace(AtomicReferenceFieldUpdater updater, Object owner) {
		return thenReplace(updater, owner, this);
	}

	@SuppressWarnings("rawtypes")
	default FutureSupplier<T> thenReplace(AtomicReferenceFieldUpdater updater, Object owner, Object expect) {
		return thenReplace(updater, owner, expect, Completed::completed);
	}

	@SuppressWarnings("rawtypes")
	default FutureSupplier<T> thenReplaceOrClear(AtomicReferenceFieldUpdater updater, Object owner) {
		return thenReplace(updater, owner, this, Completed::completedOrNull);
	}

	@SuppressWarnings("rawtypes")
	default FutureSupplier<T> thenReplaceOrClear(AtomicReferenceFieldUpdater updater, Object owner, Object expect) {
		return thenReplace(updater, owner, expect, Completed::completedOrNull);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	default FutureSupplier<T> thenReplace(AtomicReferenceFieldUpdater updater, Object owner, Object expect,
																				Function<FutureSupplier<T>, FutureSupplier<T>> replaceWith) {
		if (isDone()) {
			FutureSupplier<T> replacement = replaceWith.apply(this);
			updater.compareAndSet(owner, expect, replacement);
			if (expect instanceof Completable) ((Completable) expect).completeAs(this);
			return replacement;
		}

		return onCompletion((result, fail) -> {
			FutureSupplier<T> replacement = replaceWith.apply(this);
			updater.compareAndSet(owner, expect, replacement);
			if (expect instanceof Completable) ((Completable) expect).complete(result, fail);
		});
	}
}
