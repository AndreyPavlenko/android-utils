package me.aap.utils.function;

/**
 * @author Andrey Pavlenko
 */
public interface Cancellable extends AutoCloseable {
	Cancellable DONE = () -> false;

	boolean cancel();

	@Override
	default void close() {
		cancel();
	}
}
