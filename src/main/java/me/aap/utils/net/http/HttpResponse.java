package me.aap.utils.net.http;

import java.nio.ByteBuffer;

import me.aap.utils.net.ByteBufferSupplier;
import me.aap.utils.net.NetChannel;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * @author Andrey Pavlenko
 */
@SuppressWarnings("ALL")
public interface HttpResponse {

	void write(NetChannel channel);

	static class StaticResponse implements HttpResponse, ByteBufferSupplier {
		private final byte[] data;

		public StaticResponse(byte[] data) {
			this.data = data;
		}

		@Override
		public ByteBuffer getByteBuffer() {
			return ByteBuffer.wrap(data);
		}

		@Override
		public final void write(NetChannel channel) {
			channel.write(this).thenRun(channel::close);
		}
	}

	static final class BadRequest {
		public static final HttpResponse instance = new StaticResponse(("HTTP/1.1 400 Bad Request\r\n" +
				"Connection: close\r\n" +
				"Content-Length: 0\r\n\r\n").getBytes(US_ASCII)
		);
	}

	static final class NotFound {
		public static final HttpResponse instance = new StaticResponse(("HTTP/1.1 404 Not Found\r\n" +
				"Connection: close\r\n" +
				"Content-Length: 0\r\n\r\n").getBytes(US_ASCII)
		);
	}

	static final class MethodNotAllowed {
		public static final HttpResponse instance = new StaticResponse(("HTTP/1.1 405 Method Not Allowed\r\n" +
				"Connection: close\r\n" +
				"Content-Length: 0\r\n\r\n").getBytes(US_ASCII)
		);
	}

	static final class PayloadTooLarge {
		public static final HttpResponse instance = new StaticResponse(("HTTP/1.1 413 Payload Too Large\r\n" +
				"Connection: close\r\n" +
				"Content-Length: 0\r\n\r\n").getBytes(US_ASCII)
		);
	}

	static final class UriTooLong {
		public static final HttpResponse instance = new StaticResponse(("HTTP/1.1 414 URI Too Long\r\n" +
				"Connection: close\r\n" +
				"Content-Length: 0\r\n\r\n").getBytes(US_ASCII)
		);
	}

	static final class ServerError {
		public static final HttpResponse instance = new StaticResponse(("HTTP/1.1 500 Internal Server Error\r\n" +
				"Connection: close\r\n" +
				"Content-Length: 0\r\n\r\n").getBytes(US_ASCII)
		);
	}

	static final class ServiceUnavailable {
		public static final HttpResponse instance = new StaticResponse(("HTTP/1.1 503 Service Unavailable\r\n" +
				"Connection: close\r\n" +
				"Content-Length: 0\r\n\r\n").getBytes(US_ASCII)
		);
	}

	static final class VersionNotSupported {
		public static final HttpResponse instance = new StaticResponse(("HTTP/1.1 505 HTTP Version Not Supported\r\n" +
				"Connection: close\r\n" +
				"Content-Length: 0\r\n\r\n").getBytes(US_ASCII)
		);
	}
}
