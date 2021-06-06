package me.aap.utils.ui.activity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;

import androidx.activity.result.contract.ActivityResultContract;
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
import me.aap.utils.log.Log;

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.failed;

/**
 * @author Andrey Pavlenko
 */
public abstract class ActivityBase extends AppCompatActivity implements AppActivity {
	private static final int GRANT_PERM_REQ = 1;
	private static ActivityBase instance;
	private static Completable<AppActivity> pendingConsumer;
	private Promise<int[]> checkPermissions;
	@NonNull
	private FutureSupplier<? extends ActivityDelegate> delegate = NO_DELEGATE;

	protected abstract FutureSupplier<? extends ActivityDelegate> createDelegate(AppActivity a);

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
				pending.complete(i);
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

	@NonNull
	@Override
	public FutureSupplier<? extends ActivityDelegate> getActivityDelegate() {
		return delegate;
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		assert delegate == NO_DELEGATE;

		delegate = createDelegate(this).main().onCompletion((d, err) -> {
			if (err != null) {
				Log.e(err, "Failed to create activity delegate");
				delegate = failed(err);
			} else {
				delegate = completed(d);
				d.onActivityCreate(savedInstanceState);
				instance = this;
			}

			if (pendingConsumer != null) {
				Completable<AppActivity> c = pendingConsumer;
				pendingConsumer = null;
				if (err != null) c.completeExceptionally(err);
				else c.complete(this);
			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		delegate.onSuccess(ActivityDelegate::onActivityStart);
	}

	@Override
	protected void onResume() {
		super.onResume();
		delegate.onSuccess(ActivityDelegate::onActivityResume);
	}

	@Override
	protected void onPause() {
		delegate.onSuccess(ActivityDelegate::onActivityPause);
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		delegate.onSuccess(d -> d.onActivitySaveInstanceState(outState));
	}

	@Override
	protected void onStop() {
		delegate.onSuccess(ActivityDelegate::onActivityStop);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		delegate.onSuccess(ActivityDelegate::onActivityDestroy);
		super.onDestroy();
		delegate = NO_DELEGATE;
		instance = null;
	}

	@Override
	public void finish() {
		delegate.onSuccess(ActivityDelegate::onActivityFinish);
		super.finish();
	}

	public FutureSupplier<Intent> startActivityForResult(Supplier<Intent> intent) {
		Promise<Intent> p = new Promise<>();
		registerForActivityResult(new ActivityResultContract<Intent, Intent>() {

			@NonNull
			@Override
			public Intent createIntent(@NonNull Context context, Intent input) {
				return intent.get();
			}

			@Override
			public Intent parseResult(int resultCode, @Nullable Intent intent) {
				return intent;
			}
		}, p::complete);
		return p;
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

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
		ActivityDelegate d = delegate.peek();
		return (d != null) ? d.onKeyUp(keyCode, keyEvent, super::onKeyUp)
				: super.onKeyUp(keyCode, keyEvent);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
		ActivityDelegate d = delegate.peek();
		return (d != null) ? d.onKeyDown(keyCode, keyEvent, super::onKeyDown)
				: super.onKeyDown(keyCode, keyEvent);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent keyEvent) {
		ActivityDelegate d = delegate.peek();
		return (d != null) ? d.onKeyLongPress(keyCode, keyEvent, super::onKeyLongPress)
				: super.onKeyLongPress(keyCode, keyEvent);
	}
}
