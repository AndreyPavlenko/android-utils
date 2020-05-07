package me.aap.utils.async;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import me.aap.utils.concurrent.ConcurrentQueueBase;
import me.aap.utils.function.CheckedSupplier;
import me.aap.utils.function.Consumer;

/**
 * @author Andrey Pavlenko
 */
public class PromiseQueue {
	private final AtomicInteger state = new AtomicInteger();
	private final Q queue = new Q();
	private final Executor exec;

	public PromiseQueue(Executor exec) {
		this.exec = exec;
	}

	public <T> FutureSupplier<T> enqueue(CheckedSupplier<T, Throwable> task) {
		QueuedPromise<T> p = new QueuedPromise<>(task);
		queue.offerNode(p);
		if (queue.peekNode() == p) exec.execute(this::processQueue);
		return p;
	}

	private void processQueue() {
		if (!state.compareAndSet(0, 1)) return;

		try {
			for (QueuedPromise<?> p = queue.pollNode(); p != null; p = queue.pollNode()) {
				p.run();
			}
		} finally {
			state.compareAndSet(1, 0);
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
