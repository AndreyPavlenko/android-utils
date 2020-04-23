package me.aap.utils.vfs.content;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import me.aap.utils.app.App;
import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.io.FileUtils;
import me.aap.utils.vfs.VirtualFileSystem;
import me.aap.utils.vfs.VirtualResource;
import me.aap.utils.vfs.local.LocalFileSystem;

import static android.content.Context.MODE_PRIVATE;
import static android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME;
import static me.aap.utils.async.Completed.completed;

/**
 * @author Andrey Pavlenko
 */
public class ContentFileSystem implements VirtualFileSystem {
	private final Provider provider;

	ContentFileSystem(Provider provider) {
		this.provider = provider;
	}

	@NonNull
	@Override
	public Provider getProvider() {
		return provider;
	}

	public static final class Provider implements VirtualFileSystem.Provider {
		private static final Set<String> schemes = Collections.singleton("content");
		private final boolean preferFiles;
		private final ContentFileSystem fs;
		private final SharedPreferences uriToPathMap;

		public Provider(boolean preferFiles) {
			this.preferFiles = preferFiles;
			fs = new ContentFileSystem(this);
			uriToPathMap = preferFiles ? App.get().getSharedPreferences("uri_to_path", MODE_PRIVATE) : null;
		}

		@NonNull
		@Override
		public Set<String> getSupportedSchemes() {
			return schemes;
		}

		@NonNull
		@Override
		public FutureSupplier<VirtualFileSystem> getFileSystem() {
			return completed(fs);
		}

		@NonNull
		@Override
		public FutureSupplier<VirtualResource> getResource(Uri rootUri) {
			if (preferFiles) {
				String path = uriToPathMap.getString(rootUri.toString(), null);

				if (path != null) {
					FutureSupplier<VirtualResource> local = LocalFileSystem.Provider.getInstance().getResource(path);
					if (!Completed.isCompletedNull(local)) return local;
				}
			}

			FutureSupplier<VirtualResource> contentDir = App.get().execute(() -> create(rootUri));
			if (!preferFiles) return contentDir;

			return contentDir.then(folder -> ((ContentFolder) folder).findAnyFile()
					.then(contentFile -> {
						if (contentFile == null) return contentDir;

						File f = FileUtils.getFileFromUri(contentFile.getUri());
						if (f == null) return contentDir;

						VirtualResource dir = contentDir.get(null);
						f = f.getParentFile();


						for (ContentFolder p = contentFile.getParentFolder(); (p != null) && (f != null);
								 p = p.getParentFolder(), f = f.getParentFile()) {
							if (p == dir) {
								String path = f.getAbsolutePath();
								FutureSupplier<VirtualResource> local = LocalFileSystem.Provider.getInstance().getResource(path);
								if (Completed.isCompletedNull(local)) return contentDir;
								uriToPathMap.edit().putString(rootUri.toString(), path).apply();
								return local;
							}
						}

						return contentDir;
					}));

		}

		private ContentFolder create(@NonNull Uri rootUri) {
			Uri uri = DocumentsContract.buildDocumentUriUsingTree(rootUri,
					DocumentsContract.getTreeDocumentId(rootUri));
			String name = null;
			String id = DocumentsContract.getTreeDocumentId(rootUri);

			try (Cursor c = App.get().getContentResolver().query(uri, new String[]{COLUMN_DISPLAY_NAME}, null, null, null)) {
				if ((c != null) && c.moveToNext()) name = c.getString(0);
			}

			return new ContentFolder(null, (name == null) ? uri.getLastPathSegment() : name, id) {
				@NonNull
				@Override
				public Uri getUri() {
					return rootUri;
				}

				@NonNull
				@Override
				Uri getRootUri() {
					return rootUri;
				}

				@NonNull
				@Override
				ContentFolder getRoot() {
					return this;
				}

				@NonNull
				@Override
				public VirtualFileSystem getVirtualFileSystem() {
					return fs;
				}
			};
		}
	}
}
