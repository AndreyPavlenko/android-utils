package me.aap.utils.net;

import java.nio.ByteBuffer;

/**
 * @author Andrey Pavlenko
 */
public interface ByteBufferSupplier {

	ByteBuffer getByteBuffer();

	default void retainByteBuffer(ByteBuffer bb) {
	}

	default void releaseByteBuffer(ByteBuffer bb) {
	}

	default void release() {
	}

	default ByteBufferArraySupplier asArray() {
		return () -> new ByteBuffer[]{getByteBuffer()};
	}
}
