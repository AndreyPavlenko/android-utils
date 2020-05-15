package me.aap.utils.vfs.content;

import android.provider.DocumentsContract;

import java.io.IOException;
import java.io.InputStream;

import me.aap.utils.app.App;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.vfs.VirtualFile;
import me.aap.utils.vfs.VirtualInputStream;

/**
 * @author Andrey Pavlenko
 */
class ContentFile extends ContentResource implements VirtualFile {

	public ContentFile(ContentFolder parent, String name, String id) {
		super(parent, name, id);
	}

	@Override
	public FutureSupplier<Long> getLength() {
		return getLong(getRid().toAndroidUri(), DocumentsContract.Document.COLUMN_SIZE, 0);
	}

	@Override
	public VirtualInputStream getInputStream(long offset) throws IOException {
		InputStream in = App.get().getContentResolver().openInputStream(getRid().toAndroidUri());
		if (in == null) throw new IOException("Resource not found: " + this);
		//noinspection StatementWithEmptyBody
		for (long p = 0; p < offset; p += in.skip(offset - p)) ;
		return VirtualInputStream.wrapInputStream(in, getInputBufferLen());
	}
}
