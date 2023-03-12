package me.aap.utils.function;

import java.io.Closeable;

/**
 * @author Andrey Pavlenko
 */
public interface Cancellable extends Closeable {
	Cancellable CANCELED = () -> true;

	boolean cancel();

	@Override
	default void close() {
		cancel();
	}
}
