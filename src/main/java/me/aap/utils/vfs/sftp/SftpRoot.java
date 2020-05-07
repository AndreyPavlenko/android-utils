package me.aap.utils.vfs.sftp;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.UserInfo;

import java.util.Objects;

import me.aap.utils.app.App;
import me.aap.utils.async.Async;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.ObjectPool;
import me.aap.utils.async.ObjectPool.PooledObject;
import me.aap.utils.function.CheckedFunction;
import me.aap.utils.function.IntSupplier;
import me.aap.utils.pref.PreferenceStore;
import me.aap.utils.pref.PreferenceStore.Pref;
import me.aap.utils.text.SharedTextBuilder;
import me.aap.utils.vfs.VfsException;
import me.aap.utils.vfs.VirtualFileSystem;
import me.aap.utils.vfs.VirtualFolder;

import static java.nio.charset.StandardCharsets.UTF_16BE;
import static me.aap.utils.async.Completed.completed;
import static me.aap.utils.async.Completed.completedNull;
import static me.aap.utils.async.Completed.failed;
import static me.aap.utils.text.TextUtils.appendHexString;
import static me.aap.utils.text.TextUtils.hexToBytes;

/**
 * @author Andrey Pavlenko
 */
class SftpRoot extends SftpFolder {
	private final SessionPool pool;

	public SftpRoot(@NonNull SessionPool pool, @NonNull String path) {
		super(null, path);
		this.pool = pool;
	}

	@NonNull
	@Override
	SftpRoot getRoot() {
		return this;
	}

	@NonNull
	@Override
	public VirtualFileSystem getVirtualFileSystem() {
		return pool.fs;
	}

	@NonNull
	@Override
	public FutureSupplier<VirtualFolder> getParent() {
		return completedNull();
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
		return new SftpRoot(new SessionPool(fs, user, host, port), path);
	}

	static FutureSupplier<SftpRoot> create(
			@NonNull SftpFileSystem fs, @NonNull String user, @NonNull String host, int port,
			@Nullable String path, @Nullable String password,
			@Nullable String keyFile, @Nullable String keyPass) {
		SessionPool pool = new SessionPool(fs, user, host, port, password, keyFile, keyPass);

		return pool.getObject().closeableThen(session -> {
			try {
				ChannelSftp ch = session.get().channel;
				String p = path;
				if (p == null) p = ch.getHome();

				SftpATTRS a = ch.lstat(p);
				if (!a.isDir()) throw new VfsException("Path is not a directory: " + p);

				pool.saveCredentials();
				return completed(new SftpRoot(pool, p));
			} catch (Throwable ex) {
				pool.close();
				return failed(ex);
			}
		});
	}

	String getUser() {
		return pool.user;
	}

	String getHost() {
		return pool.host;
	}

	int getPort() {
		return pool.port;
	}

	Uri buildUri(@NonNull String path) {
		return SftpFileSystem.buildUri(pool.user, pool.host, pool.port, path);
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

	FutureSupplier<PooledObject<SftpSession>> getSession() {
		return pool.getObject();
	}

	<T> FutureSupplier<T> useChannel(CheckedFunction<ChannelSftp, T, Throwable> task) {
		return Async.retry(() -> getSession().then(session -> App.get().execute(() -> {
			try {
				return task.apply(session.get().getChannel());
			} finally {
				session.release();
			}
		})));
	}

	private static class SessionPool extends ObjectPool<SftpSession> {
		private static final Pref<IntSupplier> MAX_SESSIONS = Pref.i("SFTP_MAX_SESSIONS", 3);
		final SftpFileSystem fs;
		@NonNull
		final String user;
		@NonNull
		final String host;
		final int port;
		@Nullable
		private final String password;
		@Nullable
		private final String keyFile;
		@Nullable
		private final String keyPass;

		public SessionPool(SftpFileSystem fs, @NonNull String user, @NonNull String host,
											 int port, @Nullable String password, @Nullable String keyFile,
											 @Nullable String keyPass) {
			super(fs.getPreferenceStore().getIntPref(MAX_SESSIONS));
			this.fs = fs;
			this.user = user;
			this.host = host;
			this.port = port;
			this.password = password;
			this.keyFile = keyFile;
			this.keyPass = keyPass;
		}

		public SessionPool(SftpFileSystem fs, @NonNull String user, @NonNull String host, int port) {
			super(fs.getPreferenceStore().getIntPref(MAX_SESSIONS));
			PreferenceStore ps = fs.getPreferenceStore();
			String password;
			String keyFile;
			String keyPass;

			try (SharedTextBuilder tb = SharedTextBuilder.get()) {
				SftpFileSystem.appendUri(tb, user, host, port, null);
				tb.append('#');
				int len = tb.length();

				tb.append('P');
				password = ps.getStringPref(Pref.s(tb.toString(), () -> null));
				tb.setLength(len);
				tb.append('F');
				keyFile = ps.getStringPref(Pref.s(tb.toString(), () -> null));
				tb.setLength(len);
				tb.append('K');
				keyPass = ps.getStringPref(Pref.s(tb.toString(), () -> null));
			}


			this.fs = fs;
			this.user = user;
			this.host = host;
			this.port = port;
			this.keyFile = keyFile;
			this.password = decrypt(password);
			this.keyPass = decrypt(keyPass);
		}

		void saveCredentials() {
			try (SharedTextBuilder tb = SharedTextBuilder.get();
					 PreferenceStore.Edit e = fs.getPreferenceStore().editPreferenceStore()) {
				SftpFileSystem.appendUri(tb, user, host, port, null);
				tb.append('#');
				int len = tb.length();

				if (password != null) {
					encrypt(password, tb);
					String password = tb.substring(len);
					tb.setLength(len);
					tb.append('P');
					e.setStringPref(Pref.s(tb.toString(), () -> null), password);
				}

				if (keyFile != null) {
					tb.setLength(len);
					tb.append('F');
					e.setStringPref(Pref.s(tb.toString(), () -> null), keyFile);
				}

				if (keyPass != null) {
					tb.setLength(len);
					encrypt(keyPass, tb);
					String keyPass = tb.substring(len);
					tb.setLength(len);
					tb.append('K');
					e.setStringPref(Pref.s(tb.toString(), () -> null), keyPass);
				}
			}
		}

		@Override
		protected FutureSupplier<SftpSession> createObject() {
			return App.get().execute(this::createSession);
		}

		private SftpSession createSession() throws JSchException {
			Session s = null;
			SftpSession session = null;

			try {
				JSch jsch = new JSch();
				if (keyFile != null) jsch.addIdentity(keyFile, keyPass);

				s = jsch.getSession(user, host, port);
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
				ChannelSftp ch = (ChannelSftp) s.openChannel("sftp");
				ch.connect();
				return session = new SftpSession(s, ch);
			} finally {
				if ((session == null) && (s != null)) s.disconnect();
			}
		}

		@Override
		protected boolean validateObject(SftpSession session, boolean releasing) {
			try {
				return session.isValid();
			} catch (Throwable ex) {
				Log.d(getClass().getName(), "Session is not valid", ex);
				return false;
			}
		}

		@Override
		protected void destroyObject(SftpSession session) {
			session.close();
		}

		// Weak encryption
		private static void encrypt(@NonNull String v, SharedTextBuilder tb) {
			appendHexString(tb, v.getBytes(UTF_16BE));
		}

		private static String decrypt(@Nullable String v) {
			if (v == null) return v;
			return new String(hexToBytes(v), UTF_16BE);
		}

	}

	static final class SftpSession implements AutoCloseable {
		private final Session session;
		private final ChannelSftp channel;

		SftpSession(Session session, ChannelSftp channel) {
			this.session = session;
			this.channel = channel;
		}

		ChannelSftp getChannel() {
			return channel;
		}

		boolean isValid() {
			try {
				if (!channel.isConnected()) return false;
				session.sendKeepAliveMsg();
				return true;
			} catch (Throwable ignore) {
				return false;
			}
		}

		@Override
		public void close() {
			try {
				session.disconnect();
			} catch (Throwable ignore) {
			}
			try {
				channel.disconnect();
			} catch (Throwable ignore) {
			}
		}
	}
}
