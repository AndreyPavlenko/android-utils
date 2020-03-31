package me.aap.utils.ui.menu;

import android.content.Context;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

import java.util.function.Consumer;
import me.aap.utils.ui.menu.OverlayMenu.Builder;
import me.aap.utils.ui.menu.OverlayMenu.SelectionHandler;

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

	default OverlayMenuItem setChecked(boolean checked) {
		return setChecked(checked, false);
	}

	OverlayMenuItem setChecked(boolean checked, boolean selectChecked);

	OverlayMenuItem setVisible(boolean visible);

	OverlayMenuItem setMultiLine(boolean multiLine);

	OverlayMenuItem setHandler(SelectionHandler handler);

	OverlayMenuItem setSubmenu(Consumer<Builder> builder);

	default OverlayMenuItem setSubmenu(@LayoutRes int layout) {
		setSubmenu(b-> b.inflate(layout));
		return this;
	}
}
