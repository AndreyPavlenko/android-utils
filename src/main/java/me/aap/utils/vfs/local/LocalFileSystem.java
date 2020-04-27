package me.aap.utils.vfs.local;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.aap.utils.app.App;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.Supplier;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.vfs.VirtualFileSystem;
import me.aap.utils.vfs.VirtualFolder;
import me.aap.utils.vfs.VirtualResource;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedEmptyList;

public class LocalFileSystem implements VirtualFileSystem {
	private static final LocalFileSystem instance = new LocalFileSystem(LocalFileSystem::androidRoots);
	private final Supplier<Collection<File>> roots;

	LocalFileSystem(Supplier<Collection<File>> roots) {
		this.roots = roots;
	}

	public static LocalFileSystem getInstance() {
		return instance;
	}

	@NonNull
	@Override
	public Provider getProvider() {
		return Provider.getInstance();
	}

	@NonNull
	@Override
	public FutureSupplier<VirtualResource> getResource(Uri uri) {
		return completed(getResource(uri.getPath()));
	}

	public VirtualResource getResource(String path) {
		if (path == null) return null;
		File file = new File(path);
		if (file.isDirectory()) return new LocalFolder(file);
		if (file.isFile()) return new LocalFile(file);
		return null;
	}

	@NonNull
	@Override
	public FutureSupplier<List<VirtualFolder>> getRoots() {
		Collection<File> roots = this.roots.get();
		int size = roots.size();
		if (size == 0) return completedEmptyList();

		List<VirtualFolder> folders = new ArrayList<>(size);
		for (File f : roots) {
			folders.add(new LocalFolder(f, null));
		}
		return completed(folders);
	}

	@SuppressWarnings("JavaReflectionMemberAccess")
	@SuppressLint({"DiscouragedPrivateApi", "SdCardPath"})
	public static Collection<File> androidRoots() {
		Context ctx = App.get();
		if (ctx == null) return Collections.emptyList();

		Set<File> files = new HashSet<>();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			StorageManager sm = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
			addRoot(files, ctx.getDataDir());

			if (sm != null) {
				Class<?> c = StorageVolume.class;

				try {
					Method m = c.getDeclaredMethod("getPathFile");
					m.setAccessible(true);
					for (StorageVolume v : sm.getStorageVolumes()) files.add((File) m.invoke(v));
				} catch (Exception ex) {
					Log.e(LocalFileSystem.class.getName(), "StorageVolume.getPathFile() failed", ex);
				}
			}
		}

		File dir = new File("/");
		if (dir.canRead()) files.add(dir);
		if ((dir = new File("/mnt")).canRead()) files.add(dir);
		if ((dir = new File("/sdcard")).canRead()) files.add(dir);
		if ((dir = new File("/storage")).canRead()) files.add(dir);

		addRoot(files, ctx.getFilesDir());
		addRoot(files, ctx.getCacheDir());
		addRoot(files, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
		addRoot(files, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
		addRoot(files, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));

		addRoot(files, ctx.getObbDirs());
		addRoot(files, ctx.getExternalCacheDirs());
		addRoot(files, ctx.getExternalFilesDirs(null));
		addRoot(files, ctx.getExternalMediaDirs());

		return files;
	}

	private static void addRoot(Set<File> files, File... dirs) {
		if (dirs == null) return;
		for (File dir : dirs) addRoot(files, dir);
	}

	@SuppressWarnings("StatementWithEmptyBody")
	private static void addRoot(Set<File> files, File dir) {
		if (dir == null) return;
		for (File p = dir.getParentFile(); (p != null) && (p.isDirectory()) && p.canRead();
				 dir = p, p = dir.getParentFile()) {
		}
		if ((dir.isDirectory()) && dir.canRead()) files.add(dir);
	}

	public static final class Provider implements VirtualFileSystem.Provider {
		private final Set<String> schemes = Collections.singleton("file");
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
			return completed(LocalFileSystem.getInstance());
		}
	}
}