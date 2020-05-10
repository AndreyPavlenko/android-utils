package me.aap.utils.vfs;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.resource.Rid;

import static me.aap.utils.async.Completed.completedEmptyList;

/**
 * @author Andrey Pavlenko
 */
public interface VirtualFileSystem {

	@NonNull
	Provider getProvider();

	@NonNull
	FutureSupplier<VirtualResource> getResource(Rid rid);

	@NonNull
	default FutureSupplier<List<VirtualFolder>> getRoots() {
		return completedEmptyList();
	}

	default boolean isSupportedResource(Rid rid) {
		return getProvider().getSupportedSchemes().contains(rid.getScheme().toString());
	}

	interface Provider {

		@NonNull
		Set<String> getSupportedSchemes();

		@NonNull
		FutureSupplier<VirtualFileSystem> createFileSystem(PreferenceStore ps);
	}
}
