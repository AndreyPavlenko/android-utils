package me.aap.utils.async;

import androidx.annotation.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import javax.annotation.Nonnull;

import me.aap.utils.app.App;
import me.aap.utils.concurrent.ConcurrentQueueBase;
import me.aap.utils.function.CheckedSupplier;
import me.aap.utils.function.Consumer;

/**
 * @author Andrey Pavlenko
 */
public class PromiseQueue {
	private final AtomicReference workThread = new AtomicReference();
	@SuppressWarnings("rawtypes")
	private final Q queue = new Q();
	@Nullable
	private final Executor exec;

	public PromiseQueue() {
		this(null);
	}

	public PromiseQueue(@Nullable Executor exec) {
		this.exec = exec;
	}

	@Nonnull
	public Executor getExecutor() {
		return (exec != null) ? exec : App.get().getExecutor();
	}

	@SuppressWarnings("unchecked")
	public <T> FutureSupplier<T> enqueue(CheckedSupplier<T, Throwable> task) {
		QueuedPromise<T> p = new QueuedPromise<>(task);

		if (workThread.get() == Thread.currentThread()) {
			p.run();
			return p;
		}

		queue.offerNode(p);
		if (queue.peekNode() == p) getExecutor().execute(this::processQueue);
		return p;
	}

	private void processQueue() {
		if (!workThread.compareAndSet(null, Thread.currentThread())) return;

		try {
			for (QueuedPromise<?> p = queue.pollNode(); p != null; p = queue.pollNode()) {
				p.run();
			}
		} finally {
			workThread.compareAndSet(Thread.currentThread(), null);
		}
	}

	private static final class Q<T> extends ConcurrentQueueBase<T, QueuedPromise<T>> {

		@Override
		protected void offerNode(QueuedPromise<T> node) {
			super.offerNode(node);
		}

		@Override
		protected QueuedPromise<T> pollNode() {
			return super.pollNode();
		}

		@Override
		protected QueuedPromise<T> peekNode() {
			return super.peekNode();
		}

		@Override
		protected void clear(Consumer<QueuedPromise<T>> c) {
			super.clear(c);
		}
	}

	private static final class QueuedPromise<T> extends RunnablePromise<T> implements ConcurrentQueueBase.Node<T> {
		@SuppressWarnings("rawtypes")
		private static final AtomicReferenceFieldUpdater NEXT = AtomicReferenceFieldUpdater.newUpdater(QueuedPromise.class, QueuedPromise.class, "next");
		volatile QueuedPromise<?> next;

		CheckedSupplier<T, Throwable> task;

		QueuedPromise(CheckedSupplier<T, Throwable> task) {
			this.task = task;
		}

		@Override
		protected T runTask() throws Throwable {
			try {
				return task.get();
			} finally {
				task = null;
			}
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		public QueuedPromise getNext() {
			return next;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean compareAndSetNext(ConcurrentQueueBase.Node<T> expect, ConcurrentQueueBase.Node<T> update) {
			return NEXT.compareAndSet(this, expect, update);
		}
	}
}
