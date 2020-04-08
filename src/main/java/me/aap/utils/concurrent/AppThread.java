package me.aap.utils.concurrent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.aap.utils.misc.Assert;
import me.aap.utils.text.SharedTextBuilder;

/**
 * @author Andrey Pavlenko
 */
public class AppThread extends Thread {
	private SharedTextBuilder sb;

	public AppThread() {
	}

	public AppThread(@Nullable Runnable target) {
		super(target);
	}

	public AppThread(@Nullable Runnable target, @NonNull String name) {
		super(target, name);
	}

	public SharedTextBuilder getSharedStringBuilder() {
		Assert.assertSame(this, Thread.currentThread());
		return (sb != null) ? sb : (sb = SharedTextBuilder.create(this));
	}
}
