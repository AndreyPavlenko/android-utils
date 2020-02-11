package me.aap.utils.ui.menu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.LinearLayoutCompat;

import me.aap.utils.R;

import static me.aap.utils.ui.UiUtils.ID_NULL;
import static me.aap.utils.ui.UiUtils.toPx;

/**
 * @author Andrey Pavlenko
 */
public class OverlayMenuItemView extends LinearLayoutCompat implements OverlayMenuItem, OnClickListener,
		OnLongClickListener, OnCheckedChangeListener {
	@SuppressLint("InlinedApi")
	@LayoutRes
	int submenu = ID_NULL;
	OverlayMenuView parent;
	private Object data;
	private boolean isLongClick;

	OverlayMenuItemView(OverlayMenuView parent, int id, boolean checkable, Drawable icon, CharSequence title) {
		super(parent.getContext(), null, R.attr.popupMenuStyle);
		this.parent = parent;
		setId(id);
		init(parent.getContext(), checkable, icon, title);
	}

	@SuppressLint("InlinedApi")
	public OverlayMenuItemView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs, R.attr.popupMenuStyle);
		TypedArray ta = ctx.obtainStyledAttributes(attrs, R.styleable.OverlayMenuItemView,
				R.attr.popupMenuStyle, R.style.Theme_Utils_Base_PopupMenuStyle);
		boolean checkable = ta.getBoolean(R.styleable.OverlayMenuItemView_checkable, false);
		Drawable icon = ta.getDrawable(R.styleable.OverlayMenuItemView_icon);
		CharSequence title = ta.getText(R.styleable.OverlayMenuItemView_text);
		submenu = ta.getResourceId(R.styleable.OverlayMenuItemView_submenu, ID_NULL);
		ta.recycle();
		init(ctx, checkable, icon, title);
	}

	private void init(Context ctx, boolean checkable, Drawable icon, CharSequence title) {
		setOrientation(HORIZONTAL);

		if (submenu != ID_NULL) {
			inflate(ctx, R.layout.submenu_item, this);
		} else if (checkable) {
			inflate(ctx, R.layout.checkable_menu_item, this);
			CheckBox cb = getCheckBox();
			cb.setOnCheckedChangeListener(this);
			cb.setGravity(Gravity.CENTER_VERTICAL);
			cb.setLayoutDirection(LAYOUT_DIRECTION_LTR);
		} else {
			inflate(ctx, R.layout.menu_item, this);
		}

		if (icon == null) getIconView().setVisibility(GONE);
		else getIconView().setImageDrawable(icon);

		getTitleView().setText(title);

		setFocusable(true);
		setOnClickListener(this);
		setOnLongClickListener(this);
		setPadding(toPx(5), toPx(10), toPx(20), toPx(10));
		setBackgroundResource(R.drawable.focusable_shape_transparent);
	}

	public int getItemId() {
		return getId();
	}

	public OverlayMenuView getMenu() {
		return parent;
	}

	@Override
	public boolean isLongClick() {
		return isLongClick;
	}

	public CharSequence getTitle() {
		return getTitleView().getText();
	}

	@Override
	public OverlayMenuItem setTitle(@StringRes int title) {
		getTitleView().setText(title);
		return this;
	}

	public OverlayMenuItem setTitle(CharSequence title) {
		getTitleView().setText(title);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getData() {
		return (T) data;
	}

	@Override
	public <T> OverlayMenuItem setData(T data) {
		this.data = data;
		return this;
	}

	@Override
	public OverlayMenuItem setChecked(boolean checked) {
		if (checked) {
			getCheckBox().setChecked(true);
			if ((parent != null) && (parent.getSelectedItem() == null)) parent.setSelectedItem(this);
		} else {
			getCheckBox().setChecked(false);
		}
		return this;
	}

	public OverlayMenuItem setVisible(boolean visible) {
		setVisibility(visible ? VISIBLE : GONE);
		return this;
	}

	@Override
	public void onClick(View v) {
		getMenu().menuItemSelected(this);
	}

	@Override
	public boolean onLongClick(View v) {
		isLongClick = true;
		getMenu().menuItemSelected(this);
		isLongClick = false;
		return true;
	}

	@Override
	public void onCheckedChanged(CompoundButton v, boolean isChecked) {
		onClick(v);
	}

	private ImageView getIconView() {
		return (ImageView) getChildAt(0);
	}

	private TextView getTitleView() {
		return (TextView) getChildAt(1);
	}

	private CheckBox getCheckBox() {
		return (CheckBox) getChildAt(2);
	}
}
