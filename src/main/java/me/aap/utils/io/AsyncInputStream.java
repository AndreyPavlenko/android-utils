package me.aap.utils.io;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.log.Log;
import me.aap.utils.net.ByteBufferSupplier;

import static me.aap.utils.async.Completed.failed;

/**
 * @author Andrey Pavlenko
 */
public interface AsyncInputStream extends Closeable {

	FutureSupplier<ByteBuffer> read(ByteBufferSupplier dst);

	default FutureSupplier<ByteBuffer> read() {
		return read(() -> ByteBuffer.allocate(8192));
	}

	default FutureSupplier<ByteBuffer> read(ByteBuffer dst) {
		return read(() -> dst);
	}

	default int available() {
		return 0;
	}

	default boolean hasRemaining() {
		return true;
	}

	default FutureSupplier<Long> skip(long n) {
		return Completed.completed(0L);
	}

	@Override
	void close();

	default boolean isAsync() {
		return true;
	}

	static AsyncInputStream wrapInputStream(InputStream in, int bufferLen) {
		return new AsyncInputStream() {

			@Override
			public boolean isAsync() {
				return false;
			}

			@Override
			public FutureSupplier<ByteBuffer> read(ByteBufferSupplier dst) {
				return readInputStream(in, dst.getByteBuffer(), bufferLen);
			}

			@Override
			public void close() {
				IoUtils.close(in);
			}

			@Override
			public int available() {
				try {
					return in.available();
				} catch (IOException ex) {
					Log.d(ex, "InputStream.available() failed");
					return 0;
				}
			}

			@Override
			public FutureSupplier<Long> skip(long n) {
				try {
					return Completed.completed(in.skip(n));
				} catch (IOException ex) {
					return failed(ex);
				}
			}
		};
	}

	static FutureSupplier<ByteBuffer> readInputStream(InputStream in, ByteBuffer dst, int bufferLen) {
		try {
			byte[] a;
			int off;
			int len;
			boolean hasArray = dst.hasArray();

			if (hasArray) {
				a = dst.array();
				off = dst.arrayOffset() + dst.position();
				len = dst.remaining();
			} else {
				a = new byte[Math.min(dst.remaining(), bufferLen)];
				off = 0;
				len = a.length;
			}

			int i = in.read(a, off, len);

			if (i > 0) {
				if (hasArray) {
					dst.limit(dst.position() + i);
				} else {
					int pos = dst.position();
					dst.put(a, 0, i).position(pos);
				}
			} else {
				dst.limit(dst.position());
			}

			return Completed.completed(dst);
		} catch (Throwable ex) {
			return failed(ex);
		}
	}

	default InputStream asInputStream() {
		return new InputStream() {

			@Override
			public int read() throws IOException {
				byte[] b = new byte[1];
				int i = read(b);
				return (i != -1) ? (b[0] & 0xFF) : -1;
			}

			@Override
			public int read(@NonNull byte[] b, int off, int len) throws IOException {
				try {
					ByteBuffer buf = AsyncInputStream.this.read(() -> ByteBuffer.wrap(b, off, len)).get();
					int remain = buf.remaining();
					return (remain == 0) ? -1 : remain;
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}

			@Override
			public int available() {
				return AsyncInputStream.this.available();
			}

			@Override
			public long skip(long n) throws IOException {
				try {
					return AsyncInputStream.this.skip(n).get();
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}

			@Override
			public void close() {
				AsyncInputStream.this.close();
			}
		};
	}
}
