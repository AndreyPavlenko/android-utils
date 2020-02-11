package me.aap.utils.ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import me.aap.utils.R;
import me.aap.utils.app.App;
import me.aap.utils.concurrent.ConcurrentUtils;
import me.aap.utils.function.Consumer;

import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

/**
 * @author Andrey Pavlenko
 */
public class UiUtils {
	public static final byte ID_NULL = 0;

	public static int toPx(int dp) {
		return Math.round(dp * Resources.getSystem().getDisplayMetrics().density);
	}

	public static void showError(String msg) {
		Toast.makeText(App.get(), msg, Toast.LENGTH_LONG).show();
	}

	public static void showError(String msg, Throwable err) {
		Log.e("Utils", "Error ocurred", err);
		showError(msg);
	}

	public static void queryText(Context ctx, @StringRes int title, Consumer<CharSequence> result) {
		queryText(ctx, title, "", result);
	}

	public static void queryText(Context ctx, @StringRes int title, CharSequence initText,
															 Consumer<CharSequence> result) {
		EditText text = new EditText(ctx);
		text.setSingleLine();
		text.setText(initText);
		createAlertDialog(ctx)
				.setTitle(title).setView(text)
				.setNegativeButton(android.R.string.cancel, (d, i) -> result.accept(null))
				.setPositiveButton(android.R.string.ok, (d, i) -> result.accept(text.getText())).show();
	}

	public static AlertDialog.Builder createAlertDialog(Context ctx) {
		TypedArray ta = ctx.obtainStyledAttributes(new int[]{R.attr.alertDialogStyle});
		int style = ta.getResourceId(0, R.style.Theme_Utils_Base_AlertDialog);
		ta.recycle();
		return new AlertDialog.Builder(ctx, style);
	}

	public static boolean dpadFocusHelper(View v, int keyCode, KeyEvent event) {
		if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

		switch (keyCode) {
			case KEYCODE_DPAD_UP:
			case KEYCODE_DPAD_DOWN:
				View next = v.focusSearch(keyCode == KEYCODE_DPAD_UP ? View.FOCUS_UP : View.FOCUS_DOWN);

				if (next != null) {
					next.requestFocus();
					return true;
				}
			default:
				return false;
		}
	}

	public static Paint getPaint() {
		ConcurrentUtils.ensureMainThread(true);
		Paint p = PaintHolder.paint;
		p.reset();
		return p;
	}

	public static void drawGroupOutline(Canvas canvas, ViewGroup group, View label,
																			@ColorInt int backgroundColor,
																			@ColorInt int strokeColor, float strokeWidth,
																			float cornerRadius) {

		float w = group.getWidth();
		float h = group.getHeight();
		float x1 = label.getX();
		float x2 = x1 + label.getWidth();
		float y = label.getY() + label.getHeight() / 2f;
		float sw = strokeWidth / 2;

		Paint paint = getPaint();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(strokeWidth);

		if (backgroundColor != Color.TRANSPARENT) {
			paint.setColor(backgroundColor);
			paint.setStyle(Paint.Style.FILL);
			canvas.drawRoundRect(0, h, w, y - sw, cornerRadius, cornerRadius, paint);
		}

		w -= sw;
		h -= sw;

		Path p = new Path();
		p.moveTo(x2, y);
		p.lineTo(w, y);
		p.lineTo(w, h);
		p.lineTo(sw, h);
		p.lineTo(sw, y);
		p.lineTo(x1, y);

		paint.setPathEffect(new CornerPathEffect(cornerRadius));
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(strokeColor);
		canvas.drawPath(p, paint);
	}

	public static void drawGroupOutline(Canvas canvas, ViewGroup group, View label1, View label2,
																			@ColorInt int backgroundColor,
																			@ColorInt int strokeColor, float strokeWidth,
																			float cornerRadius) {

		float w = group.getWidth();
		float h = group.getHeight();
		float x11 = label1.getX();
		float x12 = x11 + label1.getWidth();
		float x21 = label2.getX();
		float x22 = x21 + label2.getWidth();
		float y = label1.getY() + label1.getHeight() / 2f;
		float sw = strokeWidth / 2;

		Paint paint = getPaint();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(strokeWidth);

		if (backgroundColor != Color.TRANSPARENT) {
			paint.setColor(backgroundColor);
			paint.setStyle(Paint.Style.FILL);
			canvas.drawRoundRect(0, h, w, y - sw, cornerRadius, cornerRadius, paint);
		}

		w -= sw;
		h -= sw;

		Path p = new Path();
		p.moveTo(x12, y);
		p.lineTo(x21, y);
		p.moveTo(x22, y);
		p.lineTo(w, y);
		p.lineTo(w, h);
		p.lineTo(sw, h);
		p.lineTo(sw, y);
		p.lineTo(x11, y);

		paint.setPathEffect(new CornerPathEffect(cornerRadius));
		paint.setStyle(Paint.Style.STROKE);
		paint.setColor(strokeColor);
		canvas.drawPath(p, paint);
	}

	private static final class PaintHolder {
		static final Paint paint = new Paint();
	}
}
