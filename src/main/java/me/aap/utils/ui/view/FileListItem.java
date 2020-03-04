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

			@Override
			public boolean isRoot() {
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
		if ((parent == null) || parent.isRoot()) return file.getAbsolutePath();

		String n = file.getName();
		return n.isEmpty() ? file.getAbsolutePath() : n;
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

	public boolean isRoot() {
		return false;
	}

	@Override
	public int compareTo(@NonNull FileListItem o) {
		if (hasChildren()) {
			return (o.hasChildren()) ? compareNatural(getText(), o.getText()) : -1;
		} else if (o.hasChildren()) {
			return 1;
		} else {
			return compareNatural(getText(), o.getText());
		}
	}

	@NonNull
	@Override
	public String toString() {
		return (file == null) ? "" : file.getAbsolutePath();
	}
}