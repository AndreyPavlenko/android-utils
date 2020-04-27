package me.aap.utils.vfs;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.pref.PreferenceStore;

import static me.aap.utils.async.Completed.completedEmptyList;

/**
 * @author Andrey Pavlenko
 */
public interface VirtualFileSystem {

	@NonNull
	Provider getProvider();

	@NonNull
	FutureSupplier<VirtualResource> getResource(Uri uri);

	@NonNull
	default FutureSupplier<List<VirtualFolder>> getRoots() {
		return completedEmptyList();
	}

	default boolean isSupportedResource(Uri uri) {
		return getProvider().getSupportedSchemes().contains(uri.getScheme());
	}

	interface Provider {

		@NonNull
		Set<String> getSupportedSchemes();

		@NonNull
		FutureSupplier<VirtualFileSystem> createFileSystem(PreferenceStore ps);
	}
}
