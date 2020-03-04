package me.aap.utils.ui.menu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.LinearLayoutCompat;

import me.aap.utils.R;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static me.aap.utils.ui.UiUtils.ID_NULL;

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

	OverlayMenuItemView(OverlayMenuView parent, int id, Drawable icon, CharSequence text) {
		super(parent.getContext(), null, R.attr.popupMenuStyle);
		this.parent = parent;
		setId(id);

		Context ctx = parent.getContext();
		TypedArray ta = ctx.obtainStyledAttributes(null, R.styleable.OverlayMenuItemView,
				R.attr.popupMenuStyle, R.style.Theme_Utils_Base_PopupMenuStyle);
		ColorStateList iconTint = ta.getColorStateList(R.styleable.OverlayMenuItemView_tint);
		int textColor = ta.getColor(R.styleable.OverlayMenuItemView_android_textColor, Color.BLACK);
		int textAppearance = ta.getResourceId(R.styleable.OverlayMenuItemView_android_textAppearance, R.attr.textAppearanceListItem);
		int padding = (int) ta.getDimension(R.styleable.OverlayMenuItemView_android_padding, 5);
		ta.recycle();
		init(ctx, null, icon, iconTint, text, textColor, textAppearance, padding);
	}

	@SuppressLint("InlinedApi")
	public OverlayMenuItemView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs, R.attr.popupMenuStyle);

		TypedArray ta = ctx.obtainStyledAttributes(attrs, R.styleable.OverlayMenuItemView,
				R.attr.popupMenuStyle, R.style.Theme_Utils_Base_PopupMenuStyle);
		ColorStateList iconTint = ta.getColorStateList(R.styleable.OverlayMenuItemView_tint);
		Drawable icon = ta.getDrawable(R.styleable.OverlayMenuItemView_icon);
		CharSequence text = ta.getText(R.styleable.OverlayMenuItemView_text);
		int textColor = ta.getColor(R.styleable.OverlayMenuItemView_android_textColor, Color.BLACK);
		int textAppearance = ta.getResourceId(R.styleable.OverlayMenuItemView_android_textAppearance, R.attr.textAppearanceListItem);
		int padding = (int) ta.getDimension(R.styleable.OverlayMenuItemView_android_padding, 5);
		submenu = ta.getResourceId(R.styleable.OverlayMenuItemView_submenu, ID_NULL);
		ta.recycle();
		init(ctx, attrs, icon, iconTint, text, textColor, textAppearance, padding);
	}

	private void init(Context ctx, AttributeSet attrs, Drawable icon, ColorStateList iconTint,
										CharSequence text, int textColor, int textAppearance, int padding) {
		setOrientation(HORIZONTAL);

		Drawable rightIcon = (submenu == ID_NULL) ? null : ctx.getDrawable(R.drawable.chevron_right);
		TextView t = new TextView(ctx, attrs, R.attr.popupMenuStyle);
		LinearLayoutCompat.LayoutParams lp = new LinearLayoutCompat.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
		t.setLayoutParams(lp);
		t.setText(text);
		t.setTextColor(textColor);
		t.setTextAppearance(textAppearance);
		t.setCompoundDrawableTintList(iconTint);
		t.setCompoundDrawablesWithIntrinsicBounds(icon, null, rightIcon, null);
		t.setCompoundDrawablePadding(padding);
		t.setPadding(padding, padding, padding, padding);
		t.setSingleLine(true);
		t.setVisibility(VISIBLE);
		t.setTextAlignment(TEXT_ALIGNMENT_VIEW_START);
		addView(t);

		setFocusable(true);
		setOnClickListener(this);
		setOnLongClickListener(this);
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
		return getText().getText();
	}

	@Override
	public OverlayMenuItem setTitle(@StringRes int title) {
		getText().setText(title);
		return this;
	}

	public OverlayMenuItem setTitle(CharSequence title) {
		getText().setText(title);
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
		TextView t = getText();
		Drawable[] d = t.getCompoundDrawables();
		d[2] = getContext().getDrawable(checked ? R.drawable.check_box : R.drawable.check_box_blank);
		t.setCompoundDrawablesWithIntrinsicBounds(d[0], d[1], d[2], d[3]);
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

	private TextView getText() {
		return (TextView) getChildAt(0);
	}
}
