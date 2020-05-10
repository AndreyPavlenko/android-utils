package me.aap.utils.vfs.sftp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.Supplier;
import me.aap.utils.log.Log;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.pref.PreferenceStore.Pref;
import me.aap.utils.resource.Rid;
import me.aap.utils.text.SharedTextBuilder;
import me.aap.utils.text.TextBuilder;
import me.aap.utils.text.TextUtils;
import me.aap.utils.vfs.VirtualFileSystem;
import me.aap.utils.vfs.VirtualFolder;
import me.aap.utils.vfs.VirtualResource;

import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;

/**
 * @author Andrey Pavlenko
 */
public class SftpFileSystem implements VirtualFileSystem {
	private final Pref<Supplier<String[]>> SFTP_ROOTS = Pref.sa("SFTP_ROOTS", () -> new String[0]);
	private final Provider provider;
	private final PreferenceStore ps;
	private volatile List<VirtualFolder> roots;

	private SftpFileSystem(Provider provider, PreferenceStore ps) {
		this.provider = provider;
		this.ps = ps;

		String[] pref = ps.getStringArrayPref(SFTP_ROOTS);

		if (pref.length == 0) {
			roots = Collections.emptyList();
			return;
		}

		ArrayList<VirtualFolder> roots = new ArrayList<>(pref.length);

		for (String p : pref) {
			try {
				VirtualFolder r = SftpRoot.create(this, p);
				if (r == null) Log.e("Invalid resource id: ", p);
				roots.add(r);
			} catch (NumberFormatException ex) {
				Log.e("Invalid resource id: ", p);
			}
		}

		roots.trimToSize();
		this.roots = !roots.isEmpty() ? roots : Collections.emptyList();
	}

	@NonNull
	@Override
	public Provider getProvider() {
		return provider;
	}

	@NonNull
	@Override
	public FutureSupplier<VirtualResource> getResource(Rid rid) {
		CharSequence user = rid.getUserInfo();
		if (user == null) return completedNull();
		CharSequence host = rid.getHost();
		if (host == null) return completedNull();
		CharSequence path = rid.getPath();
		if (path == null) return completedNull();
		int port = rid.getPort();
		if (port == -1) port = 22;

		for (VirtualResource root : roots) {
			SftpRoot r = (SftpRoot) root;

			if (TextUtils.equals(host, r.getHost()) && TextUtils.equals(user, r.getUser()) && (port == r.getPort())) {
				String p = r.getPath();
				if (TextUtils.startsWith(path, p) && ((p.length() == path.length()) || path.charAt(p.length()) == '/')) {
					String spath = path.toString();
					return r.lstat(spath).ifFail(fail -> null).map(s -> {
						if (s == null) return null;
						if (s.isDir()) return new SftpFolder(r, spath);
						else return new SftpFile(r, spath);
					});
				}
			}
		}

		return completedNull();
	}

	@NonNull
	@Override
	public FutureSupplier<List<VirtualFolder>> getRoots() {
		return completed(roots);
	}

	public FutureSupplier<VirtualFolder> addRoot(@NonNull String user, @NonNull String host, int port,
																							 @Nullable String path, @Nullable String password,
																							 @Nullable String keyFile, @Nullable String keyPass) {
		return SftpRoot.create(this, user, host, port, path, password, keyFile, keyPass).map(r -> {
			synchronized (this) {
				List<VirtualFolder> roots = this.roots;
				Rid rid = r.getRid();

				for (VirtualFolder root : roots) {
					if (rid.equals(root.getRid())) return root;
				}

				List<VirtualFolder> newRoots = new ArrayList<>(roots.size() + 1);
				newRoots.addAll(roots);
				newRoots.add(r);
				setRoots(newRoots);
				return r;
			}
		});
	}

	public synchronized boolean removeRoot(VirtualFolder root) {
		List<VirtualFolder> roots = this.roots;
		if (roots.isEmpty()) return false;

		boolean removed = false;
		List<VirtualFolder> newRoots = new ArrayList<>(roots.size() - 1);
		Rid rid = root.getRid();

		for (VirtualFolder r : roots) {
			if (r.getRid().equals(rid)) removed = true;
			else newRoots.add(r);
		}

		if (!removed) return false;

		setRoots(newRoots.isEmpty() ? Collections.emptyList() : newRoots);
		return true;
	}

	private void setRoots(List<VirtualFolder> roots) {
		this.roots = roots;
		String[] pref = new String[roots.size()];

		for (int i = 0, s = roots.size(); i < s; i++) {
			pref[i] = roots.get(i).getRid().toString();
		}

		getPreferenceStore().applyStringArrayPref(SFTP_ROOTS, pref);
	}

	static Rid buildRid(@NonNull String user, @NonNull String host, int port, @Nullable String path) {
		try (SharedTextBuilder tb = SharedTextBuilder.get()) {
			appendUri(tb, user, host, port, path);
			return Rid.create(tb);
		}
	}

	static void appendUri(TextBuilder tb, @NonNull String user,
												@NonNull String host, int port, @Nullable String path) {
		tb.append("sftp://").append(user).append('@');
		if (host.indexOf(':') != -1) tb.append('[').append(host).append(']');
		else tb.append(host);
		if (port != 22) tb.append(':').append(port);
		if (path != null) tb.append(path);
	}


	PreferenceStore getPreferenceStore() {
		return ps;
	}

	public static class Provider implements VirtualFileSystem.Provider {
		private final Set<String> schemes = Collections.singleton("sftp");
		private static final Provider instance = new Provider();

		private Provider() {
		}

		public static Provider getInstance() {
			return instance;
		}

		@NonNull
		@Override
		public Set<String> getSupportedSchemes() {
			return schemes;
		}

		@NonNull
		@Override
		public FutureSupplier<VirtualFileSystem> createFileSystem(PreferenceStore ps) {
			return completed(new SftpFileSystem(this, ps));
		}
	}
}
