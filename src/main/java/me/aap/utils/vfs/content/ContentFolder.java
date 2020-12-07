package me.aap.utils.vfs.content;

import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import me.aap.utils.app.App;
import me.aap.utils.async.Async;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.vfs.VirtualFolder;
import me.aap.utils.vfs.VirtualResource;

import static android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME;
import static android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID;
import static android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE;
import static android.provider.DocumentsContract.Document.MIME_TYPE_DIR;
import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

/**
 * @author Andrey Pavlenko
 */
public class ContentFolder extends ContentResource implements VirtualFolder {
	private static final String[] queryFields = new String[]{COLUMN_DISPLAY_NAME, COLUMN_DOCUMENT_ID, COLUMN_MIME_TYPE};
	private volatile FutureSupplier<List<VirtualResource>> children;

	public ContentFolder(ContentFolder parent, String name, String id) {
		super(parent, name, id);
	}

	@Override
	public FutureSupplier<List<VirtualResource>> getChildren() {
		FutureSupplier<List<VirtualResource>> children = this.children;
		if (children != null) return children;

		return App.get().execute(() -> {
			Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(getRootUri(), getId());

			try (Cursor c = App.get().getContentResolver().query(childrenUri, queryFields, null, null, null)) {
				if ((c != null) && c.moveToNext()) {
					List<VirtualResource> list = new ArrayList<>(c.getCount());

					do {
						String name = c.getString(0);
						String id = c.getString(1);
						String mime = c.getString(2);

						if (isDir(mime)) {
							list.add(new ContentFolder(this, name, id));
						} else {
							list.add(new ContentFile(this, name, id));
						}
					} while (c.moveToNext());

					return list;
				}
			}

			return Collections.<VirtualResource>emptyList();
		}).onSuccess(list -> this.children = list.isEmpty() ? null : completed(list));
	}

	FutureSupplier<ContentFile> findAnyFile() {
		return getChildren().then(ls -> {
			if (ls.isEmpty()) return completedNull();

			for (VirtualResource f : ls) {
				if (!f.isFolder()) return completed((ContentFile) f);
			}

			Iterator<VirtualResource> it = ls.iterator();
			return Async.iterate(((ContentFolder) it.next()).findAnyFile(), find -> {
				ContentFile f = find.peek();
				return (f != null) || !it.hasNext() ? null : ((ContentFolder) it.next()).findAnyFile();
			});
		});
	}

	private static boolean isDir(String mime) {
		return MIME_TYPE_DIR.equals(mime) || "directory".equals(mime);
	}
}
