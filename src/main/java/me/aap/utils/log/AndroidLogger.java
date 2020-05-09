package me.aap.utils.log;

import me.aap.utils.BuildConfig;
import me.aap.utils.app.App;

/**
 * @author Andrey Pavlenko
 */
public class AndroidLogger extends Logger {
	private final String tag = App.get().getLogTag();

	@Override
	public void logDebug(StringBuilder msg) {
		android.util.Log.d(tag, msg.toString());
	}

	@Override
	public void logDebug(StringBuilder msg, Throwable ex) {
		android.util.Log.d(tag, msg.toString(), ex);
	}

	@Override
	public void logInfo(StringBuilder msg) {
		android.util.Log.i(tag, msg.toString());
	}

	@Override
	public void logInfo(StringBuilder msg, Throwable ex) {
		android.util.Log.i(tag, msg.toString(), ex);
	}

	@Override
	public void logWarn(StringBuilder msg) {
		android.util.Log.w(tag, msg.toString());
	}

	@Override
	public void logWarn(StringBuilder msg, Throwable ex) {
		android.util.Log.w(tag, msg.toString(), ex);
	}

	@Override
	public void logError(StringBuilder msg) {
		android.util.Log.e(tag, msg.toString());
	}

	@Override
	public void logError(StringBuilder msg, Throwable ex) {
		android.util.Log.e(tag, msg.toString(), ex);
	}

	protected int getStackTraceOffset() {
		return 4;
	}

	@Override
	protected boolean addLevelName() {
		return false;
	}

	@Override
	protected boolean addThreadName() {
		return BuildConfig.DEBUG;
	}

	@Override
	protected boolean addCallerLocation() {
		return BuildConfig.DEBUG;
	}

	@Override
	protected boolean addCallerName() {
		return BuildConfig.DEBUG;
	}
}
