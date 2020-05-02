package me.aap.utils.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.function.Consumer;
import me.aap.utils.net.NetServer.ConnectionHandler;

/**
 * @author Andrey Pavlenko
 */
public interface NetHandler extends Closeable {

	static NetHandler create(Executor executor) throws IOException {
		SelectorHandler h = new SelectorHandler(executor);
		h.start();
		return h;
	}

	Executor getExecutor();

	boolean isOpen();

	@Override
	void close();

	FutureSupplier<NetServer> bind(BindOpts opts);

	default FutureSupplier<NetServer> bind(Consumer<BindOpts> c) {
		BindOpts o = new BindOpts();
		c.accept(o);
		return bind(o);
	}

	FutureSupplier<NetChannel> connect(ConnectOpts opts);

	default FutureSupplier<NetChannel> connect(Consumer<ConnectOpts> c) {
		ConnectOpts o = new ConnectOpts();
		c.accept(o);
		return connect(o);
	}

	class Opts {
		public Map<SocketOption<?>, Object> opt = new HashMap<>();
		public SocketAddress address;
		public String host;
		public int port;

		SocketAddress getAddress() {
			if (address == null) {
				return (host == null) ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
			} else {
				return address;
			}
		}
	}

	class BindOpts extends Opts {
		public ConnectionHandler handler;
		public int backlog;
	}

	class ConnectOpts extends Opts {
		public SocketAddress bindAddress;
		public String bindHost;
		public int bindPort;

		SocketAddress getBindAddress() {
			if (bindAddress != null) {
				return bindAddress;
			} else if (bindHost != null) {
				return new InetSocketAddress(bindHost, bindPort);
			} else if (bindPort != 0) {
				return new InetSocketAddress(bindPort);
			} else {
				return null;
			}
		}
	}
}
