package me.aap.utils.vfs.sftp;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.UserInfo;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import me.aap.utils.app.App;
import me.aap.utils.async.Async;
import me.aap.utils.async.FutureRef;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.CheckedFunction;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.text.SharedTextBuilder;
import me.aap.utils.vfs.VfsException;
import me.aap.utils.vfs.VirtualFileSystem;
import me.aap.utils.vfs.VirtualFolder;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;
import static me.aap.utils.text.TextUtils.appendHexString;
import static me.aap.utils.text.TextUtils.hexToBytes;

/**
 * @author Andrey Pavlenko
 */
class SftpRoot extends SftpFolder {
	private final AtomicReference<ChannelSftp> channel = new AtomicReference<>();
	@NonNull
	final SftpFileSystem fs;
	@NonNull
	final String user;
	@NonNull
	final String host;
	final int port;

	private final FutureRef<Session> session = new FutureRef<Session>() {
		@Override
		protected FutureSupplier<Session> create() {
			return createSession();
		}

		@Override
		protected boolean isValid(FutureSupplier<Session> ref) {
			try {
				return ref.get().isConnected();
			} catch (Exception ex) {
				return false;
			}
		}
	};

	public SftpRoot(@NonNull SftpFileSystem fs, @NonNull String user, @NonNull String host, int port,
									@NonNull String path) {
		super(null, path);
		this.fs = fs;
		this.user = user;
		this.host = host;
		this.port = port;
	}

	@NonNull
	@Override
	SftpRoot getRoot() {
		return this;
	}

	@NonNull
	@Override
	public VirtualFileSystem getVirtualFileSystem() {
		return fs;
	}

	@NonNull
	@Override
	public FutureSupplier<VirtualFolder> getParent() {
		return completedNull();
	}

	private FutureSupplier<Session> createSession() {
		PreferenceStore ps = fs.getPreferenceStore();
		String password;
		String keyFile;
		String keyPass;

		try (SharedTextBuilder tb = SharedTextBuilder.get()) {
			SftpFileSystem.appendUri(tb, user, host, port, null);
			tb.append('#');
			int len = tb.length();

			tb.append('P');
			password = ps.getStringPref(PreferenceStore.Pref.s(tb.toString(), () -> null));
			tb.setLength(len);
			tb.append('F');
			keyFile = ps.getStringPref(PreferenceStore.Pref.s(tb.toString(), () -> null));
			tb.setLength(len);
			tb.append('K');
			keyPass = ps.getStringPref(PreferenceStore.Pref.s(tb.toString(), () -> null));
		}

		return App.get().execute(() -> createSession(user, host, port, decrypt(password),
				keyFile, decrypt(keyPass)));
	}

	void save(PreferenceStore ps, @Nullable String password, @Nullable String keyFile, @Nullable String keyPass) {
		try (SharedTextBuilder tb = SharedTextBuilder.get();
				 PreferenceStore.Edit e = ps.editPreferenceStore()) {
			SftpFileSystem.appendUri(tb, user, host, port, null);
			tb.append('#');
			int len = tb.length();

			if (password != null) {
				encrypt(password, tb);
				password = tb.substring(len);
				tb.setLength(len);
				tb.append('P');
				e.setStringPref(PreferenceStore.Pref.s(tb.toString(), () -> null), password);
			}

			if (keyFile != null) {
				tb.setLength(len);
				tb.append('F');
				e.setStringPref(PreferenceStore.Pref.s(tb.toString(), () -> null), keyFile);
			}

			if (keyPass != null) {
				tb.setLength(len);
				encrypt(keyPass, tb);
				keyPass = tb.substring(len);
				tb.setLength(len);
				tb.append('K');
				e.setStringPref(PreferenceStore.Pref.s(tb.toString(), () -> null), keyPass);
			}
		}
	}

	@Nullable
	static SftpRoot create(@NonNull SftpFileSystem fs, String uri) {
		int i = uri.indexOf('@');
		if (i <= 7) return null;

		String user = uri.substring(7, i);
		String host;

		if (uri.charAt(7) == '[') { // IPv6
			i = uri.indexOf(']', 8);
			if (i == -1) return null;
			host = uri.substring(8, i);
		} else {
			host = null;
		}

		int c = uri.indexOf(':', ++i);
		int s = uri.indexOf('/', i);
		if (s == -1) return null;

		int port;

		if (c == -1) {
			port = 22;
			if (host == null) host = uri.substring(i, s);
		} else {
			if (c >= (s - 1)) return null;
			port = Integer.parseInt(uri.substring(c + 1, s));
			if (host == null) host = uri.substring(i, c);
		}

		String path = uri.substring(s);
		return new SftpRoot(fs, user, host, port, path);
	}

	static FutureSupplier<SftpRoot> create(
			@NonNull SftpFileSystem fs, @NonNull String user, @NonNull String host, int port,
			@Nullable String path, @Nullable String password,
			@Nullable String keyFile, @Nullable String keyPass) {
		return App.get().execute(() -> {
			Session s = createSession(user, host, port, password, keyFile, keyPass);
			ChannelSftp ch = (ChannelSftp) s.openChannel("sftp");
			ch.connect();

			String p = path;
			if (p == null) p = ch.getHome();

			SftpATTRS a = ch.lstat(p);
			if (!a.isDir()) throw new VfsException("Path is not a directory: " + p);

			SftpRoot r = new SftpRoot(fs, user, host, port, p);
			r.session.set(completed(s));
			r.channel.set(ch);
			return r;
		});
	}

	Uri buildUri(@NonNull String path) {
		return SftpFileSystem.buildUri(user, host, port, path);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		return getUri().equals(((SftpRoot) o).getUri());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getUri());
	}

	private FutureSupplier<ChannelSftp> getChannel() {
		return session.get().then(s -> {
			ChannelSftp ch = channel.getAndSet(null);
			if ((ch != null) && !ch.isClosed()) return completed(ch);

			return App.get().execute(() -> {
				ChannelSftp c = (ChannelSftp) s.openChannel("sftp");
				c.connect();
				return c;
			});
		});
	}

	<T> FutureSupplier<T> useChannel(CheckedFunction<ChannelSftp, T, Throwable> task) {
		return Async.retry(() -> getChannel().then(ch -> App.get().execute(() -> {
			try {
				return task.apply(ch);
			} finally {
				if (!channel.compareAndSet(null, ch)) {
					ch.disconnect();
				}
			}
		})));
	}

	private static Session createSession(
			@NonNull String user, @NonNull String host, int port, @Nullable String password,
			@Nullable String keyFile, @Nullable String keyPass) throws JSchException {
		JSch jsch = new JSch();
		if (keyFile != null) jsch.addIdentity(keyFile, keyPass);

		Session s = jsch.getSession(user, host, port);
		if (password != null) s.setPassword(password);

		s.setUserInfo(new UserInfo() {
			@Override
			public String getPassphrase() {
				return keyPass;
			}

			@Override
			public String getPassword() {
				return password;
			}

			@Override
			public boolean promptPassword(String message) {
				return true;
			}

			@Override
			public boolean promptPassphrase(String message) {
				return true;
			}

			@Override
			public boolean promptYesNo(String message) {
				return true;
			}

			@Override
			public void showMessage(String message) {
			}
		});

		s.connect();
		return s;
	}

	// Weak encryption
	private void encrypt(@NonNull String v, SharedTextBuilder tb) {
		appendHexString(tb, v.getBytes(UTF_16BE));
	}

	private String decrypt(@Nullable String v) {
		if (v == null) return v;
		return new String(hexToBytes(v), UTF_16BE);
	}
}
