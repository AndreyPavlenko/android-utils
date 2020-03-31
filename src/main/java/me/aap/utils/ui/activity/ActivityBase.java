package me.aap.utils.ui.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author Andrey Pavlenko
 */
public abstract class ActivityBase extends AppCompatActivity implements AppActivity {
	private static final int START_ACTIVITY_REQ = 0;
	private static final int GRANT_PERM_REQ = 1;
	private BiConsumer<Integer, Intent> resultHandler;
	private ActivityDelegate delegate;

	protected abstract Supplier<? extends ActivityDelegate> getConstructor();

	@Override
	public ActivityDelegate getActivityDelegate() {
		return delegate;
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		delegate = ActivityDelegate.create(getConstructor(), this);
		delegate.onActivityCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		delegate.onActivityStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		delegate.onActivityResume();
	}

	@Override
	protected void onPause() {
		delegate.onActivityPause();
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		delegate.onActivitySaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		delegate.onActivityStop();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		delegate.onActivityDestroy();
		super.onDestroy();
		delegate = null;
	}

	@Override
	public void finish() {
		delegate.onActivityFinish();
		super.finish();
	}

	public void startActivityForResult(BiConsumer<Integer, Intent> resultHandler, Intent intent) {
		assert this.resultHandler == null;
		this.resultHandler = resultHandler;
		super.startActivityForResult(intent, START_ACTIVITY_REQ, null);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if ((requestCode == START_ACTIVITY_REQ) && (resultHandler != null)) {
			BiConsumer<Integer, Intent> h = resultHandler;
			resultHandler = null;
			h.accept(resultCode, data);
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void checkPermissions(String... perms) {
		for (String perm : perms) {
			if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, perms, GRANT_PERM_REQ);
				return;
			}
		}
	}
}
