package me.aap.utils.voice;

/**
 * @author Andrey Pavlenko
 */
public class SpeechToTextException extends Exception {
	private final int errorCode;

	public SpeechToTextException(String msg, int errorCode) {
		super(msg);
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}
}
