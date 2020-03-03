package me.aap.utils.ui.view;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static me.aap.utils.collection.NaturalOrderComparator.compareNatural;

/**
 * @author Andrey Pavlenko
 */
public class FileListItem implements ListView.ListItem<FileListItem>, Comparable<FileListItem> {
	private final FileListView list;
	private final FileListItem parent;
	private final File file;

	public FileListItem(FileListView list, FileListItem parent, File file) {
		this.list = list;
		this.parent = parent;
		this.file = file;
	}

	public static FileListItem rootItem(FileListView list, Collection<File> files) {
		return new FileListItem(null, null, null) {
			final List<FileListItem> children = list.wrap(this, files);

			@NonNull
			@Override
			public List<FileListItem> getChildren() {
				return children;
			}

			@Nullable
			@Override
			public Drawable getIcon() {
				return null;
			}

			@Override
			public boolean hasChildren() {
				return true;
			}
		};
	}

	public FileListView getList() {
		return list;
	}

	public File getFile() {
		return file;
	}

	@Nullable
	@Override
	public Drawable getIcon() {
		return getFile().isDirectory() ? getList().getFolderIcon() : getList().getFileIcon();
	}

	@NonNull
	@Override
	public CharSequence getText() {
		String n = file.getName();
		return n.isEmpty() ? file.getPath() : n;
	}

	@Nullable
	@Override
	public FileListItem getParent() {
		return parent;
	}

	@NonNull
	@Override
	public List<FileListItem> getChildren() {
		return getList().wrap(this, getFile().listFiles());
	}

	@Override
	public boolean hasChildren() {
		return getFile().isDirectory();
	}

	@Override
	public int compareTo(FileListItem o) {
		File f1 = getFile();
		File f2 = o.getFile();

		if (f1.isDirectory()) {
			return (f2.isDirectory()) ? compareNatural(f1.getName(), f2.getName()) : -1;
		} else if (f2.isDirectory()) {
			return 1;
		} else {
			return compareNatural(f1.getName(), f2.getName());
		}
	}

	@NonNull
	@Override
	public String toString() {
		return (file == null) ? "" : file.getAbsolutePath();
	}
}