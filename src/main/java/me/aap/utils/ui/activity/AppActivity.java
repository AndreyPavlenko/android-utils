package me.aap.utils.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.Theme;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.textfield.TextInputEditText;

import me.aap.utils.R;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.ui.view.DialogBuilder;

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

	default EditText createEditText(Context ctx) {
		return new TextInputEditText(ctx, null, R.attr.editTextStyle);
	}

	default DialogBuilder createDialogBuilder(Context ctx) {
		return DialogBuilder.create(ctx);
	}
}
