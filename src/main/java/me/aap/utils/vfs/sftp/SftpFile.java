package me.aap.utils.vfs.sftp;

import com.jcraft.jsch.SftpATTRS;

import java.io.InputStream;
import java.nio.ByteBuffer;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.ObjectPool.PooledObject;
import me.aap.utils.io.IoUtils;
import me.aap.utils.net.ByteBufferSupplier;
import me.aap.utils.vfs.VirtualFile;
import me.aap.utils.vfs.VirtualInputStream;
import me.aap.utils.vfs.sftp.SftpRoot.SftpSession;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.io.IoUtils.emptyByteBuffer;
import static me.aap.utils.vfs.VirtualInputStream.readInputStream;

/**
 * @author Andrey Pavlenko
 */
class SftpFile extends SftpResource implements VirtualFile {

	public SftpFile(SftpRoot root, String path) {
		super(root, path);
	}

	@Override
	public FutureSupplier<Long> getLength() {
		return lstat().map(SftpATTRS::getSize);
	}

	@Override
	public VirtualInputStream getInputStream(long offset) {
		return new VirtualInputStream() {
			private final FutureSupplier<PooledObject<SftpSession>> session = getRoot().getSession();
			long pos = offset;
			InputStream stream;

			@Override
			public FutureSupplier<ByteBuffer> read(ByteBufferSupplier dst) {
				InputStream in = stream;

				if (in != null) {
					FutureSupplier<ByteBuffer> r = readInputStream(in, dst.getByteBuffer(), getInputBufferLen());

					if (!r.isFailed()) {
						pos += r.peek().remaining();
						return r;
					}
				}

				return session.then(s -> {
					SftpSession session = s.get();
					if (session == null) return completed(emptyByteBuffer());

					InputStream is = stream = session.getChannel().get(getPath(), null, pos);
					FutureSupplier<ByteBuffer> r = readInputStream(is, dst.getByteBuffer(), getInputBufferLen());
					pos += r.peek().remaining();
					return r;
				});
			}

			@Override
			public boolean cancel() {
				InputStream in = stream;

				if (in != null) {
					IoUtils.close(in);
					stream = null;
				}

				if (session.cancel()) return true;

				PooledObject<SftpSession> s = session.peek();
				return (s != null) && s.release();
			}
		};
	}
}
