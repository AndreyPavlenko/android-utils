package me.aap.utils.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import me.aap.utils.R;

/**
 * @author Andrey Pavlenko
 */
public class FileListView extends ListView<FileListItem> {
	@Nullable
	private final Drawable fileIcon;
	@Nullable
	private final Drawable folderIcon;
	private Pattern pattern;
	private boolean foldersOnly;

	public FileListView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, R.attr.fileListViewStyle);
	}

	public FileListView(Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
		super(context, attrs, defStyleAttr, R.style.Theme_Utils_Base_FileListViewStyle);
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FileListView, defStyleAttr,
				R.style.Theme_Utils_Base_FileListViewStyle);
		fileIcon = ta.getDrawable(R.styleable.FileListView_fileIcon);
		folderIcon = ta.getDrawable(R.styleable.FileListView_folderIcon);
		ta.recycle();
	}

	@Nullable
	public Drawable getFileIcon() {
		return fileIcon;
	}

	@Nullable
	public Drawable getFolderIcon() {
		return folderIcon;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public boolean isFoldersOnly() {
		return foldersOnly;
	}

	public void setFoldersOnly(boolean foldersOnly) {
		this.foldersOnly = foldersOnly;
	}

	public List<FileListItem> wrap(FileListItem parent, File... files) {
		return (files == null) || (files.length == 0) ? Collections.emptyList()
				: wrap(parent, Arrays.asList(files));
	}

	public List<FileListItem> wrap(FileListItem parent, Collection<File> files) {
		Pattern p = getPattern();
		boolean foldersOnly = isFoldersOnly();
		List<FileListItem> items = new ArrayList<>(files.size());

		for (File f : files) {
			if (f.isDirectory()) {
				items.add(new FileListItem(this, parent, f));
			} else if (!foldersOnly && ((p == null) || (p.matcher(f.getName()).matches()))) {
				items.add(new FileListItem(this, parent, f));
			}
		}

		Collections.sort(items);
		return items;
	}
}
