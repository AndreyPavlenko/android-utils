package me.aap.utils.vfs;

import java.util.List;

import me.aap.utils.async.Completed;
import me.aap.utils.async.FutureSupplier;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

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

	default boolean isFile() {
		return false;
	}

	default boolean isFolder() {
		return true;
	}
}
