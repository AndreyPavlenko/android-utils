package me.aap.utils.voice;

import static android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH;
import static android.speech.RecognizerIntent.EXTRA_LANGUAGE;
import static android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL;
import static android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS;
import static android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM;
import static me.aap.utils.function.ProgressiveResultConsumer.PROGRESS_UNKNOWN;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;

import androidx.annotation.Nullable;

import java.io.Closeable;
import java.util.List;
import java.util.Locale;

import me.aap.utils.async.FutureSupplier;
import me.aap.utils.async.Promise;

/**
 * @author Andrey Pavlenko
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SpeechToText implements RecognitionListener, Closeable {
	private final SpeechRecognizer recognizer;
	private final Intent recognizerIntent;
	private Promise promise;
	private Result result;

	public SpeechToText(Context ctx, Locale lang) {
		recognizer = SpeechRecognizer.createSpeechRecognizer(ctx);
		recognizer.setRecognitionListener(this);
		recognizerIntent = new Intent(ACTION_RECOGNIZE_SPEECH);
		recognizerIntent.putExtra(EXTRA_PARTIAL_RESULTS, true);
		recognizerIntent.putExtra(EXTRA_LANGUAGE, lang.toString());
		recognizerIntent.putExtra(EXTRA_LANGUAGE_MODEL, LANGUAGE_MODEL_FREE_FORM);
	}

	public <D> FutureSupplier<Result<D>> recognize(@Nullable D data) {
		if (promise != null) promise.cancel();
		Promise<Result<D>> p = new Promise<>();
		promise = p;
		result = new Result(data);
		recognizer.startListening(recognizerIntent);
		return p;
	}

	@Override
	public void close() {
		if (promise != null) promise.cancel();
		promise = null;
		result = null;
		recognizer.destroy();
	}

	@Override
	public void onResults(Bundle b) {
		if (promise == null) return;
		Promise p = promise;
		Result r = result;
		promise = null;
		result = null;
		List<String> t = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		r.text = ((t == null) || t.isEmpty()) ? null : t.get(0);
		p.complete(r);
	}

	@Override
	public void onPartialResults(Bundle b) {
		if (promise == null) return;
		List<String> t = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		result.text = ((t == null) || t.isEmpty()) ? null : t.get(0);
		promise.setProgress(result, PROGRESS_UNKNOWN, PROGRESS_UNKNOWN);
	}

	@Override
	public void onError(int error) {
		if (promise == null) return;
		Promise p = promise;
		Result r = result;
		promise = null;
		result = null;

		if (error == SpeechRecognizer.ERROR_NO_MATCH) {
			r.text = null;
			p.complete(r);
		} else {
			p.completeExceptionally(new SpeechToTextException(
					"Speech recognition failed: " + error, error));
		}
	}

	@Override
	public void onReadyForSpeech(Bundle params) {
	}

	@Override
	public void onBeginningOfSpeech() {
	}

	@Override
	public void onRmsChanged(float rmsdB) {
	}

	@Override
	public void onBufferReceived(byte[] buffer) {
	}

	@Override
	public void onEndOfSpeech() {
	}

	@Override
	public void onEvent(int eventType, Bundle params) {
	}

	public static class Result<D> {
		private final D data;
		String text;

		public Result(D data) {
			this.data = data;
		}

		public D getData() {
			return data;
		}

		public String getText() {
			return text;
		}
	}
}
