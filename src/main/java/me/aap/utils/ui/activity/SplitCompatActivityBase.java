package me.aap.utils.ui.activity;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.play.core.splitcompat.SplitCompat;

/**
 * @author Andrey Pavlenko
 */
public abstract class SplitCompatActivityBase extends ActivityBase {

	@Override
	protected void attachBaseContext(Context newBase) {
		super.attachBaseContext(newBase);
		SplitCompat.installActivity(this);
	}

	@Override
	public void onAttachFragment(@NonNull Fragment fragment) {
		SplitCompat.install(fragment.getContext());
	}
}
