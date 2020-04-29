package me.aap.utils.net;

import java.io.Closeable;
import java.net.SocketAddress;

/**
 * @author Andrey Pavlenko
 */
public interface NetServer extends Closeable {

	SocketAddress getBindAddress();

	@Override
	void close();
}
