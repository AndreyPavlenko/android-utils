package me.aap.utils.vfs;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

import java.util.List;
import java.util.UUID;

import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureSupplier;

/**
 * @author Andrey Pavlenko
 */
public interface VirtualFolder extends VirtualResource {

	FutureSupplier<List<VirtualResource>> getChildren();

	default FutureSupplier<VirtualResource> getChild(CharSequence name) {
		return getChildren().then(children -> {
			for (VirtualResource c : children) {
				if (name.equals(c.getName())) return completed(c);
			}
			return completedNull();
		});
	}

	default FutureSupplier<VirtualFile> createFile(CharSequence name) {
		return Completed.failed(new UnsupportedOperationException());
	}

	default FutureSupplier<VirtualFolder> createFolder(CharSequence name) {
		return Completed.failed(new UnsupportedOperationException());
	}

	default FutureSupplier<VirtualFile> createTempFile(CharSequence prefix, CharSequence suffix) {
		String name = prefix.toString() + UUID.randomUUID() + suffix;
		return getChild(name).then(f -> (f == null) ? createFile(name) : f.exists()
				.then(e -> e ? createTempFile(prefix, suffix) : createFile(name)));
	}

	default boolean isFile() {
		return false;
	}

	default boolean isFolder() {
		return true;
	}
}
