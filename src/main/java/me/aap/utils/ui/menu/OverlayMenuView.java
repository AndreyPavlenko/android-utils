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
import androidx.annotation.StringRes;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.textview.MaterialTextView;

import me.aap.utils.R;
import me.aap.utils.ui.activity.ActivityDelegate;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static me.aap.utils.ui.UiUtils.ID_NULL;
import static me.aap.utils.ui.UiUtils.toPx;

/**
 * @author Andrey Pavlenko
 */
public class OverlayMenuView extends ScrollView implements OverlayMenu {
	private SelectionHandler handler;
	private CloseHandler closeHandler;
	private OverlayMenuItemView selectedItem;
	@ColorInt
	private final int headerColor;
	private final LinearLayoutCompat childGroup;
	private View focus;

	public OverlayMenuView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs, R.attr.popupMenuStyle);

		TypedArray ta = ctx.obtainStyledAttributes(attrs,
				new int[]{android.R.attr.colorBackground, R.attr.colorPrimarySurface},
				R.attr.popupMenuStyle, R.style.Theme_Utils_Base_PopupMenuStyle);
		setBackgroundColor(ta.getColor(0, Color.TRANSPARENT));
		headerColor = ta.getColor(1, Color.TRANSPARENT);
		ta.recycle();

		childGroup = new LinearLayoutCompat(getContext());
		childGroup.setOrientation(LinearLayoutCompat.VERTICAL);
		childGroup.setLayoutParams(new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
		addView(childGroup);
	}

	@Override
	public View inflate(@LayoutRes int content) {
		hide();
		return inflateLayout(content);
	}

	@Override
	public void setView(View view) {
		hide();
		initMenuItems(getChildGroup());
	}

	private View inflateLayout(@LayoutRes int content) {
		ViewGroup g = getChildGroup();
		inflate(getContext(), content, g);
		initMenuItems(g);
		return g;
	}

	private void initMenuItems(ViewGroup g) {
		for (int i = 0, count = g.getChildCount(); i < count; i++) {
			View c = g.getChildAt(i);
			if (c instanceof OverlayMenuItemView) ((OverlayMenuItemView) c).parent = this;
		}
	}

	@Override
	public void show(SelectionHandler handler, CloseHandler closeHandler) {
		this.handler = handler;
		this.closeHandler = closeHandler;
		ActivityDelegate a = ActivityDelegate.get(getContext());
		focus = a.getCurrentFocus();
		a.setActiveMenu(this);
		setVisibility(VISIBLE);
		ViewGroup g = getChildGroup();

		if (selectedItem != null) {
			selectedItem.setSelected(true);
			selectedItem.requestFocus();
		} else {
			View select = null;

			for (int i = 0, count = g.getChildCount(); i < count; i++) {
				View c = g.getChildAt(i);

				if (c.getVisibility() != VISIBLE) continue;
				if ((select == null) && c.isFocusable()) select = c;

				if (c instanceof OverlayMenuItemView) {
					c.requestFocus();
					select = null;
					break;
				}
			}

			if (select != null) select.requestFocus();
		}
	}

	@Override
	public void hide() {
		if (handler == null) return;

		CloseHandler ch = closeHandler;
		View f = focus;
		handler = null;
		closeHandler = null;
		selectedItem = null;
		focus = null;
		getChildGroup().removeAllViews();
		setVisibility(GONE);
		ActivityDelegate.get(getContext()).setActiveMenu(null);
		if (ch != null) ch.menuClosed(this);
		if (f != null) f.requestFocus();
	}

	@Override
	public OverlayMenuItem findItem(int id) {
		return super.findViewById(id);
	}

	@Override
	public OverlayMenuItem addItem(int id, Drawable icon, CharSequence title) {
		OverlayMenuItemView i = new OverlayMenuItemView(this, id, icon, title);
		getChildGroup().addView(i);
		return i;
	}

	@Override
	public void setSelectedItem(OverlayMenuItem item) {
		selectedItem = (OverlayMenuItemView) item;
	}

	public OverlayMenuItemView getSelectedItem() {
		return selectedItem;
	}

	@Override
	public void setTitle(@StringRes int title) {
		setTitle(getContext().getResources().getString(title));
	}

	@Override
	public void setTitle(CharSequence title) {
		hide();
		MaterialTextView v = new MaterialTextView(getContext());
		LinearLayoutCompat.LayoutParams p = new LinearLayoutCompat.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
		v.setGravity(Gravity.CENTER);
		v.setText(title);
		v.setLayoutParams(p);
		v.setElevation(toPx(10));
		v.setTranslationZ(toPx(10));
		v.setBackgroundColor(headerColor);
		v.setPadding(toPx(5), toPx(10), toPx(20), toPx(10));
		getChildGroup().addView(v);
	}

	void menuItemSelected(OverlayMenuItemView item) {
		if (handler == null) return;
		SelectionHandler h = handler;

		if ((item.submenu == ID_NULL) || (item.submenu == R.layout.dynamic)) {
			hide();
			h.menuItemSelected(item);
		} else {
			hide();
			setTitle(item.getTitle());
			inflateLayout(item.submenu);
			h.menuItemSelected(item);
			if (handler == null) show(h);
		}
	}

	private ViewGroup getChildGroup() {
		return childGroup;
	}
}
