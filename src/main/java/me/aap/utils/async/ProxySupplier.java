package me.aap.utils.async;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.aap.utils.function.CheckedFunction;
import me.aap.utils.function.Function;

import static me.aap.utils.async.CompletableSupplier.Cancelled.CANCELLED;

/**
 * @author Andrey Pavlenko
 */
public abstract class ProxySupplier<C, S> extends CompletableSupplier<C, S> implements CompletableConsumer<C> {

	protected ProxySupplier() {
	}

	protected ProxySupplier(@Nullable FutureSupplier<? extends C> supplier) {
		if (supplier != null) supplier.addConsumer(this);
	}

	public abstract S map(C value) throws Throwable;

	public static <T> ProxySupplier<T, T> create() {
		return create((FutureSupplier<? extends T>) null);
	}

	public static <T> ProxySupplier<T, T> create(FutureSupplier<? extends T> supplier) {
		return new ProxySupplier<T, T>(supplier) {
			@Override
			public T map(T t) {
				return t;
			}
		};
	}

	public static <T, R> ProxySupplier<T, R> create(CheckedFunction<? super T, ? extends R, Throwable> map) {
		return create(null, map);
	}

	public static <T, R> ProxySupplier<T, R> create(@Nullable FutureSupplier<? extends T> supplier,
																									@NonNull CheckedFunction<? super T, ? extends R, Throwable> map) {
		return new ProxySupplier<T, R>(supplier) {
			@Override
			public R map(T t) throws Throwable {
				return map.apply(t);
			}
		};
	}

	public static <T, R> ProxySupplier<T, R> create(@Nullable FutureSupplier<? extends T> supplier,
																									@NonNull CheckedFunction<? super T, ? extends R, Throwable> map,
																									@NonNull Function<Throwable, ? extends T> onFail) {
		return new ProxySupplier<T, R>(supplier) {
			@Override
			public R map(T t) throws Throwable {
				return map.apply(t);
			}

			@Override
			public boolean completeExceptionally(@NonNull Throwable fail) {
				return complete(onFail.apply(fail));
			}

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return complete(onFail.apply(CANCELLED.fail));
			}
		};
	}
}
