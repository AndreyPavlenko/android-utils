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
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.BooleanSupplier;
import me.aap.utils.io.FileUtils;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.pref.PreferenceStore.Pref;
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
	private final boolean preferFiles;
	private final SharedPreferences uriToPathMap;

	ContentFileSystem(Provider provider, boolean preferFiles) {
		this.provider = provider;
		this.preferFiles = preferFiles;
		uriToPathMap = preferFiles ? App.get().getSharedPreferences("uri_to_path", MODE_PRIVATE) : null;
	}

	@NonNull
	@Override
	public Provider getProvider() {
		return provider;
	}


	@NonNull
	@Override
	public FutureSupplier<VirtualResource> getResource(Uri rootUri) {
		if (preferFiles) {
			String path = uriToPathMap.getString(rootUri.toString(), null);

			if (path != null) {
				VirtualResource local = LocalFileSystem.getInstance().getResource(path);
				if (local != null) return completed(local);
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
							VirtualResource local = LocalFileSystem.getInstance().getResource(path);
							if (local == null) return contentDir;
							uriToPathMap.edit().putString(rootUri.toString(), path).apply();
							return completed(local);
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
				return ContentFileSystem.this;
			}
		};
	}

	public static final class Provider implements VirtualFileSystem.Provider {
		public static final Pref<BooleanSupplier> PREFER_FILE_API = Pref.b("PREFER_FILE_API", true);
		private static final Set<String> schemes = Collections.singleton("content");
		private static final Provider instance = new Provider();

		private Provider() {
		}

		public static Provider getInstance() {
			return instance;
		}

		@NonNull
		@Override
		public Set<String> getSupportedSchemes() {
			return schemes;
		}

		@NonNull
		@Override
		public FutureSupplier<VirtualFileSystem> createFileSystem(PreferenceStore ps) {
			return completed(new ContentFileSystem(this, ps.getBooleanPref(PREFER_FILE_API)));
		}
	}
}
