package me.aap.utils.net;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.nio.ByteBuffer;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.ProgressiveResultConsumer.Completion;

/**
 * @author Andrey Pavlenko
 */
public interface NetChannel extends Closeable {

	NetHandler getHandler();

	FutureSupplier<ByteBuffer> read(ByteBufferSupplier buf, @Nullable Completion<ByteBuffer> consumer);

	default FutureSupplier<ByteBuffer> read(ByteBufferSupplier buf) {
		return read(buf, null);
	}

	default FutureSupplier<ByteBuffer> read(ByteBuffer buf) {
		return read(() -> buf);
	}

	default FutureSupplier<ByteBuffer> read() {
		return read(() -> ByteBuffer.allocate(4096));
	}

	FutureSupplier<Void> write(ByteBufferArraySupplier buf);

	default FutureSupplier<Void> write(ByteBufferSupplier buf) {
		return write(buf.asArray());
	}

	default FutureSupplier<Void> write(ByteBuffer buf) {
		return write(() -> new ByteBuffer[]{buf});
	}

	boolean isOpen();

	@Override
	void close();
}
