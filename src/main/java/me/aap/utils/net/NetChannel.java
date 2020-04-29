package me.aap.utils.net;

import java.io.Closeable;
import java.nio.ByteBuffer;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.Supplier;

/**
 * @author Andrey Pavlenko
 */
public interface NetChannel extends Closeable {

	FutureSupplier<ByteBuffer> read(Supplier<ByteBuffer> buf);

	default FutureSupplier<ByteBuffer> read(ByteBuffer buf) {
		return read(() -> buf);
	}

	default FutureSupplier<ByteBuffer> read() {
		return read(() -> ByteBuffer.allocate(4096));
	}

	FutureSupplier<Void> write(Supplier<ByteBuffer> buf);

	default FutureSupplier<Void> write(ByteBuffer buf) {
		return write(() -> buf);
	}

	@Override
	void close();
}
