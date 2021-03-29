package me.aap.utils.vfs.content;

import android.provider.DocumentsContract;

import androidx.annotation.Keep;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import me.aap.utils.app.App;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.Promise;
import me.aap.utils.io.IoUtils;
import me.aap.utils.log.Log;
import me.aap.utils.vfs.VirtualFile;
import me.aap.utils.io.AsyncInputStream;

import static me.aap.utils.async.Completed.completed;

/**
 * @author Andrey Pavlenko
 */
class ContentFile extends ContentResource implements VirtualFile {
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static final AtomicReferenceFieldUpdater<ContentFile, FutureSupplier<Long>> LENGTH =
			(AtomicReferenceFieldUpdater) AtomicReferenceFieldUpdater.newUpdater(ContentFile.class, FutureSupplier.class, "length");
	@Keep
	@SuppressWarnings("unused")
	private volatile FutureSupplier<Long> length;

	public ContentFile(ContentFolder parent, String name, String id) {
		super(parent, name, id);
	}

	@Override
	public FutureSupplier<Long> getLength() {
		FutureSupplier<Long> length = LENGTH.get(this);
		if (length != null) return length;

		Promise<Long> p = new Promise<>();
		if (!LENGTH.compareAndSet(this, null, p)) return LENGTH.get(this);

		App.get().execute(() -> {
			long len = queryLong(getRid().toAndroidUri(), DocumentsContract.Document.COLUMN_SIZE, -1);

			if (len == -1) {
				try (InputStream in = App.get().getContentResolver().openInputStream(getRid().toAndroidUri())) {
					len = IoUtils.skip(in, Long.MAX_VALUE);
				} catch (IOException ex) {
					Log.d(ex);
				}
			}

			p.complete(len);
			LENGTH.set(this, completed(len));
			return len;
		});

		return p;
	}

	@Override
	public AsyncInputStream getInputStream(long offset) throws IOException {
		InputStream in = App.get().getContentResolver().openInputStream(getRid().toAndroidUri());
		if (in == null) throw new IOException("Resource not found: " + this);
		IoUtils.skip(in, offset);
		return AsyncInputStream.wrapInputStream(in, getInputBufferLen());
	}
}
