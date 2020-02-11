package me.aap.utils.ui.menu;

import android.content.Context;

import androidx.annotation.StringRes;

/**
 * @author Andrey Pavlenko
 */
public interface OverlayMenuItem {

	Context getContext();

	int getItemId();

	OverlayMenu getMenu();

	boolean isLongClick();

	CharSequence getTitle();

	OverlayMenuItem setTitle(@StringRes int title);

	OverlayMenuItem setTitle(CharSequence title);

	<T> OverlayMenuItem setData(T data);

	<T> T getData();

	OverlayMenuItem setChecked(boolean checked);

	OverlayMenuItem setVisible(boolean visible);
}
