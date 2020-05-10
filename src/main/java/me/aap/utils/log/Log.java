package me.aap.utils.log;

import me.aap.utils.text.SharedTextBuilder;

import static me.aap.utils.log.Log.Level.DEBUG;
import static me.aap.utils.log.Log.Level.ERROR;
import static me.aap.utils.log.Log.Level.INFO;
import static me.aap.utils.log.Log.Level.WARN;
import static me.aap.utils.misc.MiscUtils.isAndroid;

/**
 * @author Andrey Pavlenko
 */
public abstract class Log {
	private static final Logger impl = isAndroid() ? new AndroidLogger() : new PrintStreamLogger(System.out);

	public static void d(Object... msg) {
		try (SharedTextBuilder tb = SharedTextBuilder.get()) {
			StringBuilder sb = tb.getStringBuilder();
			impl.formatMessage(DEBUG, sb, msg);
			impl.logDebug(sb);
		}
	}

	public static void d(Throwable err, Object... msg) {
		try (SharedTextBuilder tb = SharedTextBuilder.get()) {
			StringBuilder sb = tb.getStringBuilder();
			impl.formatMessage(DEBUG, sb, msg);
			impl.logDebug(sb, err);
		}
	}

	public static void i(Object... msg) {
		try (SharedTextBuilder tb = SharedTextBuilder.get()) {
			StringBuilder sb = tb.getStringBuilder();
			impl.formatMessage(INFO, sb, msg);
			impl.logInfo(sb);
		}
	}

	public static void i(Throwable err, Object... msg) {
		try (SharedTextBuilder tb = SharedTextBuilder.get()) {
			StringBuilder sb = tb.getStringBuilder();
			impl.formatMessage(INFO, sb, msg);
			impl.logInfo(sb, err);
		}
	}

	public static void w(Object... msg) {
		try (SharedTextBuilder tb = SharedTextBuilder.get()) {
			StringBuilder sb = tb.getStringBuilder();
			impl.formatMessage(WARN, sb, msg);
			impl.logWarn(sb);
		}
	}

	public static void w(Throwable err, Object... msg) {
		try (SharedTextBuilder tb = SharedTextBuilder.get()) {
			StringBuilder sb = tb.getStringBuilder();
			impl.formatMessage(WARN, sb, msg);
			impl.logWarn(sb, err);
		}
	}

	public static void e(Object... msg) {
		try (SharedTextBuilder tb = SharedTextBuilder.get()) {
			StringBuilder sb = tb.getStringBuilder();
			impl.formatMessage(ERROR, sb, msg);
			impl.logError(sb);
		}
	}

	public static void e(Throwable err, Object... msg) {
		try (SharedTextBuilder tb = SharedTextBuilder.get()) {
			StringBuilder sb = tb.getStringBuilder();
			impl.formatMessage(ERROR, sb, msg);
			impl.logError(sb, err);
		}
	}

	public enum Level {
		DEBUG, INFO, WARN, ERROR
	}
}
