package me.aap.utils.net;

import android.os.Build;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import me.aap.utils.async.Completable;
import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.Promise;
import me.aap.utils.async.RunnablePromise;
import me.aap.utils.concurrent.ConcurrentQueueBase;
import me.aap.utils.concurrent.ConcurrentQueueBase.Node;
import me.aap.utils.function.ProgressiveResultConsumer.Completion;
import me.aap.utils.io.IoUtils;
import me.aap.utils.log.Log;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.util.Objects.requireNonNull;
import static me.aap.utils.async.Completed.failed;
import static me.aap.utils.misc.Assert.assertEquals;

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

					if (k.isValid()) {
						Selectable select = (Selectable) k.attachment();
						if (select != null) select.select();
					}
				}
			} catch (Throwable ex) {
				if (!selector.isOpen()) break;
				Log.e(ex, "Selector failed");
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
				protected NetServer runTask() throws ClosedChannelException {
					channel.register(selector, OP_ACCEPT, server);
					return server;
				}

				@Nullable
				@Override
				public Executor getExecutor() {
					return executor;
				}
			};

			queue.add(p);
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

				@Nullable
				@Override
				public Executor getExecutor() {
					return executor;
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
							p.complete(nc);
						} catch (CancelledKeyException ignore) {
						} catch (Throwable ex) {
							p.completeExceptionally(ex);
						}
					});
				} catch (Throwable ex) {
					p.completeExceptionally(ex);
				}
			});

			selector.wakeup();
			return p;
		} catch (Throwable ex) {
			return failed(ex);
		}
	}

	@Override
	public Executor getExecutor() {
		return executor;
	}

	@Override
	public boolean isOpen() {
		return selector.isOpen();
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

	private static int getBufferOffset(ByteBuffer[] buf) {
		for (int i = 0; i < buf.length; i++) {
			if (buf[i].hasRemaining()) {
				return i;
			}
		}
		return -1;
	}

	private void selectorRun(Runnable run) {
		queue.add(run);
		selector.wakeup();
	}

	private interface Selectable {
		void select();
	}

	private final class SelectableNetServer implements NetServer, Selectable {
		private final ServerSocketChannel channel;
		private final Map<SocketOption<?>, ?> opts;
		private final ConnectionHandler handler;

		public SelectableNetServer(ServerSocketChannel channel, BindOpts o) {
			this.channel = channel;
			opts = o.opt.isEmpty() ? Collections.emptyMap() : o.opt;
			handler = requireNonNull(o.handler);
		}

		@Override
		public NetHandler getHandler() {
			return SelectorHandler.this;
		}

		@Override
		public int getPort() {
			return channel.socket().getLocalPort();
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
			} catch (CancelledKeyException ignore) {
				return;
			} catch (Throwable ex) {
				IoUtils.close(ch);
				Log.e(ex, "Failed to accept a connection");
				return;
			}

			try {
				executor.execute(() -> {
					try {
						handler.acceptConnection(nc);
					} catch (Throwable ex) {
						Log.e(ex, "Connection handler failed");
						nc.close();
					}
				});
			} catch (Throwable ex) {
				Log.e(ex, "Failed to execute connection handler");
				nc.close();
			}
		}

		@Override
		public boolean isOpen() {
			return channel.isOpen();
		}

		@Override
		public void close() {
			try {
				channel.close();
			} catch (Throwable ex) {
				Log.e(ex, "Failed to close server channel");
			}
		}

		@Override
		public String toString() {
			return channel.toString();
		}
	}

	private static final AtomicReferenceFieldUpdater<SelectableNetChannel, ReadPromise> READER =
			AtomicReferenceFieldUpdater.newUpdater(SelectableNetChannel.class, ReadPromise.class, "reader");
	private static final AtomicIntegerFieldUpdater<SelectableNetChannel> WRITING =
			AtomicIntegerFieldUpdater.newUpdater(SelectableNetChannel.class, "writing");

	private final class SelectableNetChannel
			extends ConcurrentQueueBase<ByteBufferArraySupplier, WritePromise>
			implements NetChannel, Selectable {
		private final SelectionKey key;
		@Keep
		@SuppressWarnings("unused")
		volatile ReadPromise reader;
		@Keep
		@SuppressWarnings("unused")
		volatile int writing;

		public SelectableNetChannel(SelectionKey key) {
			this.key = key;
		}

		@Override
		public void select() {
			try {
				assertEquals(0, key.interestOps() & (OP_ACCEPT | OP_CONNECT));
				int ready = key.readyOps();
				int interest = key.interestOps();

				if (((ready & OP_READ) != 0) && ((interest & OP_READ) != 0)) {
					key.interestOps(interest &= ~OP_READ);
					executor.execute(this::doRead);
				}

				if (((ready & OP_WRITE) != 0) && ((interest & OP_WRITE) != 0)) {
					key.interestOps(interest & ~OP_WRITE);
					if (WRITING.compareAndSet(this, 0, 1)) executor.execute(this::doWrite);
				}
			} catch (CancelledKeyException ignore) {
			} catch (Throwable ex) {
				Log.d(ex, "Selected operation failed - closing channel ", this);
				close();
			}
		}

		@Override
		public FutureSupplier<ByteBuffer> read(ByteBufferSupplier buf, @Nullable Completion<ByteBuffer> consumer) {
			ReadPromise p = new ReadPromise(buf);
			if (consumer != null) p.onCompletion(consumer);

			if (!READER.compareAndSet(this, null, p)) {
				for (ReadPromise r = reader; ; r = reader) {
					if (!r.isDone()) {
						p.completeExceptionally(new IOException("Read pending"));
						return p;
					} else if (READER.compareAndSet(this, r, p)) {
						break;
					}
				}
			}

			if (!isOpen()) {
				p.completeExceptionally(ChannelClosed.instance);
				READER.compareAndSet(this, p, null);
			} else {
				assertEquals(0, (key.interestOps() & OP_READ));
				setInterest(p, OP_READ);
			}

			return p;
		}

		private void doRead() {
			ReadPromise p = reader;
			if (p == null) return;

			ByteBufferSupplier bs = p.buf;

			if (bs == null) {
				READER.compareAndSet(this, p, null);
				return;
			}

			ByteBuffer buf = bs.getByteBuffer();

			try {
				int i = channel().read(buf);

				if ((i != 0) || !buf.hasRemaining()) {
					if (i == -1) buf.limit(buf.position()); // End of stream
					else buf.flip();
					READER.compareAndSet(this, p, null);
					p.complete(buf);
				} else {
					bs.retainByteBuffer(buf);
					setInterest(p, OP_READ);
				}
			} catch (Throwable ex) {
				bs.releaseByteBuffer(buf);
				READER.compareAndSet(this, p, null);
				p.completeExceptionally(ex);
			}
		}

		@Override
		public FutureSupplier<Void> write(ByteBufferArraySupplier buf) {
			if (!isOpen()) return failed(ChannelClosed.instance);

			WritePromise p = new WritePromise(buf);
			offerNode(p);
			if (peekNode() == p) setInterest(p, OP_WRITE);
			return p;
		}

		private void doWrite() {
			ByteBufferArraySupplier bs = null;
			ByteBuffer[] buf = null;

			try {
				for (SocketChannel ch = channel(); ; ) {
					WritePromise p = peekNode();

					for (; p == null; p = peekNode()) {
						assertEquals(1, writing);
						writing = 0;
						if (isEmpty() || !WRITING.compareAndSet(this, 0, 1)) return;
					}

					bs = p.buf;

					if (bs == null) {
						poll();
						continue;
					}

					buf = bs.getByteBufferArray();
					int off = getBufferOffset(buf);

					if (off == -1) {
						bs.releaseByteBufferArray(buf);
						p.releaseBuf();
						poll();
						continue;
					}

					for (; ; ) {
						long i = ch.write(buf, off, buf.length - off);

						if (i == 0) {
							bs.retainByteBufferArray(buf, off);
							writing = 0;
							setInterest(p, OP_WRITE);
							return;
						}

						off = getBufferOffset(buf);

						if (off == -1) {
							bs.releaseByteBufferArray(buf);
							poll();
							p.complete(null);
							break;
						}
					}
				}
			} catch (Throwable ex) {
				if ((bs != null) && (buf != null)) bs.releaseByteBufferArray(buf);
				close(ex);
				writing = 0;
			}
		}

		private void setInterest(Completable<?> p, int interest) {
			selectorRun(() -> {
				try {
					if (key.isValid()) key.interestOps(key.interestOps() | interest);
				} catch (Throwable ex) {
					p.completeExceptionally(ex);
				}
			});
		}

		@Override
		public NetHandler getHandler() {
			return SelectorHandler.this;
		}

		@Override
		public boolean isOpen() {
			return channel().isOpen();
		}

		@Override
		public void close() {
			close(ChannelClosed.instance);
		}

		private void close(Throwable err) {
			key.cancel();
			IoUtils.close(channel());

			clear(w -> w.completeExceptionally(err));
			ReadPromise r = READER.getAndSet(this, null);
			if (r != null) r.completeExceptionally(err);
		}

		@Override
		public String toString() {
			return channel().toString();
		}

		private SocketChannel channel() {
			return (SocketChannel) key.channel();
		}
	}

	private static abstract class ChannelPromise<T> extends Promise<T> {

		abstract void releaseBuf();

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if (!super.cancel(mayInterruptIfRunning)) return false;
			releaseBuf();
			return true;
		}

		@Override
		public boolean complete(@Nullable T value) {
			if (!super.complete(value)) return false;
			releaseBuf();
			return true;
		}

		@Override
		public boolean completeExceptionally(@NonNull Throwable ex) {
			if (!super.completeExceptionally(ex)) return false;
			releaseBuf();
			return true;
		}
	}

	private static final class ReadPromise extends ChannelPromise<ByteBuffer> {
		volatile ByteBufferSupplier buf;

		ReadPromise(ByteBufferSupplier buf) {
			this.buf = buf;
		}

		void releaseBuf() {
			ByteBufferSupplier buf = this.buf;

			if (buf != null) {
				this.buf = null;
				buf.release();
			}
		}
	}

	private static final class WritePromise extends ChannelPromise<Void> implements Node<ByteBufferArraySupplier> {
		@SuppressWarnings("rawtypes")
		private static final AtomicReferenceFieldUpdater NEXT = AtomicReferenceFieldUpdater.newUpdater(WritePromise.class, WritePromise.class, "next");
		private volatile WritePromise next;
		volatile ByteBufferArraySupplier buf;

		WritePromise(ByteBufferArraySupplier buf) {
			this.buf = buf;
		}

		@Override
		public ByteBufferArraySupplier getValue() {
			return buf;
		}

		@Override
		public WritePromise getNext() {
			return next;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean compareAndSetNext(Node<ByteBufferArraySupplier> expect, Node<ByteBufferArraySupplier> update) {
			return NEXT.compareAndSet(this, expect, update);
		}

		void releaseBuf() {
			ByteBufferArraySupplier buf = this.buf;

			if (buf != null) {
				this.buf = null;
				buf.release();
			}
		}
	}

	private static final class ChannelClosed extends ClosedChannelException {
		static final ChannelClosed instance = new ChannelClosed();


		@NonNull
		@Override
		public String getMessage() {
			return "Channel closed";
		}

		@Override
		public void printStackTrace() {
		}
	}
}
