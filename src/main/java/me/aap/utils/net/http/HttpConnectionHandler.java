package me.aap.utils.net.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.aap.utils.BuildConfig;
import me.aap.utils.concurrent.NetThread;
import me.aap.utils.log.Log;
import me.aap.utils.net.ByteBufferSupplier;
import me.aap.utils.net.NetChannel;
import me.aap.utils.net.NetServer;
import me.aap.utils.net.http.HttpError.BadRequest;
import me.aap.utils.net.http.HttpError.MethodNotAllowed;
import me.aap.utils.net.http.HttpError.NotFound;
import me.aap.utils.net.http.HttpError.PayloadTooLarge;
import me.aap.utils.net.http.HttpError.UriTooLong;
import me.aap.utils.net.http.HttpError.VersionNotSupported;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static me.aap.utils.io.IoUtils.emptyByteBuffer;
import static me.aap.utils.misc.Assert.assertEquals;
import static me.aap.utils.net.http.HttpUtils.parseLong;
import static me.aap.utils.net.http.HttpVersion.HTTP_1_0;

/**
 * @author Andrey Pavlenko
 */
public class HttpConnectionHandler implements NetServer.ConnectionHandler, ByteBufferSupplier {
	private final Map<CharSequence, HttpRequestHandler.Provider> handlers = new ConcurrentHashMap<>();

	public HttpConnectionHandler() {
	}

	public HttpRequestHandler.Provider addHandler(String path, HttpRequestHandler.Provider provider) {
		return handlers.put(path, provider);
	}

	public HttpRequestHandler.Provider removeHandler(String path) {
		return handlers.remove(path);
	}

	@Override
	public ByteBuffer getByteBuffer() {
		Thread t = Thread.currentThread();
		if (t instanceof NetThread) return ((NetThread) t).getRequestBuffer();
		return ByteBuffer.allocate(4096);
	}

	@Override
	public void retainByteBuffer(ByteBuffer bb) {
		if (BuildConfig.DEBUG) {
			assertLocalBuffer(bb);
			assertEquals(0, bb.position());
			assertEquals(bb.capacity(), bb.limit());
		}
	}

	private void assertLocalBuffer(ByteBuffer bb) {
		if (BuildConfig.DEBUG && (bb != null)) {
			Thread t = Thread.currentThread();
			if (t instanceof NetThread) ((NetThread) t).assertRequestBuffer(bb);
		}
	}

	@Override
	public void acceptConnection(NetChannel channel) {
		channel.read(this, (bb, fail) -> {
			assertLocalBuffer(bb);
			read(channel, bb, fail);
		});
	}

	private void read(NetChannel channel, ByteBuffer bb, Throwable fail) {
		for (; ; ) {
			if (!readReq(channel, bb, fail) || !channel.isOpen()) return;

			if (!bb.hasRemaining()) {
				acceptConnection(channel);
				return;
			}
		}
	}

	private boolean readReq(NetChannel channel, ByteBuffer bb, Throwable fail) {
		if (fail != null) {
			if (channel.isOpen()) {
				channel.close();
				Log.d(fail, "Failed to read HTTP request");
			}
			return false;
		}

		int start = bb.position();
		int end = bb.limit();

		if (start == end) { // End of stream
			Log.d("HTTP Stream closed");
			channel.close();
			return false;
		}

		byte[] buf = bb.array();
		HttpMethod method = HttpMethod.get(buf, start, end);

		if (method == null) {
			incompleteReq(channel, buf, start, end);
			return false;
		} else if (method == HttpMethod.UNSUPPORTED) {
			MethodNotAllowed.instance.write(channel);
			return false;
		}

		int off = start + method.length();

		if (off == end) {
			incompleteReq(channel, buf, start, end);
			return false;
		}

		if (buf[off] != ' ') {
			BadRequest.instance.write(channel);
			return false;
		}

		int hash = 0;
		int uriStart = ++off;
		int pathEnd = 0;
		int uriEnd = 0;

		for (; off < end; off++) {
			char c = (char) buf[off];

			if (c == '?') {
				pathEnd = off++;
				break;
			} else if (c == ' ') {
				uriEnd = pathEnd = off++;
				break;
			} else {
				hash = 31 * hash + c;
			}
		}

		if (uriEnd == 0) {
			for (; off < end; off++) {
				if (buf[off] == ' ') {
					uriEnd = off++;
					break;
				}
			}
		}

		if (uriEnd == 0) {
			incompleteReq(channel, buf, start, end);
			return false;
		}

		HttpVersion version = HttpVersion.get(buf, off, end);

		if (version == null) {
			incompleteReq(channel, buf, start, end);
			return false;
		} else if (version == HttpVersion.UNSUPPORTED) {
			VersionNotSupported.instance.write(channel);
			return false;
		}

		for (off++; off < end; off++) {
			if (buf[off] == '\n') {
				Req req = new Req(buf, uriStart, uriEnd, pathEnd, hash, method, version, ++off);
				HttpRequestHandler.Provider p = handlers.get(req);
				HttpRequestHandler handler;

				if ((p == null) || ((handler = p.getHandler(req, method, version)) == null)) {
					NotFound.instance.write(channel);
					return false;
				}

				if (off < end) {
					return readHeaders(channel, bb, null, handler, req, off, false);
				} else if ((off == buf.length) && (start == 0)) {
					UriTooLong.instance.write(channel);
					return false;
				} else {
					int o = off - start;
					req.uriStart -= start;
					req.headerStart -= start;
					channel.read(retainBuf(buf, start, end)).onCompletion((b, f) ->
							readHeaders(channel, b, f, handler, req, o, true));
					return false;
				}
			}
		}

		incompleteReq(channel, buf, start, end);
		return false;
	}

	private static final byte[] H_CONNECTION = "oOnNnNeEcCtTiIoOnN".getBytes(US_ASCII);
	private static final byte[] H_CONNECTION_CLOSE = "cClLoOsSeE".getBytes(US_ASCII);
	private static final byte[] H_CONNECTION_KEEP = "KkeEeEpP--AalLiIvVeE".getBytes(US_ASCII);
	private static final byte[] H_CONTENT_LEN = "oOnNtTeEnNtT-LleEnNgGtThH".getBytes(US_ASCII);
	private static final byte[] H_RANGE = "aAnNgGeE".getBytes(US_ASCII);

	private boolean readHeaders(NetChannel channel, ByteBuffer bb, Throwable fail,
															HttpRequestHandler handler, Req req, int off, boolean read) {
		if (fail != null) {
			if (channel.isOpen()) {
				channel.close();
				Log.d(fail, "Failed to read HTTP request");
			}
			return false;
		}

		int start = bb.position();
		int end = bb.limit();

		if (start == end) { // End of stream
			Log.d("HTTP Stream closed");
			channel.close();
			return false;
		}

		byte[] buf = bb.array();

		loop:
		for (int i = off; i < end; ) {
			switch (buf[i]) {
				case 'C':
				case 'c':
					int value = headerMatch(H_CONNECTION, buf, i + 1, end);

					if (value < 0) {
						if (value == Integer.MIN_VALUE) break loop;

						i = -value;
						value = valueMatch(H_CONNECTION_CLOSE, buf, i, end);

						if (value < 0) {
							if (value == Integer.MIN_VALUE) break loop;
							req.connectionClose = 1;
							i = -value;
							break;
						} else {
							value = valueMatch(H_CONNECTION_KEEP, buf, i, end);

							if (value < 0) {
								if (value == Integer.MIN_VALUE) break loop;
								req.connectionClose = 2;
								i = -value;
								break;
							}
						}

						i = value;
						break;
					}

					value = headerMatch(H_CONTENT_LEN, buf, i + 1, end);

					if (value < 0) {
						if (value == Integer.MIN_VALUE) break loop;
						i = -value;
						req.contentLen = -(i - req.headerStart);
						break;
					}

					i = value;
					break;
				case 'R':
				case 'r':
					value = headerMatch(H_RANGE, buf, i + 1, end);

					if (value < 0) {
						if (value == Integer.MIN_VALUE) break loop;
						i = -value;
						req.rangeOff = (i - req.headerStart);
						break;
					}

					i = value;
					break;
			}

			for (; i < end; i++) {
				if (buf[i] != '\n') continue;

				if (i < (end - 1)) {
					if (buf[i + 1] != '\n') {
						if (buf[i + 1] == '\r') {
							if (i < (end - 2)) {
								if (buf[i += 2] != '\n') {
									off = i;
									continue loop;
								}
							} else {
								off = i;
								break loop;
							}
						} else {
							off = ++i;
							continue loop;
						}
					} else {
						i++;
					}
				} else {
					off = i;
					break loop;
				}

				bb.position(i + 1);
				req.buf = buf;
				req.headerEnd = i + 1;

				long contentLen = req.getContentLength();

				if (contentLen > 0) {
					int limit = bb.limit();
					bb.limit((int) Math.min(limit, bb.position() + contentLen));
					handler.handleRequest(channel, req, bb);
					bb.position(bb.limit()).limit(limit);
				} else {
					handler.handleRequest(channel, req, emptyByteBuffer());
				}

				if (req.closeConnection()) {
					return false;
				} else {
					if (read) {
						if (bb.hasRemaining()) read(channel, bb, null);
						else if (channel.isOpen()) acceptConnection(channel);
					}

					return true;
				}
			}
		}

		if ((off == buf.length) && (start == 0)) {
			PayloadTooLarge.instance.write(channel);
		} else {
			req.uriStart -= start;
			req.headerStart -= start;
			int o = off - start;
			channel.read(retainBuf(buf, start, end)).onCompletion((b, f) ->
					readHeaders(channel, b, f, handler, req, o, true));
		}

		return false;
	}

	private static int headerMatch(byte[] header, byte[] buf, int start, int end) {
		for (int h = 0; (start < end) && (h < header.length); start++, h += 2) {
			if ((buf[start] != header[h]) && (buf[start] != header[h + 1])) return start;
		}

		if (start == end) return Integer.MIN_VALUE;
		if (buf[start] != ':') return start;
		if (++start == end) return Integer.MIN_VALUE;
		if ((buf[start] != ' ') && (buf[start] != '\t')) return start;

		for (start++; start < end; start++) {
			if ((buf[start] != ' ') && (buf[start] != '\t')) {
				return ((buf[start] != '\r') && (buf[start] != '\n')) ? -start : start;
			}
		}

		return Integer.MIN_VALUE;
	}

	private static int valueMatch(byte[] value, byte[] buf, int start, int end) {
		for (int h = 0; (start < end) && (h < value.length); start++, h += 2) {
			if ((buf[start] != value[h]) && (buf[start] != value[h + 1])) return start;
		}

		return (start != end) ? -start : Integer.MIN_VALUE;
	}

	private void incompleteReq(NetChannel channel, byte[] buf, int start, int end) {
		if ((end == buf.length) && (start == 0)) {
			UriTooLong.instance.write(channel);
		} else {
			channel.read(retainBuf(buf, start, end)).onCompletion((b, f) -> read(channel, b, f));
		}
	}

	private static ByteBufferSupplier retainBuf(byte[] buf, int start, int end) {
		byte[] b = Arrays.copyOfRange(buf, start, end);
		int off = end - start;
		int newLen = buf.length;
		return () -> {
			ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOf(b, newLen));
			bb.position(off);
			return bb;
		};
	}

	private static final class Req implements HttpRequest, CharSequence {
		byte[] buf;
		int uriStart;
		final int uriLen;
		final int pathLen;
		final int hash;
		final HttpMethod method;
		final HttpVersion version;
		int headerStart;
		int headerEnd;
		int rangeOff;
		long contentLen;
		byte connectionClose;

		public Req(byte[] buf, int uriStart, int uriEnd, int pathEnd, int hash, HttpMethod method,
							 HttpVersion version, int headerStart) {
			this.buf = buf;
			this.uriStart = uriStart;
			this.uriLen = uriEnd - uriStart;
			this.pathLen = pathEnd - uriStart;
			this.hash = hash;
			this.method = method;
			this.version = version;
			this.headerStart = headerStart;
		}

		@Override
		public long getContentLength() {
			long len = contentLen;
			if (len < 0) {
				contentLen = len = parseLong(buf, (int) (headerStart + -len), headerEnd, "\n\r\t ", 0);
			}
			return len;
		}

		@Nullable
		@Override
		public Range getRange() {
			return (rangeOff == 0) ? null : Range.parse(buf, headerStart + rangeOff, headerEnd);

		}

		@Override
		public boolean closeConnection() {
			return (connectionClose == 1) || ((version == HTTP_1_0) && ((connectionClose != 2)));
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		@Override
		public boolean equals(Object o) {
			CharSequence s = (CharSequence) o;
			int len = length();
			if (len != s.length()) return false;

			for (int i = 0, b = uriStart; i < len; i++, b++) {
				if (s.charAt(i) != ((char) buf[b])) return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public int length() {
			return pathLen;
		}

		@Override
		public char charAt(int index) {
			return (char) buf[uriStart + index];
		}

		@NonNull
		@Override
		public CharSequence subSequence(int start, int end) {
			return new AsciiSeq(buf, uriStart + start, end - start);
		}

		@Override
		public HttpMethod getMethod() {
			return method;
		}

		@Override
		public HttpVersion getVersion() {
			return version;
		}

		@Override
		public CharSequence getUri() {
			if (uriLen == pathLen) return this;
			return new AsciiSeq(buf, uriStart, uriLen);
		}

		public CharSequence getPath() {
			return this;
		}

		@Override
		public CharSequence getQuery() {
			if (uriLen == pathLen) return null;
			return new AsciiSeq(buf, uriStart + pathLen + 1, uriLen - pathLen - 1);
		}

		@Override
		public CharSequence getHeaders() {
			return new AsciiSeq(buf, headerStart, headerEnd - headerStart);
		}

		@Override
		public String toString() {
			return new String(buf, uriStart - method.length() - 1, headerEnd);
		}
	}

	private static final class AsciiSeq implements CharSequence {
		private final byte[] chars;
		private final int off;
		private final int len;

		AsciiSeq(byte[] chars, int off, int len) {
			this.chars = chars;
			this.off = off;
			this.len = len;
		}

		@Override
		public int length() {
			return len;
		}

		@Override
		public char charAt(int index) {
			return (char) chars[off + index];
		}

		@NonNull
		@Override
		public CharSequence subSequence(int start, int end) {
			return new AsciiSeq(chars, off + start, end - start);
		}

		@Override
		public String toString() {
			return new String(chars, off, len);
		}
	}
}
