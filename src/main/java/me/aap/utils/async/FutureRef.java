package me.aap.utils.async;

import androidx.annotation.Keep;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import me.aap.utils.function.CheckedSupplier;

/**
 * @author Andrey Pavlenko
 */
public abstract class FutureRef<T> {
	@SuppressWarnings("rawtypes")
	private static final AtomicReferenceFieldUpdater<FutureRef, FutureSupplier> ref =
			AtomicReferenceFieldUpdater.newUpdater(FutureRef.class, FutureSupplier.class, "refHolder");
	@Keep
	@SuppressWarnings("unused")
	private volatile FutureSupplier<T> refHolder;

	protected abstract FutureSupplier<T> create() throws Throwable;

	public static <T> FutureRef<T> create(CheckedSupplier<FutureSupplier<T>, Throwable> supplier) {
		return new FutureRef<T>() {
			@Override
			protected FutureSupplier<T> create() throws Throwable {
				return supplier.get();
			}
		};
	}

	@SuppressWarnings("unchecked")
	public FutureSupplier<T> get() {
		FutureSupplier<T> r = ref.get(this);
		if ((r != null) && (!r.isDone() || isValid(r))) return r;

		Promise<T> p = new Promise<>();

		for (; !ref.compareAndSet(this, r, p); r = ref.get(this)) {
			if ((r != null) && (!r.isDone() || isValid(r))) return r;
		}

		try {
			create().thenReplaceOrClear(ref, this, p);
		} catch (Throwable ex) {
			p.completeExceptionally(ex);
			compareAndSet(p, null);
			return p;
		}

		r = ref.get(this);
		return (r != null) ? r : p;
	}

	public void set(FutureSupplier<T> r) {
		ref.set(this, r);
	}

	public boolean compareAndSet(FutureSupplier<T> expect, FutureSupplier<T> update) {
		return ref.compareAndSet(this, expect, update);
	}

	protected boolean isValid(FutureSupplier<T> ref) {
		return true;
	}
}
