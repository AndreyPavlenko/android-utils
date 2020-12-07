package me.aap.utils.vfs;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.Cancellable;
import me.aap.utils.io.IoUtils;
import me.aap.utils.net.ByteBufferSupplier;

import static me.aap.utils.async.Completed.failed;

/**
 * @author Andrey Pavlenko
 */
public interface VirtualInputStream extends Cancellable {

	FutureSupplier<ByteBuffer> read(ByteBufferSupplier dst);

	default FutureSupplier<Long> available() {
		return Completed.completed(0L);
	}

	default FutureSupplier<Long> skip(long n) {
		return Completed.completed(0L);
	}

	static VirtualInputStream wrapInputStream(InputStream in, int bufferLen) {
		return new VirtualInputStream() {
			boolean canceled;

			@Override
			public FutureSupplier<ByteBuffer> read(ByteBufferSupplier dst) {
				return readInputStream(in, dst.getByteBuffer(), bufferLen);
			}

			@Override
			public boolean cancel() {
				if (canceled) return false;
				IoUtils.close(in);
				return canceled = true;
			}

			@Override
			public FutureSupplier<Long> available() {
				try {
					return Completed.completed((long) in.available());
				} catch (IOException ex) {
					return failed(ex);
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
				off = dst.arrayOffset();
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
				return (i != -1) ? (int) (b[0] & 0xFF) : -1;
			}

			@Override
			public int read(@NonNull byte[] b, int off, int len) throws IOException {
				try {
					ByteBuffer buf = VirtualInputStream.this.read(() -> ByteBuffer.wrap(b, off, len)).get();
					int remain = buf.remaining();
					return (remain == 0) ? -1 : remain;
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}

			@Override
			public int available() throws IOException {
				try {
					return VirtualInputStream.this.available().get().intValue();
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}

			@Override
			public long skip(long n) throws IOException {
				try {
					return VirtualInputStream.this.skip(n).get();
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}

			@Override
			public void close() {
				VirtualInputStream.this.close();
			}
		};
	}
}
