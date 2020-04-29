package me.aap.utils.net;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Keep;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import me.aap.utils.async.Completable;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.Promise;
import me.aap.utils.async.RunnablePromise;
import me.aap.utils.function.CheckedConsumer;
import me.aap.utils.function.Supplier;
import me.aap.utils.io.IoUtils;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.util.Objects.requireNonNull;
import static me.aap.utils.async.Completed.failed;
import static me.aap.utils.misc.Assert.assertEquals;
import static me.aap.utils.misc.Assert.assertNull;

/**
 * @author Andrey Pavlenko
 */
class SelectorHandler implements NetHandler, Runnable {
	private final Executor executor;
	private final Selector selector;
	private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();

	public SelectorHandler(Executor executor) throws IOException {
		this.executor = executor;
		selector = Selector.open();
	}

	public void start() {
		Thread t = new Thread(this, "SelectorHandler");
		t.setDaemon(true);
		t.start();
	}

	@Override
	protected void finalize() {
		close();
	}

	@Override
	public void run() {
		while (selector.isOpen()) {
			try {
				selector.select();

				for (Runnable run = queue.poll(); run != null; run = queue.poll()) {
					run.run();
				}

				Set<SelectionKey> keys = selector.selectedKeys();

				for (Iterator<SelectionKey> it = keys.iterator(); it.hasNext(); ) {
					SelectionKey k = it.next();
					it.remove();
					Selectable select = (Selectable) k.attachment();
					if (select != null) select.select();
				}
			} catch (Throwable ex) {
				if (!selector.isOpen()) break;
				Log.e(getClass().getName(), "Selector failed", ex);
			}
		}
	}

	@Override
	public FutureSupplier<NetServer> bind(BindOpts opts) {
		try {
			ServerSocketChannel channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
			SelectableNetServer server = new SelectableNetServer(channel, opts);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				channel.bind(opts.getAddress(), opts.backlog);
			} else {
				channel.socket().bind(opts.getAddress(), opts.backlog);
			}

			RunnablePromise<NetServer> p = new RunnablePromise<NetServer>() {
				@Override
				protected NetServer runTask() {
					return server;
				}
			};

			queue.add(() -> {
				try {
					channel.register(selector, OP_ACCEPT, server);
					execute(p, p);
				} catch (Throwable ex) {
					execute(() -> p.completeExceptionally(ex), p);
				}
			});

			selector.wakeup();
			return p;
		} catch (Throwable ex) {
			return failed(ex);
		}
	}

	@Override
	public FutureSupplier<NetChannel> connect(ConnectOpts o) {
		try {
			SocketAddress addr = o.getAddress();
			SocketAddress bindAddr = o.getBindAddress();
			SocketChannel ch = SocketChannel.open();
			ch.configureBlocking(false);
			setOpts(ch, o.opt);

			if (bindAddr != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					ch.bind(bindAddr);
				} else {
					ch.socket().bind(bindAddr);
				}
			}

			Promise<NetChannel> p = new Promise<NetChannel>() {
				@Override
				public boolean cancel(boolean mayInterruptIfRunning) {
					if (!super.cancel(mayInterruptIfRunning)) return false;
					IoUtils.close(ch);
					return true;
				}
			};

			ch.connect(addr);

			queue.add(() -> {
				try {
					SelectionKey key = ch.register(selector, OP_CONNECT);
					SelectableNetChannel nc = new SelectableNetChannel(key);
					key.attach((Selectable) () -> {
						try {
							assertEquals(OP_CONNECT, key.interestOps());
							if (!key.isConnectable() || !ch.finishConnect()) return;
							key.attach(nc);
							key.interestOps(0);
							execute(() -> p.complete(nc), p);
						} catch (Throwable ex) {
							execute(() -> p.completeExceptionally(ex), p);
						}
					});
				} catch (Throwable ex) {
					execute(() -> p.completeExceptionally(ex), p);
				}
			});

			selector.wakeup();
			return p;
		} catch (Throwable ex) {
			return failed(ex);
		}
	}

	@Override
	public void close() {
		for (SelectionKey k : selector.keys()) {
			try {
				Object a = k.attachment();
				if (a instanceof Closeable) ((Closeable) a).close();
				else k.channel().close();
			} catch (Throwable ignore) {
			}
		}

		IoUtils.close(selector);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void setOpts(SocketChannel ch, Map<SocketOption<?>, ?> opts) throws IOException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			for (Map.Entry<SocketOption<?>, ?> e : opts.entrySet()) {
				ch.setOption((SocketOption) e.getKey(), e.getValue());
			}
		}
	}

	private void selectorRun(Runnable run) {
		queue.add(run);
		selector.wakeup();
	}

	private void execute(Runnable run, Completable<?> c) {
		try {
			executor.execute(run);
		} catch (Throwable ex) {
			c.completeExceptionally(ex);
		}
	}

	private interface Selectable {
		void select();
	}

	private final class SelectableNetServer implements NetServer, Selectable {
		private final ServerSocketChannel channel;
		private final Map<SocketOption<?>, ?> opts;
		private final CheckedConsumer<NetChannel, Throwable> handler;

		public SelectableNetServer(ServerSocketChannel channel, BindOpts o) {
			this.channel = channel;
			opts = o.opt.isEmpty() ? Collections.emptyMap() : o.opt;
			handler = requireNonNull(o.handler);
		}

		@Override
		public SocketAddress getBindAddress() {
			return channel.socket().getLocalSocketAddress();
		}

		@Override
		public void select() {
			SelectableNetChannel nc;
			SocketChannel ch = null;

			try {
				ch = channel.accept();
				if (ch == null) return;

				ch.configureBlocking(false);
				setOpts(ch, opts);

				SelectionKey key = ch.register(selector, 0);
				nc = new SelectableNetChannel(key);
				key.attach(nc);
			} catch (Throwable ex) {
				IoUtils.close(ch);
				Log.e(getClass().getName(), "Failed to accept a  connection", ex);
				return;
			}

			try {
				executor.execute(() -> {
					try {
						handler.accept(nc);
					} catch (Throwable ex) {
						Log.e(getClass().getName(), "Connection handler failed", ex);
						nc.close();
					}
				});
			} catch (Throwable ex) {
				Log.e(getClass().getName(), "Failed to execute connection handler", ex);
				nc.close();
			}
		}

		@Override
		public void close() {
			try {
				channel.close();
			} catch (Throwable ex) {
				Log.e(getClass().getName(), "Failed to close server channel", ex);
			}
		}
	}

	private static final AtomicReferenceFieldUpdater<SelectableNetChannel, SelectableNetChannel.ReadPromise> reader =
			AtomicReferenceFieldUpdater.newUpdater(SelectableNetChannel.class, SelectableNetChannel.ReadPromise.class, "readerHolder");
	private static final AtomicReferenceFieldUpdater<SelectableNetChannel, SelectableNetChannel.WritePromise> writer =
			AtomicReferenceFieldUpdater.newUpdater(SelectableNetChannel.class, SelectableNetChannel.WritePromise.class, "writerHolder");

	private final class SelectableNetChannel implements NetChannel, Selectable {
		private final SelectionKey key;

		@Keep
		@SuppressWarnings("unused")
		volatile ReadPromise readerHolder;
		@Keep
		@SuppressWarnings("unused")
		volatile WritePromise writerHolder;

		public SelectableNetChannel(SelectionKey key) {
			this.key = key;
		}

		@Override
		public void select() {
			try {
				assertEquals(0, key.interestOps() & (OP_ACCEPT | OP_CONNECT));
				int ready = key.readyOps();

				if ((ready & OP_READ) != 0) {
					ReadPromise p = reader.get(this);

					if (p != null) {
						ByteBuffer b = p.getBuf();

						if (b != null) {
							key.interestOps(key.interestOps() & ~OP_READ);
							execute(() -> read(p, b), p);
						}
					}
				}

				if ((ready & OP_WRITE) != 0) {
					WritePromise p = writer.get(this);

					if (p != null) {
						ByteBuffer b = p.getBuf();

						if (b != null) {
							key.interestOps(key.interestOps() & ~OP_WRITE);
							execute(() -> write(p, b), p);
						}
					}
				}
			} catch (Throwable ex) {
				Log.e(getClass().getName(), ex.getMessage(), ex);
				close();
			}
		}

		@Override
		public FutureSupplier<ByteBuffer> read(Supplier<ByteBuffer> buf) {
			assertEquals(0, (key.interestOps() & OP_READ));
			ReadPromise p = new ReadPromise(buf);

			if (!reader.compareAndSet(this, null, p)) {
				p.completeExceptionally(new IOException("Read pending"));
			} else if (!key.isValid()) {
				p.completeExceptionally(new ClosedChannelException());
				reader.compareAndSet(this, p, null);
			} else {
				try {
					if (key.isReadable()) {
						ByteBuffer b = p.getBuf();
						if (b != null) read(p, b);
					} else {
						selectorRun(() -> {
							if (reader.get(this) == p) key.interestOps(key.interestOps() | OP_READ);
						});
					}
				} catch (Throwable ex) {
					reader.compareAndSet(this, p, null);
					p.completeExceptionally(ex);
				}
			}

			return p;
		}

		private void read(ReadPromise p, ByteBuffer b) {
			try {
				assertEquals(0, (key.interestOps() & OP_READ));

				int i = channel().read(b);

				if ((i != 0) || !b.hasRemaining()) {
					if (i == -1) b.limit(b.position()); // End of stream
					else b.flip();
					reader.compareAndSet(this, p, null);
					p.complete(b);
				} else {
					p.setBuf(b);
					selectorRun(() -> {
						if (reader.get(this) == p) key.interestOps(key.interestOps() | OP_READ);
					});
				}
			} catch (Throwable ex) {
				reader.compareAndSet(this, p, null);
				p.completeExceptionally(ex);
			}
		}

		@Override
		public FutureSupplier<Void> write(Supplier<ByteBuffer> buf) {
			assertEquals(0, (key.interestOps() & OP_WRITE));
			WritePromise p = new WritePromise(buf);

			if (!writer.compareAndSet(this, null, p)) {
				p.completeExceptionally(new IOException("Write pending"));
			} else if (!key.isValid()) {
				p.completeExceptionally(new ClosedChannelException());
				writer.compareAndSet(this, p, null);
			} else {
				try {
					if (key.isWritable()) {
						ByteBuffer b = p.getBuf();
						if (b != null) write(p, b);
					} else {
						selectorRun(() -> {
							if (writer.get(this) == p) key.interestOps(key.interestOps() | OP_WRITE);
						});
					}
				} catch (Throwable ex) {
					writer.compareAndSet(this, p, null);
					p.completeExceptionally(ex);
				}
			}

			return p;
		}

		private void write(WritePromise p, ByteBuffer b) {
			try {
				assertEquals(0, (key.interestOps() & OP_WRITE));

				for (SocketChannel ch = channel(); b.hasRemaining(); ) {
					int i = ch.write(b);

					if (i == 0) {
						p.setBuf(b);
						selectorRun(() -> {
							if (writer.get(this) == p) key.interestOps(key.interestOps() | OP_WRITE);
						});
						return;
					}
				}

				writer.compareAndSet(this, p, null);
				p.complete(null);
			} catch (Throwable ex) {
				writer.compareAndSet(this, p, null);
				p.completeExceptionally(ex);
			}
		}

		@Override
		public void close() {
			key.cancel();
			IoUtils.close(channel());

			ReadPromise r = reader.getAndSet(this, null);
			WritePromise w = writer.getAndSet(this, null);
			if (r != null) r.completeExceptionally(new ClosedChannelException());
			if (w != null) w.completeExceptionally(new ClosedChannelException());
		}

		private SocketChannel channel() {
			return (SocketChannel) key.channel();
		}

		private final class ReadPromise extends ChannelPromise<ByteBuffer> {
			public ReadPromise(Supplier<ByteBuffer> bufSupplier) {
				super(bufSupplier);
			}

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				reader.compareAndSet(SelectableNetChannel.this, this, null);
				return super.cancel(mayInterruptIfRunning);
			}
		}

		private final class WritePromise extends ChannelPromise<Void> {
			public WritePromise(Supplier<ByteBuffer> bufSupplier) {
				super(bufSupplier);
			}

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				writer.compareAndSet(SelectableNetChannel.this, this, null);
				return super.cancel(mayInterruptIfRunning);
			}
		}
	}

	private static class ChannelPromise<T> extends Promise<T> {
		@SuppressWarnings({"unchecked", "rawtypes"})
		private static final AtomicReferenceFieldUpdater<ChannelPromise, Supplier<ByteBuffer>> buf =
				(AtomicReferenceFieldUpdater) AtomicReferenceFieldUpdater.newUpdater(ChannelPromise.class, Supplier.class, "bufHolder");
		@Keep
		@SuppressWarnings({"unused", "FieldCanBeLocal"})
		private volatile Supplier<ByteBuffer> bufHolder;

		ChannelPromise(Supplier<ByteBuffer> bufSupplier) {
			this.bufHolder = bufSupplier;
		}

		ByteBuffer getBuf() {
			Supplier<ByteBuffer> b = buf.getAndSet(this, null);
			return (b != null) ? b.get() : null;
		}

		void setBuf(ByteBuffer b) {
			assertNull(buf.get(this));
			buf.set(this, () -> b);
		}
	}
}
