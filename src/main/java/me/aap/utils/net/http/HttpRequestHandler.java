package me.aap.utils.net.http;

import java.nio.ByteBuffer;

import me.aap.utils.net.NetChannel;

/**
 * @author Andrey Pavlenko
 */
public interface HttpRequestHandler {

	void handleRequest(NetChannel channel, HttpRequest req, ByteBuffer payload);

	interface Provider {
		HttpRequestHandler getHandler(CharSequence path, HttpMethod method, HttpVersion version);
	}
}
