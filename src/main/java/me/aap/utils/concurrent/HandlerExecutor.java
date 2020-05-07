package me.aap.utils.concurrent;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Executor;

/**
 * @author Andrey Pavlenko
 */
public class HandlerExecutor extends Handler implements Executor {

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
}
