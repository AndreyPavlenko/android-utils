package me.aap.utils.ui.menu;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.aap.utils.R;
import me.aap.utils.function.Consumer;
import me.aap.utils.ui.activity.ActivityDelegate;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static java.util.Objects.requireNonNull;
import static me.aap.utils.ui.UiUtils.toPx;

/**
 * @author Andrey Pavlenko
 */
public class OverlayMenuView extends ScrollView implements OverlayMenu {
	@ColorInt
	private final int headerColor;
	@Nullable
	MenuBuilder builder;

	public OverlayMenuView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs, R.attr.popupMenuStyle);

		TypedArray ta = ctx.obtainStyledAttributes(attrs,
				new int[]{android.R.attr.colorBackground, R.attr.colorPrimarySurface},
				R.attr.popupMenuStyle, R.style.Theme_Utils_Base_PopupMenuStyle);
		setBackgroundColor(ta.getColor(0, Color.TRANSPARENT));
		headerColor = ta.getColor(1, Color.TRANSPARENT);
		ta.recycle();
	}

	@Override
	public void show(Consumer<Builder> consumer) {
		hide();
		MenuBuilder b = new MenuBuilder(consumer, null);
		b.focus = ActivityDelegate.get(getContext()).getCurrentFocus();
		showMenu(b);
	}

	private void showMenu(MenuBuilder builder) {
		ActivityDelegate.get(getContext()).setActiveMenu(this);
		this.builder = builder;
		setVisibility(VISIBLE);

		ViewGroup g = builder.view;
		builder.consumer.accept(builder);
		int count = g.getChildCount();
		if (count == 0) return;

		List<View> items = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			View c = g.getChildAt(i);
			if (c.getId() == R.id.overlay_menu_title) continue;
			if (!(c instanceof OverlayMenuItemView)) return;
			if (c.getVisibility() != VISIBLE) continue;
			items.add(c);
		}

		int size = items.size();
		if (size == 0) return;

		View focus = items.get(0);
		int firstId = focus.getId();
		int lastId = items.get(size - 1).getId();

		for (int i = 0; i < size; i++) {
			View c = items.get(i);
			int downId = (i == (size - 1)) ? firstId : items.get(i + 1).getId();
			c.setNextFocusDownId(downId);
			c.setNextFocusForwardId(downId);
			c.setNextFocusUpId((i == 0) ? lastId : items.get(i - 1).getId());
			c.setNextFocusLeftId(firstId);
			c.setNextFocusRightId(lastId);

			if (c == builder.selectedItem) {
				focus = c;
				focus.setSelected(true);
			}
		}

		focus.requestFocus();
		invalidate();
	}

	@Override
	public void hide() {
		if (builder == null) return;
		CloseHandler ch = builder.closeHandler;
		View f = builder.focus;
		builder.cleanUp();
		builder = null;
		setVisibility(GONE);
		ActivityDelegate.get(getContext()).setActiveMenu(null);
		if (ch != null) ch.menuClosed(this);
		if (f != null) f.requestFocus();
	}

	@Override
	public boolean back() {
		if (builder == null) return false;

		MenuBuilder b = builder.parent;
		if (b == null) return false;

		int parentItemId = builder.parentItemId;
		builder.cleanUp();
		b.init();
		if (b.title != null) b.setTitle(b.title);
		showMenu(b);

		if (parentItemId != NO_ID) {
			View f = findViewById(parentItemId);
			if (f != null) f.requestFocus();
		}

		return true;
	}

	@Override
	public OverlayMenuItem findItem(int id) {
		return super.findViewById(id);
	}

	@Override
	public List<OverlayMenuItem> getItems() {
		if ((builder == null) || (builder.view == null)) return Collections.emptyList();
		int count = builder.view.getChildCount();
		List<OverlayMenuItem> items = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			View c = builder.view.getChildAt(i);
			if (c instanceof OverlayMenuItem) items.add((OverlayMenuItem) c);
		}
		return items;
	}

	void menuItemSelected(OverlayMenuItemView item) {
		if (item.submenuBuilder == null) {
			SelectionHandler handler = item.handler;
			if (handler == null) handler = requireNonNull(builder).selectionHandler;
			hide();
			if (handler != null) handler.menuItemSelected(item);
		} else {
			MenuBuilder builder = requireNonNull(this.builder);
			builder.cleanUp();
			MenuBuilder b = new MenuBuilder(item.submenuBuilder, builder);
			b.setParentItem(item);
			showMenu(b);
		}
	}

	final class MenuBuilder implements Builder {
		final Consumer<Builder> consumer;
		final MenuBuilder parent;
		ViewGroup view;
		SelectionHandler selectionHandler;
		CloseHandler closeHandler;
		OverlayMenuItemView selectedItem;
		View focus;
		String title;
		int parentItemId = NO_ID;

		public MenuBuilder(Consumer<Builder> consumer, MenuBuilder parent) {
			this.consumer = consumer;
			this.parent = parent;
			init();
		}

		void init() {
			LinearLayoutCompat view = new LinearLayoutCompat(getContext());
			view.setOrientation(LinearLayoutCompat.VERTICAL);
			view.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
			view.setPadding(0, 0, 0, 0);
			addView(view);
			this.view = view;
		}

		void cleanUp() {
			if (view == null) return;
			removeView(view);
			view = null;
			selectionHandler = null;
			closeHandler = null;
			selectedItem = null;
		}

		void setParentItem(OverlayMenuItemView item) {
			parentItemId = item.getItemId();
			setTitle(item.getTitle());
		}

		@Override
		public OverlayMenuView getMenu() {
			return OverlayMenuView.this;
		}

		@Override
		public OverlayMenuItem addItem(int id, Drawable icon, CharSequence title) {
			OverlayMenuItemView i = new OverlayMenuItemView(OverlayMenuView.this, id, icon, title);
			view.addView(i);
			return i;
		}

		@Override
		public OverlayMenuItem addItem(int id, Drawable icon, CharSequence title,
																	 int relativeToId, boolean after) {
			OverlayMenuItemView i = (OverlayMenuItemView) findItem(relativeToId);
			if (i == null) return addItem(id, icon, title);

			int idx = view.indexOfChild(i);
			if (after) idx++;

			i = new OverlayMenuItemView(OverlayMenuView.this, id, icon, title);
			view.addView(i, idx);
			return i;
		}

		@Override
		public Builder setSelectedItem(OverlayMenuItem item) {
			selectedItem = (OverlayMenuItemView) item;
			return this;
		}

		@Override
		public Builder setTitle(CharSequence title) {
			ViewGroup g = view;
			this.title = title.toString();

			if (g.getChildCount() > 0) {
				View v = g.getChildAt(0);

				if (v.getId() == R.id.overlay_menu_title) {
					((MaterialTextView) v).setText(this.title);
					return this;
				}
			}

			Context ctx = getContext();
			MaterialTextView v = new MaterialTextView(ctx);
			LinearLayoutCompat.LayoutParams p = new LinearLayoutCompat.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
			int padding = (int) toPx(ctx, 10);
			int elevation = (int) toPx(ctx, 5);
			v.setId(R.id.overlay_menu_title);
			v.setGravity(Gravity.CENTER);
			v.setText(this.title);
			v.setLayoutParams(p);
			v.setElevation(elevation);
			v.setTranslationZ(elevation);
			v.setBackgroundColor(headerColor);
			v.setPadding(0, padding, 0, padding);
			view.addView(v, 0);
			v.setVisibility(VISIBLE);
			return this;
		}

		@Override
		public View inflate(@LayoutRes int content) {
			return inflateLayout(content);
		}

		@Override
		public Builder setView(View view) {
			initMenuItems(this.view);
			this.view.addView(view);
			return this;
		}

		private View inflateLayout(@LayoutRes int content) {
			ViewGroup g = view;
			View.inflate(getContext(), content, g);
			initMenuItems(g);
			return g;
		}

		private void initMenuItems(ViewGroup g) {
			for (int i = 0, count = g.getChildCount(); i < count; i++) {
				View c = g.getChildAt(i);
				if (c instanceof OverlayMenuItemView) {
					((OverlayMenuItemView) c).parent = OverlayMenuView.this;
				}
			}
		}

		@Override
		public Builder setSelectionHandler(SelectionHandler selectionHandler) {
			this.selectionHandler = selectionHandler;
			return this;
		}

		@Override
		public Builder setCloseHandlerHandler(CloseHandler closeHandler) {
			this.closeHandler = closeHandler;
			return this;
		}
	}
}
