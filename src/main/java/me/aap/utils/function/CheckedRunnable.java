package me.aap.utils.function;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author Andrey Pavlenko
 */
public interface CheckedRunnable<T extends Throwable> {

	void run() throws T;

	static <T extends Throwable> void runWithRetry(@NonNull CheckedRunnable run) {
		runWithRetry(run, null, null);
	}

	static <T extends Throwable> void runWithRetry(@NonNull CheckedRunnable run,
																								 @Nullable Consumer<Throwable> onFailure) {
		runWithRetry(run, onFailure, null);
	}

	static <T extends Throwable> void runWithRetry(@NonNull CheckedRunnable run,
																								 @Nullable Consumer<Throwable> onFailure,
																								 @Nullable String msg) {
		try {
			run.run();
		} catch (Throwable ex) {
			Log.d(CheckedRunnable.class.getName(), ((msg != null) ? msg : ex.getMessage()) + ". Retrying...", ex);

			try {
				run.run();
			} catch (Throwable ex1) {
				if (onFailure != null) {
					onFailure.accept(ex1);
				} else {
					Log.e(CheckedRunnable.class.getName(), (msg != null) ? msg : ex.getMessage(), ex1);
				}
			}
		}
	}
}
