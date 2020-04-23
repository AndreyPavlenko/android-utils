package me.aap.utils.vfs.generic;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Objects;

import me.aap.utils.vfs.VirtualFileSystem;
import me.aap.utils.vfs.VirtualResource;

/**
 * @author Andrey Pavlenko
 */
public class GenericResource implements VirtualResource {
	private final Uri uri;

	public GenericResource(Uri uri) {
		this.uri = uri;
	}

	@NonNull
	@Override
	public VirtualFileSystem getVirtualFileSystem() {
		return GenericFileSystem.getInstance();
	}

	@NonNull
	@Override
	public String getName() {
		return getUri().toString();
	}

	@NonNull
	@Override
	public Uri getUri() {
		return uri;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GenericResource that = (GenericResource) o;
		return Objects.equals(uri, that.uri);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uri);
	}
}