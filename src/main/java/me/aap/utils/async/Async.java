package me.aap.utils.async;

import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import me.aap.utils.function.CheckedFunction;
import me.aap.utils.function.CheckedSupplier;

import static me.aap.utils.async.Completed.completedVoid;
import static me.aap.utils.async.Completed.failed;

/**
 * @author Andrey Pavlenko
 */
public class Async {

	public static <T> FutureSupplier<Void> forEach(CheckedFunction<T, FutureSupplier<?>, Throwable> apply, Iterable<T> it) {
		return forEach(apply, it.iterator());
	}

	@SuppressWarnings("unchecked")
	public static <T> FutureSupplier<Void> forEach(CheckedFunction<T, FutureSupplier<?>, Throwable> apply, Iterator<T> it) {
		try {
			while (it.hasNext()) {
				FutureSupplier<?> s = apply.apply(it.next());
				if (s == null) break;

				if (s.isDone()) {
					if (s.isFailed()) return (FutureSupplier<Void>) s;
				} else {
					return new AsyncIterator.Void(s, c -> it.hasNext() ? apply.apply(it.next()) : null);
				}
			}
		} catch (Throwable ex) {
			return failed(ex);
		}

		return completedVoid();
	}

	@SuppressWarnings("unchecked")
	public static <T> FutureSupplier<Void> forEach(CheckedFunction<T, FutureSupplier<?>, Throwable> apply, T... items) {
		try {
			for (int i = 0; i < items.length; i++) {
				FutureSupplier<?> s = apply.apply(items[i]);
				if (s == null) break;

				if (s.isDone()) {
					if (s.isFailed()) return (FutureSupplier<Void>) s;
				} else {
					Iterator<T> it = (i == (items.length - 1)) ? Collections.emptyIterator() :
							Arrays.asList(items).subList(i + 1, items.length).iterator();
					return new AsyncIterator.Void(s, c -> it.hasNext() ? apply.apply(it.next()) : null);
				}
			}
		} catch (Throwable ex) {
			return failed(ex);
		}

		return completedVoid();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static FutureSupplier<Void> all(FutureSupplier<?> first, FutureSupplier<?>... next) {
		return iterate((FutureSupplier) first, Arrays.asList((FutureSupplier[]) next));
	}

	@SuppressWarnings("unchecked")
	public static <T> FutureSupplier<T> iterate(FutureSupplier<T> first, FutureSupplier<T>... next) {
		return iterate(first, Arrays.asList(next));
	}

	public static <T> FutureSupplier<T> iterate(FutureSupplier<T> first, Iterable<FutureSupplier<T>> next) {
		return iterate(first, next.iterator());
	}

	public static <T> FutureSupplier<T> iterate(FutureSupplier<T> first, Iterator<FutureSupplier<T>> next) {
		return iterate(first, c -> next.hasNext() ? next.next() : null);
	}

	public static <T> FutureSupplier<T> iterate(FutureSupplier<T> first, CheckedSupplier<FutureSupplier<T>, Throwable> next) {
		return iterate(first, c -> next.get());
	}

	public static <T> FutureSupplier<T> iterate(Iterable<FutureSupplier<T>> futures) {
		return iterate(futures.iterator());
	}

	public static <T> FutureSupplier<T> iterate(Iterator<FutureSupplier<T>> iterator) {
		return iterate(iterator.next(), iterator);
	}

	public static <T> FutureSupplier<T> iterate(CheckedSupplier<FutureSupplier<T>, Throwable> supplier) {
		try {
			return iterate(supplier.get(), supplier);
		} catch (Throwable ex) {
			return failed(ex);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <T> FutureSupplier<T> iterate(FutureSupplier<T> first, CheckedFunction<FutureSupplier<T>, FutureSupplier<T>, Throwable> next) {
		try {
			for (FutureSupplier<T> s = first; ; ) {
				if (s.isDone()) {
					if (s.isFailed()) {
						return s;
					} else {
						FutureSupplier<T> n = next.apply(s);
						if (n == null) return s;
						s = n;
					}
				} else {
					return new AsyncIterator(s, next);
				}
			}
		} catch (Throwable ex) {
			return failed(ex);
		}
	}

	public static <T> FutureSupplier<T> retry(CheckedSupplier<FutureSupplier<T>, Throwable> task) {
		FutureSupplier<T> s;

		for (int i = 0; ; i++) {
			try {
				s = task.get();
			} catch (Throwable ex) {
				if (i == 1) return failed(ex);
				Log.d(Async.class.getName(), "Task failed, retrying ...", ex);
				continue;
			}

			if (i == 1) return s;

			if (s.isDone()) {
				if (!s.isFailed() || s.isCancelled()) return s;
			} else {
				break;
			}
		}

		Promise<T> p = new Promise<>();

		s.onCompletion((result, fail) -> {
			if (fail == null) {
				p.complete(result);
			} else {
				try {
					task.get().onCompletion(p::complete);
				} catch (Throwable ex) {
					p.completeExceptionally(ex);
				}
			}
		});

		return p;
	}
}
