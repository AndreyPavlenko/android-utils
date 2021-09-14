package me.aap.utils.concurrent;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Executor;

import me.aap.utils.BuildConfig;
import me.aap.utils.function.Cancellable;
import me.aap.utils.log.Log;

/**
 * @author Andrey Pavlenko
 */
public class HandlerExecutor extends Handler implements Executor {
	private final ScheduledTask queue = new ScheduledTask();
	private volatile boolean closed;

	public HandlerExecutor() {
	}

	public HandlerExecutor(@Nullable Callback callback) {
		super(callback);
	}

	public HandlerExecutor(@NonNull Looper looper) {
		super(looper);
	}

	public HandlerExecutor(@NonNull Looper looper, @Nullable Callback callback) {
		super(looper, callback);
	}

	@Override
	public void execute(Runnable command) {
		post(command);
	}

	public synchronized Cancellable schedule(@NonNull Runnable task, long delay) {
		if (isClosed()) {
			Log.d("Handler is closed! Unable to schedule task: ", task);
			return () -> false;
		}

		ScheduledTask t = new ScheduledTask(task);
		postDelayed(t, delay);
		return t;
	}

	public synchronized void close() {
		if (!isClosed()) {
			closed = true;
			while (queue.next != null) queue.next.cancel();
		}
	}

	public boolean isClosed() {
		return closed;
	}

	private final class ScheduledTask implements Runnable, Cancellable {
		private Runnable task;
		private ScheduledTask prev;
		private ScheduledTask next;

		ScheduledTask() {
			task = this;
		}

		private ScheduledTask(@NonNull Runnable task) {
			if (BuildConfig.D && (task == null)) throw new RuntimeException();
			this.task = task;
			prev = queue;
			next = queue.next;
			if (next != null) next.prev = this;
			queue.next = this;
		}

		@Override
		public void run() {
			if (BuildConfig.D && (task == this)) throw new RuntimeException();
			Runnable t = remove();
			if (t == null) Log.e("Delayed task is already done or canceled: ", t);
			else if (isClosed()) Log.e("Executor is closed! Ignoring delayed task: ", t);
			else t.run();
		}

		@Override
		public boolean cancel() {
			if (BuildConfig.D && (task == this)) throw new RuntimeException();
			Runnable t = remove();
			if (t == null) return false;
			removeCallbacks(this);
			return true;
		}

		private Runnable remove() {
			synchronized (HandlerExecutor.this) {
				Runnable t = task;
				if (t == null) return null;
				if (prev != null) prev.next = next;
				if (next != null) next.prev = prev;
				task = null;
				return t;
			}
		}
	}
}
