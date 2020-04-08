package me.aap.utils.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Andrey Pavlenko
 */
public class PriorityThreadPool extends ThreadPoolExecutor {
	public static final byte MIN_PRIORITY = Thread.MIN_PRIORITY;
	public static final byte NORM_PRIORITY = Thread.NORM_PRIORITY;
	public static final byte MAX_PRIORITY = Thread.MAX_PRIORITY;

	public PriorityThreadPool(int corePoolSize) {
		this(corePoolSize, corePoolSize, 60, SECONDS);
	}

	public PriorityThreadPool(int corePoolSize, int maximumPoolSize) {
		this(corePoolSize, maximumPoolSize, 60, SECONDS);
	}

	public PriorityThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, Executors.defaultThreadFactory());
	}

	public PriorityThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory, new AbortPolicy());
	}

	public PriorityThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, RejectedExecutionHandler handler) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, Executors.defaultThreadFactory(), handler);
	}

	public PriorityThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new PriorityBlockingQueue<>(), threadFactory, handler);
	}

	public Future<?> submit(byte priority, Runnable task) {
		RunnableFuture<Void> t = new PriorityFuture<>(requireNonNull(task), null, priority);
		execute(t);
		return t;
	}

	public <T> Future<T> submit(byte priority, Callable<T> task) {
		RunnableFuture<T> t = new PriorityFuture<>(requireNonNull(task), priority);
		execute(t);
		return t;
	}

	public <T> Future<T> submit(byte priority, Runnable task, T result) {
		RunnableFuture<T> t = new PriorityFuture<>(requireNonNull(task), result, priority);
		execute(t);
		return t;
	}

	@Override
	protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
		return new PriorityFuture<>(callable, NORM_PRIORITY);
	}

	@Override
	protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
		return new PriorityFuture<>(runnable, value, NORM_PRIORITY);
	}

	private static final class PriorityFuture<V> extends FutureTask<V> implements Comparable<PriorityFuture<V>> {
		final long time = System.currentTimeMillis();
		final byte priority;

		public PriorityFuture(Callable<V> callable, byte priority) {
			super(callable);
			this.priority = priority;
		}

		public PriorityFuture(Runnable runnable, V result, byte priority) {
			super(runnable, result);
			this.priority = priority;
		}

		@Override
		public int compareTo(PriorityFuture<V> o) {
			if (priority == o.priority) return Long.compare(time, o.time);
			else if (priority < o.priority) return 1;
			else return -1;
		}
	}
}
