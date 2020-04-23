package me.aap.utils.vfs.gdrive;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import javax.security.auth.login.LoginException;

import me.aap.utils.app.App;
import me.aap.utils.async.Completable;
import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.Promise;
import me.aap.utils.function.Supplier;
import me.aap.utils.ui.activity.AppActivity;
import me.aap.utils.vfs.VirtualFileSystem;
import me.aap.utils.vfs.VirtualResource;

import static me.aap.utils.async.Completed.completedNull;

/**
 * @author Andrey Pavlenko
 */
// TODO: Implement
public class GdriveFileSystem implements VirtualFileSystem {
	private final Provider provider;

	public GdriveFileSystem(Provider provider) {
		this.provider = provider;
	}

	@NonNull
	@Override
	public Provider getProvider() {
		return provider;
	}

	public static class Provider implements VirtualFileSystem.Provider {
		private static final Set<String> scheme = Collections.singleton("gdrive");
		@SuppressWarnings({"unchecked", "rawtypes"})
		private static final AtomicReferenceFieldUpdater<Provider, FutureSupplier<Drive>> drive =
				(AtomicReferenceFieldUpdater) AtomicReferenceFieldUpdater.newUpdater(Provider.class, FutureSupplier.class, "driveHolder");
		private final String requestToken;
		private final Supplier<FutureSupplier<? extends AppActivity>> activitySupplier;
		@Keep
		@SuppressWarnings("unused")
		private volatile FutureSupplier<Drive> driveHolder;

		private Provider(String requestToken, Supplier<FutureSupplier<? extends AppActivity>> activitySupplier) {
			this.requestToken = requestToken;
			this.activitySupplier = activitySupplier;
		}

		public static Provider create(String requestToken, Supplier<FutureSupplier<? extends AppActivity>> activitySupplier) {
			return new Provider(requestToken, activitySupplier);
		}

		@NonNull
		@Override
		public Set<String> getSupportedSchemes() {
			return scheme;
		}

		@NonNull
		@Override
		public FutureSupplier<VirtualFileSystem> getFileSystem() {
			return getDrive().then(d -> completedNull());
		}

		@NonNull
		@Override
		public FutureSupplier<VirtualResource> getResource(Uri uri) {
			return getDrive().then(d -> Completed.completedNull());
		}

		private FutureSupplier<Drive> getDrive() {
			FutureSupplier<Drive> d = drive.get(this);
			if (d != null) return d;

			Promise<Drive> p = new Promise<>();

			for (; drive.compareAndSet(this, null, p); d = drive.get(this)) {
				if (d != null) return d;
			}

			GoogleSignInAccount a = GoogleSignIn.getLastSignedInAccount(App.get());

			if (a != null) p.complete(createDrive(a));
			else signIn(p);

			p.thenReplaceOrClear(drive, this);
			return drive.get(this);
		}

		private void signIn(Promise<Drive> p) {
			activitySupplier.get().onFailure(p::completeExceptionally).onSuccess(a -> signIn(p, a));
		}

		private void signIn(Promise<Drive> p, AppActivity activity) {
			GoogleSignInOptions o = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
					.requestScopes(new Scope(DriveScopes.DRIVE))
					.requestIdToken(requestToken).requestEmail().build();
			GoogleSignInClient client = GoogleSignIn.getClient(activity.getContext(), o);
			activity.startActivityForResult((r, d) -> {
				if (d != null) handleSignInResult(p, d);
				else p.completeExceptionally(new LoginException("Google sign in failed"));
			}, client.getSignInIntent());
		}

		private void handleSignInResult(Completable<Drive> p, Intent result) {
			GoogleSignIn.getSignedInAccountFromIntent(result)
					.addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
						@Override
						public void onSuccess(GoogleSignInAccount account) {
							Log.d(getClass().getName(), "Signed in as " + account.getEmail());
							p.complete(createDrive(account));
						}
					}).addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception ex) {
					Log.e(getClass().getName(), "Google sign in failed", ex);
					p.completeExceptionally(ex);
				}
			});
		}

		private static Drive createDrive(GoogleSignInAccount account) {
			App app = App.get();
			GoogleAccountCredential c = GoogleAccountCredential
					.usingOAuth2(app, Collections.singleton(DriveScopes.DRIVE_FILE));
			c.setSelectedAccount(account.getAccount());
			return new Drive.Builder(AndroidHttp.newCompatibleTransport(),
					new GsonFactory(), c).setApplicationName(app.getPackageName()).build();
		}
	}
}
