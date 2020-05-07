package me.aap.utils.app;

import android.content.Context;

import com.google.android.play.core.splitcompat.SplitCompat;

/**
 * @author Andrey Pavlenko
 */
public class NetSplitCompatApp extends NetApp {
	@Override
	protected void attachBaseContext(Context context) {
		super.attachBaseContext(context);
		SplitCompat.install(this);
	}
}
