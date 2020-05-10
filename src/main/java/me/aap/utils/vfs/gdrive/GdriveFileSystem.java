package me.aap.utils.vfs.gdrive;

import android.content.Intent;

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

import javax.security.auth.login.LoginException;

import me.aap.utils.app.App;
import me.aap.utils.async.Completable;
import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureRef;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.Promise;
import me.aap.utils.function.Supplier;
import me.aap.utils.log.Log;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.resource.Rid;
import me.aap.utils.ui.activity.AppActivity;
import me.aap.utils.vfs.VirtualFileSystem;
import me.aap.utils.vfs.VirtualResource;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

/**
 * @author Andrey Pavlenko
 */
// TODO: Implement
public class GdriveFileSystem implements VirtualFileSystem {
	private final Provider provider;
	private final String requestToken;
	private final Supplier<FutureSupplier<? extends AppActivity>> activitySupplier;
	@Keep
	@SuppressWarnings("unused")
	private final FutureRef<Drive> drive = FutureRef.create(this::getDrive);

	private GdriveFileSystem(Provider provider, String requestToken, Supplier<FutureSupplier<? extends AppActivity>> activitySupplier) {
		this.provider = provider;
		this.requestToken = requestToken;
		this.activitySupplier = activitySupplier;
	}

	@NonNull
	@Override
	public FutureSupplier<VirtualResource> getResource(Rid rid) {
		return getDrive().then(d -> Completed.completedNull());
	}

	private FutureSupplier<Drive> getDrive() {
		GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(App.get());
		if (account != null) return completed(createDrive(account));

		Promise<Drive> p = new Promise<>();
		activitySupplier.get().onFailure(p::completeExceptionally).onSuccess(a -> signIn(p, a));
		return p;
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
						Log.d("Signed in as ", account.getEmail());
						p.complete(createDrive(account));
					}
				}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception ex) {
				Log.e(ex, "Google sign in failed");
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

	@NonNull
	@Override
	public Provider getProvider() {
		return provider;
	}

	public static class Provider implements VirtualFileSystem.Provider {
		private static final Set<String> scheme = Collections.singleton("gdrive");
		private static final Provider instance = new Provider();

		private Provider() {
		}

		public static Provider getInstance() {
			return instance;
		}

		@NonNull
		@Override
		public Set<String> getSupportedSchemes() {
			return scheme;
		}

		@NonNull
		@Override
		public FutureSupplier<VirtualFileSystem> createFileSystem(PreferenceStore ps) {
			return completedNull();
		}
	}
}
