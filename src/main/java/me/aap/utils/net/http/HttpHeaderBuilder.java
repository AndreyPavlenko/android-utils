package me.aap.utils.net.http;

import java.nio.ByteBuffer;

import me.aap.utils.concurrent.NetThread;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static me.aap.utils.net.http.HttpVersion.HTTP_1_1;

/**
 * @author Andrey Pavlenko
 */
public class HttpHeaderBuilder {
	private static final byte[] EOL = new byte[]{(byte) '\r', (byte) '\n'};
	private static final byte[] SEP = new byte[]{(byte) ':', (byte) ' '};
	private static final byte[] OK = " 200 OK\r\n".getBytes(US_ASCII);
	private static final byte[] PART = " 206 Partial content\r\n".getBytes(US_ASCII);
	private ByteBuffer buf;
	private int pos;
	private boolean local;

	public HttpHeaderBuilder() {
		Thread t = Thread.currentThread();

		if (t instanceof NetThread) {
			buf = ((NetThread) t).getResponseBuffer();
			pos = buf.position();
			local = true;
		} else {
			buf = ByteBuffer.allocate(1024);
			pos = 0;
			local = false;
		}
	}

	public HttpHeaderBuilder(ByteBuffer buf) {
		this.buf = buf;
		pos = buf.position();
		local = false;
	}

	public HttpHeaderBuilder addLine(CharSequence line) {
		int len = line.length();
		ensureCapacity(len + 2);
		append(line, len);
		buf.put(EOL);
		return this;
	}

	public HttpHeaderBuilder reqLine(HttpMethod m, CharSequence uri) {
		return reqLine(m, uri, HTTP_1_1);
	}

	public HttpHeaderBuilder reqLine(HttpMethod m, CharSequence uri, HttpVersion version) {
		int uriLen = uri.length();
		ensureCapacity(uriLen + m.length() + version.length() + 4);
		buf.put(m.bytes).put((byte) ' ');
		append(uri, uriLen);
		buf.put((byte) ' ').put(version.bytes).put(EOL);
		return this;
	}

	public HttpHeaderBuilder statusOk(HttpVersion version) {
		ensureCapacity(version.bytes.length + OK.length);
		buf.put(version.bytes).put(OK);
		return this;
	}

	public HttpHeaderBuilder statusPartial(HttpVersion version) {
		ensureCapacity(version.bytes.length + PART.length);
		buf.put(version.bytes).put(PART);
		return this;
	}

	public HttpHeaderBuilder statusLine(HttpVersion version, CharSequence status) {
		int statusLen = status.length();
		ensureCapacity(statusLen + version.length() + 3);
		buf.put(version.bytes).put((byte) ' ');
		append(status, statusLen);
		buf.put(EOL);
		return this;
	}

	public HttpHeaderBuilder addHeader(CharSequence name, CharSequence value) {
		int nameLen = name.length();
		int valueLen = value.length();
		ensureCapacity(nameLen + valueLen + 4);
		append(name, nameLen);
		buf.put(SEP);
		append(value, valueLen);
		buf.put(EOL);
		return this;
	}

	public ByteBuffer build() {
		ByteBuffer buf = this.buf;
		ensureCapacity(2);
		buf.put(EOL);
		buf.limit(buf.position()).position(pos);

		if (local) {
			byte[] hdr = new byte[buf.remaining()];
			buf.get(hdr);
			return ByteBuffer.wrap(hdr);
		} else {
			return buf;
		}
	}

	private void append(CharSequence str, int len) {
		ByteBuffer buf = this.buf;

		if (buf.hasArray()) {
			byte[] b = buf.array();
			for (int i = 0, off = buf.arrayOffset() + buf.position(); i < len; i++) {
				b[off + i] = (byte) str.charAt(i);
			}
			buf.position(buf.position() + len);
		} else {
			for (int i = 0; i < len; i++) {
				buf.put((byte) str.charAt(i));
			}
		}
	}

	private void ensureCapacity(int len) {
		if (buf.remaining() - 1 < len) {
			ByteBuffer b = ByteBuffer.allocate(buf.capacity() << 1);
			buf.limit(buf.position()).position(pos);
			b.put(buf);
			buf = b;
			pos = 0;
			local = false;
		}
	}
}
