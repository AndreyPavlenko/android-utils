package me.aap.utils.log;

/**
 * @author Andrey Pavlenko
 */
public abstract class Logger {

	public abstract void logDebug(StringBuilder msg);

	public abstract void logDebug(StringBuilder msg, Throwable ex);

	public abstract void logInfo(StringBuilder msg);

	public abstract void logInfo(StringBuilder msg, Throwable ex);

	public abstract void logWarn(StringBuilder msg);

	public abstract void logWarn(StringBuilder msg, Throwable ex);

	public abstract void logError(StringBuilder msg);

	public abstract void logError(StringBuilder msg, Throwable ex);

	public void formatMessage(Log.Level level, StringBuilder sb, Object... msg) {
		boolean threadName = addThreadName();
		boolean callerName = addCallerName();
		boolean callerLocation = addCallerLocation();
		StackTraceElement ste;

		if (callerName || callerLocation) {
			StackTraceElement[] st = Thread.currentThread().getStackTrace();
			int off = getStackTraceOffset();
			ste = (st.length > off) ? st[off] : null;
		} else {
			ste = null;
		}

		if (addLevelName()) {
			sb.append('[').append(level);
			if ((ste == null) && !threadName) sb.append("] ");
			else sb.append(']');
		}
		if (threadName) {
			sb.append('[').append(Thread.currentThread().getName());
			if (ste == null) sb.append("] ");
			else sb.append(']');
		}
		if (ste != null) {
			sb.append('[');
			if (addCallerName() && addCallerLocation()) {
				sb.append(ste.getClassName()).append('.').append(ste.getMethodName()).append('(');
				sb.append(ste.getFileName()).append(':').append(ste.getLineNumber()).append(')');
			} else if (addCallerLocation()) {
				sb.append(ste.getFileName()).append(':').append(ste.getLineNumber());
			} else {
				sb.append(ste.getClassName()).append('.').append(ste.getMethodName()).append("()");
			}
			sb.append("] ");
		}

		for (Object m : msg) {
			sb.append(m);
		}
	}

	protected int getStackTraceOffset() {
		return 3;
	}

	protected boolean addLevelName() {
		return true;
	}

	protected boolean addThreadName() {
		return true;
	}

	protected boolean addCallerLocation() {
		return true;
	}

	protected boolean addCallerName() {
		return true;
	}
}
