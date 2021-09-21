package me.aap.utils.vfs.sftp;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.io.AsyncInputStream.readInputStream;
import static me.aap.utils.io.IoUtils.emptyByteBuffer;

import androidx.annotation.NonNull;

import com.jcraft.jsch.SftpATTRS;

import java.io.InputStream;
import java.nio.ByteBuffer;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.ObjectPool.PooledObject;
import me.aap.utils.io.AsyncInputStream;
import me.aap.utils.io.IoUtils;
import me.aap.utils.log.Log;
import me.aap.utils.net.ByteBufferSupplier;
import me.aap.utils.vfs.VirtualFile;
import me.aap.utils.vfs.VirtualFolder;
import me.aap.utils.vfs.sftp.SftpRoot.SftpSession;

/**
 * @author Andrey Pavlenko
 */
class SftpFile extends SftpResource implements VirtualFile {
	private FutureSupplier<Long> length;

	SftpFile(SftpRoot root, String path) {
		super(root, path);
	}

	SftpFile(@NonNull SftpRoot root, @NonNull String path, VirtualFolder parent) {
		super(root, path, parent);
	}

	@Override
	public FutureSupplier<Long> getLength() {
		if (length != null) return length;
		return length = lstat().map(SftpATTRS::getSize).onSuccess(len -> length = completed(len));
	}

	@Override
	public AsyncInputStream getInputStream(long offset) {
		return new AsyncInputStream() {
			private final FutureSupplier<PooledObject<SftpSession>> session = getRoot().getSession();
			long pos = offset;
			InputStream stream;

			@Override
			public FutureSupplier<ByteBuffer> read(ByteBufferSupplier dst) {
				InputStream in = stream;

				if (in != null) {
					ByteBuffer b = dst.getByteBuffer();
					FutureSupplier<ByteBuffer> r = readInputStream(in, b, b.remaining());
					if (!r.isFailed()) pos += r.getOrThrow().remaining();
					return r;
				}

				return session.then(s -> {
					SftpSession session = s.get();
					if (session == null) return completed(emptyByteBuffer());

					InputStream is = stream = session.getChannel().get(getPath(), null, pos);
					ByteBuffer b = dst.getByteBuffer();
					FutureSupplier<ByteBuffer> r = readInputStream(is, b, b.remaining());
					if (!r.isFailed()) pos += r.getOrThrow().remaining();
					return r;
				});
			}

			@Override
			public void close() {
				IoUtils.close(stream);
				session.cancel();
				PooledObject<SftpSession> s = session.peek();
				if (s != null) s.release();
			}
		};
	}

	@Override
	public boolean canDelete() {
		return true;
	}

	@NonNull
	@Override
	public FutureSupplier<Boolean> delete() {
		return getRoot().useChannel(ch -> {
			try {
				ch.rm(getPath());
				return true;
			} catch (Exception ex) {
				Log.e(ex, "Failed to delete file ", getPath());
				return false;
			}
		});
	}
}
