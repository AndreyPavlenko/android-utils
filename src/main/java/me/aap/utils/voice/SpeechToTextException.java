package me.aap.utils.voice;

import android.speech.SpeechRecognizer;

/**
 * @author Andrey Pavlenko
 */
public class SpeechToTextException extends Exception {
	private final int errorCode;

	public SpeechToTextException(int errorCode) {
		super(msg(errorCode));
		this.errorCode = errorCode;
	}

	public int getErrorCode() {
		return errorCode;
	}

	private static String msg(int errorCode) {
		String err;

		switch (errorCode) {
			case SpeechRecognizer.ERROR_NO_MATCH:
				err = "ERROR_NO_MATCH";
				break;
			case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
				err = "ERROR_SPEECH_TIMEOUT";
				break;
			case SpeechRecognizer.ERROR_AUDIO:
				err = "ERROR_AUDIO";
				break;
			case SpeechRecognizer.ERROR_CLIENT:
				err = "ERROR_CLIENT";
				break;
			case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
				err = "ERROR_INSUFFICIENT_PERMISSIONS";
				break;
			case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
				err = "ERROR_LANGUAGE_NOT_SUPPORTED";
				break;
			case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
				err = "ERROR_LANGUAGE_UNAVAILABLE";
				break;
			case SpeechRecognizer.ERROR_NETWORK:
				err = "ERROR_NETWORK";
				break;
			case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
				err = "ERROR_NETWORK_TIMEOUT";
				break;
			case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
				err = "ERROR_RECOGNIZER_BUSY";
				break;
			case SpeechRecognizer.ERROR_SERVER:
				err = "ERROR_SERVER";
				break;
			case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:
				err = "ERROR_SERVER_DISCONNECTED";
				break;
			case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS:
				err = "ERROR_TOO_MANY_REQUESTS";
				break;
			default:
				err = "code=" + errorCode;
		}

		return "Speech recognition failed: " + err;
	}
}
