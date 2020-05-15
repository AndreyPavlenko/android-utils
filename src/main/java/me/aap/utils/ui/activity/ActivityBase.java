package me.aap.utils.ui.activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import me.aap.utils.async.Completable;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.Promise;
import me.aap.utils.function.Supplier;

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static me.aap.utils.async.Completed.completed;

/**
 * @author Andrey Pavlenko
 */
public abstract class ActivityBase extends AppCompatActivity implements AppActivity {
	private static final int START_ACTIVITY_REQ = 0;
	private static final int GRANT_PERM_REQ = 1;
	private static ActivityBase instance;
	private static Completable<AppActivity> pendingConsumer;
	private Promise<Intent> startActivity;
	private Promise<int[]> checkPermissions;
	private ActivityDelegate delegate;

	protected abstract Supplier<? extends ActivityDelegate> getConstructor();

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <A extends ActivityBase> FutureSupplier<A> create(
			Context ctx, String channelId, String channelName, @DrawableRes int icon,
			String title, String text, Class<A> c) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel nc = new NotificationChannel(channelId, channelName, IMPORTANCE_LOW);
			NotificationManager nmgr = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
			if (nmgr != null) nmgr.createNotificationChannel(nc);
		}

		ActivityBase i = instance;
		Completable<AppActivity> pending = pendingConsumer;

		if (i != null) {
			if (pending != null) {
				pending.complete((A) i);
				pendingConsumer = null;
			}

			return completed((A) i);
		} else {
			if (pending != null) pending.cancel();
			Promise<A> p = new Promise<>();
			pendingConsumer = (Completable) p;
			NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, channelId);
			b.setSmallIcon(icon).setContentTitle(title).setContentText(text);
			Intent intent = new Intent(ctx, c);
			b.setFullScreenIntent(PendingIntent.getActivity(ctx, 0, intent, 0), true);
			NotificationManagerCompat.from(ctx).notify(0, b.build());
			return p;
		}
	}

	@Override
	public ActivityDelegate getActivityDelegate() {
		return delegate;
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		delegate = ActivityDelegate.create(getConstructor(), this);
		delegate.onActivityCreate(savedInstanceState);
		instance = this;

		if (pendingConsumer != null) {
			Completable<AppActivity> c = pendingConsumer;
			pendingConsumer = null;
			c.complete(this);
		}
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
		instance = null;
	}

	@Override
	public void finish() {
		delegate.onActivityFinish();
		super.finish();
	}

	public FutureSupplier<Intent> startActivityForResult(Intent intent) {
		if (startActivity != null) {
			startActivity.cancel();
			startActivity = null;
		}

		Promise<Intent> p = startActivity = new Promise<>();
		super.startActivityForResult(intent, START_ACTIVITY_REQ, null);
		return p;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if ((requestCode == START_ACTIVITY_REQ) && (startActivity != null)) {
			Promise<Intent> p = startActivity;
			startActivity = null;
			p.complete(data);
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public FutureSupplier<int[]> checkPermissions(String... perms) {
		if (checkPermissions != null) {
			checkPermissions.cancel();
			checkPermissions = null;
		}

		int[] result = new int[perms.length];

		for (int i = 0; i < perms.length; i++) {
			if (ContextCompat.checkSelfPermission(this, perms[i]) != PERMISSION_GRANTED) {
				Promise<int[]> p = checkPermissions = new Promise<>();
				ActivityCompat.requestPermissions(this, perms, GRANT_PERM_REQ);
				return p;
			} else {
				result[i] = PERMISSION_GRANTED;
			}
		}

		return completed(result);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		Promise<int[]> p = checkPermissions;

		if (p != null) {
			checkPermissions = null;
			p.complete(grantResults);
		}
	}
}
