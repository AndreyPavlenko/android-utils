package me.aap.utils.vfs.content;

import android.provider.DocumentsContract;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.vfs.VirtualFile;

/**
 * @author Andrey Pavlenko
 */
class ContentFile extends ContentResource implements VirtualFile {

	public ContentFile(ContentFolder parent, String name, String id) {
		super(parent, name, id);
	}

	@Override
	public FutureSupplier<Long> getLength() {
		return getLong(getUri(), DocumentsContract.Document.COLUMN_SIZE, 0);
	}
}
