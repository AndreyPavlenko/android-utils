package me.aap.utils.vfs.generic;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Set;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.vfs.VirtualFileSystem;
import me.aap.utils.vfs.VirtualResource;

import static me.aap.utils.async.Completed.completed;

/**
 * @author Andrey Pavlenko
 */
public class GenericFileSystem implements VirtualFileSystem {
	private static final GenericFileSystem instance = new GenericFileSystem();

	public static GenericFileSystem getInstance() {
		return instance;
	}

	@NonNull
	@Override
	public Provider getProvider() {
		return Provider.getInstance();
	}

	public static final class Provider implements VirtualFileSystem.Provider {
		private static final Provider instance = new Provider();

		public static Provider getInstance() {
			return instance;
		}

		@NonNull
		@Override
		public Set<String> getSupportedSchemes() {
			return Collections.emptySet();
		}

		@Override
		public boolean isSupportedResource(Uri uri) {
			return true;
		}

		@NonNull
		@Override
		public FutureSupplier<VirtualFileSystem> getFileSystem() {
			return completed(GenericFileSystem.getInstance());
		}

		@NonNull
		@Override
		public FutureSupplier<VirtualResource> getResource(Uri uri) {
			return completed(new GenericResource(uri));
		}
	}
}
