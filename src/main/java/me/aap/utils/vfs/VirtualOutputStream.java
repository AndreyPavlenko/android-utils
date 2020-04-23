package me.aap.utils.vfs;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.Cancellable;
import me.aap.utils.io.IoUtils;

import static me.aap.utils.async.Completed.failed;

/**
 * @author Andrey Pavlenko
 */
public interface VirtualOutputStream extends Cancellable {

	FutureSupplier<Void> write(ByteBuffer src);

	default void flush() throws IOException {
	}

	@Override
	default void close() {
		cancel();
	}

	static VirtualOutputStream wrapOutputStream(OutputStream out, int bufferLen) {
		return new VirtualOutputStream() {
			boolean canceled;

			@Override
			public FutureSupplier<Void> write(ByteBuffer src) {
				try {
					int len = src.remaining();

					if (src.hasArray()) {
						byte[] a = src.array();
						out.write(a, src.arrayOffset(), len);
						src.position(src.position() + len);
					} else {
						byte[] a = new byte[Math.min(len, bufferLen)];

						for (int l = a.length; l > 0; l = Math.min(src.remaining(), a.length)) {
							src.get(a, 0, l);
							out.write(a, 0, l);
						}
					}

					return Completed.completedNull();
				} catch (IOException ex) {
					return failed(ex);
				}
			}

			@Override
			public void flush() throws IOException {
				out.flush();
			}

			@Override
			public boolean cancel() {
				if (canceled) return false;
				IoUtils.close(out);
				return canceled = true;
			}
		};
	}

	default OutputStream asOutputStream() {
		return new OutputStream() {

			@Override
			public void write(int b) throws IOException {
				write(new byte[]{(byte) b});
			}

			@Override
			public void write(@NonNull byte[] b, int off, int len) throws IOException {
				try {
					ByteBuffer buf = ByteBuffer.wrap(b, off, len);
					while (buf.hasRemaining()) VirtualOutputStream.this.write(buf).get();
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}

			@Override
			public void flush() throws IOException {
				VirtualOutputStream.this.flush();
			}

			@Override
			public void close() {
				VirtualOutputStream.this.close();
			}
		};
	}
}
