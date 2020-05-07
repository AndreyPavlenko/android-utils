package me.aap.utils.vfs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.aap.utils.async.Async;
import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.holder.BooleanHolder;
import me.aap.utils.holder.Holder;
import me.aap.utils.holder.LongHolder;
import me.aap.utils.io.IoUtils;
import me.aap.utils.net.ByteBufferSupplier;
import me.aap.utils.net.NetChannel;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedVoid;
import static me.aap.utils.async.Completed.failed;

/**
 * @author Andrey Pavlenko
 */
public interface VirtualFile extends VirtualResource {
	int DEFAULT_BUFFER_LEN = 8192;

	default boolean isFile() {
		return true;
	}

	@Override
	default boolean isFolder() {
		return false;
	}

	default FutureSupplier<Long> getLength() {
		return completed(0L);
	}

	@NonNull
	default FutureSupplier<Void> copyTo(VirtualFile to) {
		VirtualInputStream in = null;
		VirtualOutputStream out = null;

		try {
			VirtualInputStream is = in = getInputStream();
			VirtualOutputStream os = out = to.getOutputStream();
			BooleanHolder completed = new BooleanHolder();
			ByteBuffer buf = ByteBuffer.allocate(Math.min(getInputBufferLen(), to.getOutputBufferLen()));

			FutureSupplier<Void> rw = is.read(() -> buf).then(read -> {
				if (read.hasRemaining()) {
					buf.flip();
					return os.write(buf);
				} else {
					completed.set(true);
					return Completed.completedVoid();
				}
			});

			FutureSupplier<Void> copy = rw.thenIterate(r -> completed.get() ? null : rw);
			copy.onCompletion((r, f) -> IoUtils.close(is, os));
			return copy;
		} catch (Throwable ex) {
			IoUtils.close(in, out);
			return failed(ex);
		}
	}

	@NonNull
	default FutureSupplier<Boolean> moveTo(VirtualFile to) {
		return copyTo(to).then(v -> delete());
	}

	default FutureSupplier<Void> transferTo(NetChannel channel, long off, long len,
																					@Nullable ByteBufferSupplier header) {
		VirtualInputStream vis = null;

		try {
			VirtualInputStream in = vis = getInputStream(off);
			LongHolder pos = new LongHolder(off);
			Holder<ByteBufferSupplier> hdr = (header != null) ? new Holder<>(header) : null;

			return Async.iterate(() -> {
				long remaining = len - (pos.value - off);
				if (remaining <= 0) return null;

				return in.read(() -> allocateInputBuffer(remaining)).then(buf -> {
					int read = buf.remaining();

					if (read > 0) {
						pos.value += read;
						return channel.write(() -> {
							if ((hdr != null) && (hdr.value != null)) {
								ByteBuffer[] b = new ByteBuffer[]{hdr.value.getByteBuffer(), buf};
								hdr.value = null;
								return b;
							} else {
								return new ByteBuffer[]{buf};
							}
						});
					} else {
						pos.value = len + off;
						return completedVoid();
					}
				});
			}).thenRun(vis::close);
		} catch (Throwable ex) {
			IoUtils.close(vis);
			return failed(ex);
		}
	}

	default VirtualInputStream getInputStream() throws IOException {
		return getInputStream(0);
	}

	default VirtualInputStream getInputStream(long offset) throws IOException {
		throw new IOException();
	}

	default VirtualOutputStream getOutputStream() throws IOException {
		throw new IOException();
	}

	default int getInputBufferLen() {
		return DEFAULT_BUFFER_LEN;
	}

	default int getOutputBufferLen() {
		return DEFAULT_BUFFER_LEN;
	}

	default ByteBuffer allocateInputBuffer(long max) {
		return ByteBuffer.allocate((int) Math.min(getInputBufferLen(), max));
	}

	default ByteBuffer allocateOutputBuffer(long max) {
		return ByteBuffer.allocate((int) Math.min(getOutputBufferLen(), max));
	}
}
