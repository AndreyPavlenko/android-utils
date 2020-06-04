package me.aap.utils.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.util.AttributeSet;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.FragmentManager;

import me.aap.utils.async.FutureSupplier;

/**
 * @author Andrey Pavlenko
 */
public interface AppActivity {

	ActivityDelegate getActivityDelegate();

	Theme getTheme();

	void setTheme(int resid);

	Window getWindow();

	View getCurrentFocus();

	@NonNull
	FragmentManager getSupportFragmentManager();

	void setContentView(@LayoutRes int layoutResID);

	<T extends View> T findViewById(@IdRes int id);

	void recreate();

	void finish();

	FutureSupplier<Intent> startActivityForResult(Intent intent);

	FutureSupplier<int[]> checkPermissions(String... perms);

	default Context getContext() {
		return (Context) this;
	}

	default EditText createEditText(Context ctx, AttributeSet attrs) {
		return new AppCompatEditText(ctx, attrs);
	}
}
