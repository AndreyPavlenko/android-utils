package me.aap.utils.concurrent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import static me.aap.utils.misc.Assert.assertSame;

/**
 * @author Andrey Pavlenko
 */
public class NetThread extends PooledThread {
	private ByteBuffer requestBuffer;
	private ByteBuffer responseBuffer;

	public NetThread() {
	}

	public NetThread(@Nullable Runnable target) {
		super(target);
	}

	public NetThread(@Nullable Runnable target, @NonNull String name) {
		super(target, name);
	}

	public ByteBuffer getRequestBuffer() {
		assertSame(this, Thread.currentThread());

		if (requestBuffer != null) {
			requestBuffer.clear();
			return requestBuffer;
		}

		return requestBuffer = createRequestBuffer();
	}

	public ByteBuffer getResponseBuffer() {
		assertSame(this, Thread.currentThread());

		if (responseBuffer != null) {
			responseBuffer.clear();
			return responseBuffer;
		}

		return responseBuffer = createResponseBuffer();
	}

	public void assertRequestBuffer(ByteBuffer bb) {
		assertSame(requestBuffer, bb);
	}

	public void assertResponseBuffer(ByteBuffer bb) {
		assertSame(requestBuffer, bb);
	}

	protected ByteBuffer createRequestBuffer() {
		return ByteBuffer.allocate(4096);
	}

	protected ByteBuffer createResponseBuffer() {
		return ByteBuffer.allocate(4096);
	}
}
