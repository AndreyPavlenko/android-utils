package me.aap.utils.function;

import java.io.Closeable;

/**
 * @author Andrey Pavlenko
 */
public interface Cancellable extends Closeable {

	boolean cancel();

	@Override
	default void close() {
		cancel();
	}
}
