package me.aap.utils.vfs;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

import me.aap.utils.async.FutureSupplier;

import static me.aap.utils.async.Completed.completedEmptyList;

/**
 * @author Andrey Pavlenko
 */
public interface VirtualFileSystem {

	@NonNull
	Provider getProvider();

	@NonNull
	default FutureSupplier<List<VirtualFolder>> getRoots() {
		return completedEmptyList();
	}

	interface Provider {

		@NonNull
		Set<String> getSupportedSchemes();

		@NonNull
		FutureSupplier<VirtualFileSystem> getFileSystem();

		@NonNull
		FutureSupplier<VirtualResource> getResource(Uri uri);

		default boolean isSupportedResource(Uri uri) {
			return getSupportedSchemes().contains(uri.getScheme());
		}
	}
}
