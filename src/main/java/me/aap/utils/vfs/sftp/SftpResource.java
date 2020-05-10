package me.aap.utils.vfs.sftp;

import androidx.annotation.NonNull;

import com.jcraft.jsch.SftpATTRS;

import java.util.Objects;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.resource.Rid;
import me.aap.utils.vfs.VirtualFileSystem;
import me.aap.utils.vfs.VirtualFolder;
import me.aap.utils.vfs.VirtualResource;

import static me.aap.utils.async.Completed.completed;

/**
 * @author Andrey Pavlenko
 */
class SftpResource implements VirtualResource {
	@NonNull
	private final SftpRoot root;
	@NonNull
	private final String path;
	private Rid rid;
	private FutureSupplier<VirtualFolder> parent;

	SftpResource(@NonNull SftpRoot root, @NonNull String path) {
		this.root = root;
		this.path = path;
	}

	@NonNull
	@Override
	public VirtualFileSystem getVirtualFileSystem() {
		return getRoot().getVirtualFileSystem();
	}

	@NonNull
	@Override
	public String getName() {
		if (path.equals("/")) return "/";
		int idx = path.lastIndexOf('/');
		return (idx != -1) ? path.substring(idx + 1) : path;
	}

	@NonNull
	@Override
	public Rid getRid() {
		if (rid == null) rid = getRoot().buildRid(path);
		return rid;
	}

	@NonNull
	@Override
	public FutureSupplier<VirtualFolder> getParent() {
		FutureSupplier<VirtualFolder> p = parent;
		if (p != null) return p;

		SftpRoot root = getRoot();
		int idx = path.lastIndexOf('/');
		if (idx == 0) return parent = completed(root);

		String pp = path.substring(0, idx);
		if (pp.equals(root.getPath())) return parent = completed(root);

		return parent = completed(new SftpFolder(root, pp));
	}

	@Override
	public FutureSupplier<Long> getLastModified() {
		return lstat().map(s -> s.getATime() * 1000L);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SftpResource that = (SftpResource) o;
		return getRoot().equals(that.getRoot()) && path.equals(that.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getRoot(), path);
	}

	@NonNull
	@Override
	public String toString() {
		return getRid().toString();
	}

	@NonNull
	SftpRoot getRoot() {
		return root;
	}

	@NonNull
	String getPath() {
		return path;
	}

	FutureSupplier<SftpATTRS> lstat() {
		return lstat(getPath());
	}

	FutureSupplier<SftpATTRS> lstat(String path) {
		return getRoot().useChannel(ch -> ch.lstat(path));
	}
}
