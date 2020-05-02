package me.aap.utils.net;

import java.nio.ByteBuffer;

/**
 * @author Andrey Pavlenko
 */
public interface ByteBufferArraySupplier {

	ByteBuffer[] getByteBufferArray();

	default void retainByteBufferArray(ByteBuffer[] bb, int off) {
	}

	default void releaseByteBufferArray(ByteBuffer[] bb) {
	}

	default void release() {
	}
}
