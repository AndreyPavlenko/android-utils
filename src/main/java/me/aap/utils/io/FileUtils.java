package me.aap.utils.io;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;

import me.aap.utils.app.App;

/**
 * @author Andrey Pavlenko
 */
public class FileUtils {

	public static File getFileFromUri(Uri fileUri) {
		return getFileFromUri(App.get(), fileUri);
	}

	public static File getFileFromUri(Context ctx, Uri fileUri) {
		try (ParcelFileDescriptor pfd = ctx.getContentResolver().openFileDescriptor(fileUri, "r")) {
			return (pfd != null) ? getFileFromDescriptor(pfd) : null;
		} catch (Exception ex) {
			Log.d("Utils", "Failed to resolve real path: " + fileUri, ex);
		}

		return null;
	}

	public static File getFileFromDescriptor(ParcelFileDescriptor fd) throws Exception {
		String path = Os.readlink("/proc/self/fd/" + fd.getFd());
		File f = new File(path);
		if (f.isFile()) return f;

		if (path.startsWith("/mnt/media_rw/")) {
			f = new File("/storage" + path.substring(13));
			if (f.isFile()) return f;
		}

		return null;
	}

	public static String getFileExtension(String fileName) {
		if (fileName == null) return null;
		int idx = fileName.lastIndexOf('.');
		return ((idx == -1) || (idx == (fileName.length() - 1))) ? null : fileName.substring(idx + 1);
	}

	public static String getMimeType(String fileName) {
		return getMimeTypeFromExtension(getFileExtension(fileName));
	}

	public static String getMimeTypeFromExtension(String fileExt) {
		return (fileExt == null) ? null : MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExt);
	}
}
