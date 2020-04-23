package me.aap.utils.vfs;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.holder.BooleanHolder;
import me.aap.utils.io.IoUtils;

import static me.aap.utils.async.Completed.completed;
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

			FutureSupplier<Void> rw = is.read(buf).then(i -> {
				if (i == -1) {
					completed.set(true);
					return Completed.completedVoid();
				} else {
					buf.flip();
					return os.write(buf);
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
}
