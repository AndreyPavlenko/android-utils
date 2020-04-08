package me.aap.utils.function;

/**
 * @author Andrey Pavlenko
 */
public interface CheckedSupplier<R, T extends Throwable> {
	R get() throws T;
}
