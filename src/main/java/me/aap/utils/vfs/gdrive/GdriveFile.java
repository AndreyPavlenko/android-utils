package me.aap.utils.vfs.gdrive;

import com.google.api.client.http.HttpHeaders;
import com.google.api.services.drive.Drive;

import java.io.InputStream;
import java.nio.ByteBuffer;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.io.IoUtils;
import me.aap.utils.net.ByteBufferSupplier;
import me.aap.utils.vfs.VirtualFile;
import me.aap.utils.vfs.VirtualFolder;
import me.aap.utils.vfs.VirtualInputStream;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.vfs.VirtualInputStream.readInputStream;

/**
 * @author Andrey Pavlenko
 */
class GdriveFile extends GdriveResource implements VirtualFile {
	private FutureSupplier<Long> length;

	GdriveFile(GdriveFileSystem fs, String id, String name) {
		super(fs, id, name);
	}

	GdriveFile(GdriveFileSystem fs, String id, String name, VirtualFolder parent) {
		super(fs, id, name, parent);
	}

	@Override
	public FutureSupplier<Long> getLength() {
		if (length != null) return length;
		return length = fs.useDrive(d -> d.files().get(id).setFields("size").execute().getSize())
				.onSuccess(len -> length = completed(len));
	}

	@Override
	public VirtualInputStream getInputStream(long offset) {
		return new VirtualInputStream() {
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

				return fs.useDrive(d -> {
					Drive.Files.Get get = d.files().get(id);
					get.getMediaHttpDownloader().setDirectDownloadEnabled(true);

					if (pos > 0) {
						HttpHeaders h = new HttpHeaders();
						h.setRange("bytes=" + pos + "-");
						get.setRequestHeaders(h);
					}

					InputStream is = stream = get.executeMediaAsInputStream();
					ByteBuffer b = readInputStream(is, dst.getByteBuffer(), getInputBufferLen()).peek();
					pos += b.remaining();
					return b;
				});
			}

			@Override
			public boolean cancel() {
				InputStream in = stream;

				if (in != null) {
					IoUtils.close(in);
					return true;
				} else {
					return false;
				}
			}
		};
	}
}
