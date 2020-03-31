package me.aap.utils.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import me.aap.utils.R;
import java.util.function.Consumer;
import me.aap.utils.ui.UiUtils;
import me.aap.utils.ui.activity.ActivityDelegate;
import me.aap.utils.ui.view.FileListItem;
import me.aap.utils.ui.view.FileListView;
import me.aap.utils.ui.view.ImageButton;
import me.aap.utils.ui.view.ListView.OnItemClickListener;
import me.aap.utils.ui.view.NavBarView;
import me.aap.utils.ui.view.ToolBarView;

import static android.view.KeyEvent.KEYCODE_DPAD_CENTER;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_NUMPAD_ENTER;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.LEFT;
import static me.aap.utils.ui.UiUtils.ID_NULL;
import static me.aap.utils.ui.UiUtils.toPx;
import static me.aap.utils.ui.activity.ActivityListener.FRAGMENT_CONTENT_CHANGED;

/**
 * @author Andrey Pavlenko
 */
public class FilePickerFragment extends ActivityFragment implements OnItemClickListener<FileListItem> {
	public static final byte FILE = 1;
	public static final byte FOLDER = 2;
	public static final byte FILE_OR_FOLDER = FILE | FOLDER;
	private static FileListItem currentFolder;
	private NavBarView.Mediator navBarMediator;
	private Consumer<Uri> consumer;
	private byte mode;
	private Pattern pattern;
	private File selection;

	public void init(Consumer<Uri> consumer, byte mode) {
		init(consumer, mode, null);
	}

	public void init(Consumer<Uri> consumer, byte mode, Pattern pattern) {
		this.consumer = consumer;
		this.mode = mode;
		this.pattern = pattern;
		FileListView v = (FileListView) getView();
		if (v != null) init(v);
	}

	public void init(FileListView v) {
		if (currentFolder == null) currentFolder = FileListItem.rootItem(v, getRoots());
		v.setPattern(getPattern());
		v.setFoldersOnly(getMode() == FOLDER);
		setCurrentFolder(v, currentFolder);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return new FileListView(getContext(), null);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		FileListView v = (FileListView) view;
		ViewGroup.LayoutParams lp = v.getLayoutParams();
		lp.height = lp.width = MATCH_PARENT;
		v.setOnItemClickListener(this);
		v.setHasFixedSize(true);
		init(v);
	}

	@Override
	public int getFragmentId() {
		return R.id.file_picker;
	}

	@Override
	public ToolBarView.Mediator getToolBarMediator() {
		return ToolBarMediator.instance;
	}

	@Override
	public NavBarView.Mediator getNavBarMediator() {
		return navBarMediator;
	}

	@Override
	public void switchingFrom(@Nullable ActivityFragment currentFragment) {
		navBarMediator = (currentFragment == null) ? super.getNavBarMediator()
				: currentFragment.getNavBarMediator();
	}

	@Override
	public boolean isRootPage() {
		FileListView v = (FileListView) getView();
		return (v == null) || (v.getParentItem() == null);
	}

	@Override
	public boolean onBackPressed() {
		FileListView v = (FileListView) getView();
		if (v == null) return false;

		FileListItem p = v.getParentItem();

		if ((p != null) && ((p = p.getParent()) != null)) {
			setCurrentFolder(v, p);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean onListItemClick(FileListItem item) {
		FileListView v = (FileListView) getView();
		if (v == null) return false;

		File f = item.getFile();

		if (f.isFile()) {
			if ((getMode() & FILE_OR_FOLDER) != 0) {
				selection = f;
				pick();
			}
		} else {
			setCurrentFolder(v, item);
		}

		return true;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public byte getMode() {
		return mode;
	}

	public void setPath(CharSequence path) {
		FileListView v = (FileListView) getView();
		if (v == null) return;

		File f = new File(path.toString());

		if (f.isFile()) {
			f = f.getParentFile();
			if (f == null) return;
		}

		setCurrentFolder(v, new FileListItem(v, v.getParentItem(), f));
	}

	public CharSequence getPath() {
		return (currentFolder == null) ? "" : currentFolder.toString();
	}

	public void pick() {
		if (consumer == null) return;

		Uri uri = (selection == null) ? null : Uri.fromFile(selection);
		consumer.accept(uri);
		selection = null;
		consumer = null;
		pattern = null;
	}

	public boolean canPick() {
		return selection != null;
	}

	public void close() {
		selection = null;
		pick();
	}

	private void setCurrentFolder(FileListView v, FileListItem f) {
		currentFolder = f;
		selection = (getMode() == FILE) ? null : currentFolder.getFile();
		v.setItems(f);
		ActivityDelegate.get(v.getContext()).fireBroadcastEvent(FRAGMENT_CONTENT_CHANGED);
	}


	@SuppressWarnings("JavaReflectionMemberAccess")
	@SuppressLint({"DiscouragedPrivateApi", "SdCardPath"})
	public Collection<File> getRoots() {
		Context ctx = getContext();
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
					Log.e(getClass().getName(), "StorageVolume.getPathFile() failed", ex);
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

	interface ToolBarMediator extends ToolBarView.Mediator {
		ToolBarMediator instance = new ToolBarMediator() {
		};

		@Override
		default void enable(ToolBarView tb, ActivityFragment f) {
			FilePickerFragment p = (FilePickerFragment) f;

			EditText t = createPath(tb, p);
			t.setText(p.getPath());
			addView(tb, t, getPathId(), LEFT);

			ImageButton b = createBackButton(tb, p);
			addView(tb, b, getBackButtonId(), LEFT);
			b.setVisibility(getBackButtonVisibility(p));

			b = createOkButton(tb, p);
			addView(tb, b, getOkButtonId());
			b.setVisibility(getOkButtonVisibility(p));

			b = createCloseButton(tb, p);
			addView(tb, b, getCloseButtonId());
		}

		@Override
		default void onActivityEvent(ToolBarView tb, ActivityDelegate a, long e) {
			if (e == FRAGMENT_CONTENT_CHANGED) {
				FilePickerFragment p = (FilePickerFragment) a.getActiveFragment();
				if (p == null) return;

				EditText t = tb.findViewById(getPathId());
				t.setText(p.getPath());

				ImageButton b = tb.findViewById(getBackButtonId());
				b.setVisibility(getBackButtonVisibility(p));

				b = tb.findViewById(getOkButtonId());
				b.setVisibility(getOkButtonVisibility(p));
			}
		}

		default void onBackButtonClick(FilePickerFragment f) {
			f.getActivityDelegate().onBackPressed();
		}

		default void onOkButtonClick(FilePickerFragment f) {
			f.pick();
		}

		default void onCloseButtonClick(FilePickerFragment f) {
			f.close();
		}

		default boolean onPathKeyEvent(FilePickerFragment f, EditText text, int keyCode, KeyEvent event) {
			if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

			switch (keyCode) {
				case KEYCODE_DPAD_CENTER:
				case KEYCODE_ENTER:
				case KEYCODE_NUMPAD_ENTER:
					f.setPath(text.getText());
					return true;
				default:
					return UiUtils.dpadFocusHelper(text, keyCode, event);
			}
		}

		@IdRes
		default int getBackButtonId() {
			return R.id.tool_bar_back_button;
		}

		@DrawableRes
		default int getBackButtonIcon() {
			return R.drawable.back;
		}

		default ImageButton createBackButton(ToolBarView tb, FilePickerFragment f) {
			ImageButton b = new ImageButton(tb.getContext(), null, R.attr.toolbarStyle);
			initButton(b, getBackButtonIcon(), v -> onBackButtonClick(f));
			return b;
		}

		default int getBackButtonVisibility(FilePickerFragment f) {
			return f.getActivityDelegate().isRootPage() ? GONE : VISIBLE;
		}

		@IdRes
		default int getOkButtonId() {
			return R.id.file_picker_ok;
		}

		@DrawableRes
		default int getOkButtonIcon() {
			return R.drawable.check;
		}

		default ImageButton createOkButton(ToolBarView tb, FilePickerFragment f) {
			ImageButton b = new ImageButton(tb.getContext(), null, R.attr.toolbarStyle);
			initButton(b, getOkButtonIcon(), v -> onOkButtonClick(f));
			return b;
		}

		default int getOkButtonVisibility(FilePickerFragment f) {
			return f.canPick() ? VISIBLE : GONE;
		}

		@IdRes
		default int getCloseButtonId() {
			return R.id.file_picker_close;
		}

		@DrawableRes
		default int getCloseButtonIcon() {
			return R.drawable.close;
		}

		default ImageButton createCloseButton(ToolBarView tb, FilePickerFragment f) {
			ImageButton b = new ImageButton(tb.getContext(), null, R.attr.toolbarStyle);
			initButton(b, getCloseButtonIcon(), v -> onCloseButtonClick(f));
			return b;
		}

		@IdRes
		default int getPathId() {
			return R.id.file_picker_path;
		}

		default EditText createPath(ToolBarView tb, FilePickerFragment f) {
			Context ctx = tb.getContext();
			EditText t = new AppCompatEditText(ctx);
			ConstraintLayout.LayoutParams lp = setLayoutParams(t, 0, WRAP_CONTENT);
			t.setTextAppearance(getPathTextAppearance(f));
			t.setBackgroundResource(R.drawable.tool_bar_edittext_bg);
			t.setOnKeyListener((v, k, e) -> onPathKeyEvent(f, t, k, e));
			t.setMaxLines(1);
			t.setSingleLine(true);
			lp.horizontalWeight = 2;
			setPathPadding(t);
			return t;
		}

		@StyleRes
		default int getPathTextAppearance(FilePickerFragment f) {
			Context ctx = f.getContext();
			if (ctx == null) return ID_NULL;

			TypedArray ta = ctx.obtainStyledAttributes(null, new int[]{R.attr.textAppearanceBody1},
					R.attr.toolbarStyle, R.style.Theme_Utils_Base_ToolBarStyle);
			int style = ta.getResourceId(0, R.style.TextAppearance_MaterialComponents_Body1);
			ta.recycle();
			return style;
		}

		default void setPathPadding(EditText t) {
			int p = (int) toPx(t.getContext(), 2);
			t.setPadding(p, p, p, p);
		}
	}
}
