package me.aap.utils.ui.fragment;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.List;
import java.util.regex.Pattern;

import me.aap.utils.R;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.Consumer;
import me.aap.utils.holder.BiHolder;
import me.aap.utils.ui.UiUtils;
import me.aap.utils.ui.activity.ActivityDelegate;
import me.aap.utils.ui.view.ListView;
import me.aap.utils.ui.view.ToolBarView;
import me.aap.utils.ui.view.VirtualResourceAdapter;
import me.aap.utils.vfs.VirtualFileSystem;
import me.aap.utils.vfs.VirtualFolder;
import me.aap.utils.vfs.VirtualResource;

import static android.view.KeyEvent.KEYCODE_DPAD_CENTER;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_NUMPAD_ENTER;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.LEFT;
import static java.util.Objects.requireNonNull;
import static me.aap.utils.ui.UiUtils.ID_NULL;
import static me.aap.utils.ui.UiUtils.toPx;
import static me.aap.utils.ui.activity.ActivityListener.FRAGMENT_CONTENT_CHANGED;

/**
 * @author Andrey Pavlenko
 */
public class FilePickerFragment extends GenericDialogFragment implements
		ListView.ItemClickListener<VirtualResource>, ListView.ItemsChangeListener<VirtualResource> {
	public static final byte FILE = 1;
	public static final byte FOLDER = 2;
	public static final byte FILE_OR_FOLDER = FILE | FOLDER;
	private Consumer<? super VirtualResource> consumer;
	private FutureSupplier<BiHolder<? extends VirtualResource, List<? extends VirtualResource>>> supplier;
	private byte mode = FILE_OR_FOLDER;
	private Pattern pattern;

	public FilePickerFragment() {
		super(ToolBarMediator.instance);
	}

	@Override
	public int getFragmentId() {
		return R.id.file_picker;
	}

	public void setFileSystem(VirtualFileSystem fs) {
		setSupplier(fs.getRoots().map(r -> new BiHolder<>(null, r)));
	}

	public void setFolder(VirtualFolder folder) {
		setSupplier(folder.getChildren().map(c -> new BiHolder<>(folder, c)));
	}

	public void setSupplier(FutureSupplier<BiHolder<? extends VirtualResource, List<? extends VirtualResource>>> supplier) {
		this.supplier = supplier;
		ListView<VirtualResource> v = getListView();
		if (v != null) v.setItems(supplier);
	}

	public void setConsumer(Consumer<VirtualResource> consumer) {
		this.consumer = consumer;
	}

	public void setMode(byte mode) {
		this.mode = mode;
	}

	public void setPattern(@Nullable Pattern pattern) {
		this.pattern = pattern;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		ListView<VirtualResource> v = new ListView<>(getContext(), null, 0, R.style.Theme_Utils_Base_FileListViewStyle);
		v.setItemAdapter(new VirtualResourceAdapter(getContext(), null));
		return v;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ListView<VirtualResource> v = getListView();
		if (v == null) return;
		ViewGroup.LayoutParams lp = v.getLayoutParams();
		lp.height = lp.width = MATCH_PARENT;
		v.setHasFixedSize(true);
		v.setItemClickListener(this);
		v.setItemsChangeListener(this);

		if ((pattern != null) || (mode != FILE_OR_FOLDER)) {
			v.setFilter(this::filter);
		} else {
			v.setFilter(null);
		}

		v.setItems(requireNonNull(supplier));
	}

	private boolean filter(VirtualResource f) {
		if (mode == FILE) {
			if (!f.isFile()) return false;
		} else if (mode == FOLDER) {
			if (!f.isFolder()) return false;
		}
		return (pattern == null) || pattern.matcher(f.getName()).matches();
	}

	@Override
	public ToolBarView.Mediator getToolBarMediator() {
		return ToolBarMediator.instance;
	}

	@Override
	public boolean isRootPage() {
		ListView<VirtualResource> v = getListView();
		return (v == null) || (v.getParentItem() == null);
	}

	@Override
	public boolean onBackPressed() {
		ListView<VirtualResource> v = getListView();
		if ((v == null) || (v.getParentItem() == null)) return false;

		v.setParentItems();
		return true;
	}

	@Override
	public boolean onListItemClick(VirtualResource item) {
		ListView<VirtualResource> v = getListView();
		if (v == null) return false;

		if (item.isFile()) {
			if ((mode & FILE) != 0) pick(item);
			return true;
		}

		return false;
	}

	@Override
	public void onListItemsChange(@Nullable VirtualResource parent, @NonNull List<? extends VirtualResource> items) {
		getActivityDelegate().fireBroadcastEvent(FRAGMENT_CONTENT_CHANGED);
		if (parent == null) return;
		if (parent.isFile()) onListItemClick(parent);
	}

	public void setPath(String path) {
		ListView<VirtualResource> v = getListView();
		if (v == null) return;
		v.setItems(path);
	}

	public CharSequence getPath() {
		ListView<VirtualResource> v = getListView();
		if (v == null) return "";
		VirtualResource p = v.getParentItem();
		return (p == null) ? "" : p.getUri().toString();
	}

	protected void onOkButtonClick() {
		ListView<VirtualResource> v = getListView();
		if (v != null) pick(v.getParentItem());
	}

	protected void onCloseButtonClick() {
		pick(null);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private ListView<VirtualResource> getListView() {
		return (ListView<VirtualResource>) getView();
	}

	private void pick(VirtualResource v) {
		Consumer<? super VirtualResource> c = consumer;
		ListView<VirtualResource> view = getListView();
		pattern = null;
		supplier = null;
		consumer = null;
		mode = FILE_OR_FOLDER;
		if (view != null) view.cleanUp();
		if (c != null) c.accept(v);
	}

	protected int getOkButtonVisibility() {
		ListView<VirtualResource> v = getListView();
		if (v == null) return GONE;
		if (v.getParentItem() == null) return GONE;
		return ((mode & FOLDER) != 0) ? VISIBLE : GONE;
	}

	interface ToolBarMediator extends GenericDialogFragment.ToolBarMediator {
		ToolBarMediator instance = new ToolBarMediator() {
		};

		@Override
		default void enable(ToolBarView tb, ActivityFragment f) {
			FilePickerFragment p = (FilePickerFragment) f;

			EditText t = createPath(tb, p);
			t.setText(p.getPath());
			addView(tb, t, getPathId(), LEFT);

			GenericDialogFragment.ToolBarMediator.super.enable(tb, f);
		}

		@Override
		default void onActivityEvent(ToolBarView tb, ActivityDelegate a, long e) {
			if (e == FRAGMENT_CONTENT_CHANGED) {
				FilePickerFragment p = (FilePickerFragment) a.getActiveFragment();
				if (p == null) return;

				EditText t = tb.findViewById(getPathId());
				t.setText(p.getPath());
				GenericDialogFragment.ToolBarMediator.super.onActivityEvent(tb, a, e);
			}
		}

		default boolean onPathKeyEvent(FilePickerFragment f, EditText text, int keyCode, KeyEvent event) {
			if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

			switch (keyCode) {
				case KEYCODE_DPAD_CENTER:
				case KEYCODE_ENTER:
				case KEYCODE_NUMPAD_ENTER:
					f.setPath(text.getText().toString());
					return true;
				default:
					return UiUtils.dpadFocusHelper(text, keyCode, event);
			}
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
