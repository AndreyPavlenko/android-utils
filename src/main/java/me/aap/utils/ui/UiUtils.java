package me.aap.utils.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build.VERSION_CODES;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import me.aap.utils.R;
import me.aap.utils.app.App;
import me.aap.utils.concurrent.ConcurrentUtils;
import me.aap.utils.function.Consumer;

import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.os.Build.VERSION.SDK_INT;
import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

/**
 * @author Andrey Pavlenko
 */
public class UiUtils {
	public static final byte ID_NULL = 0;

	public static float toPx(Context ctx, int dp) {
		return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
	}

	public static int toIntPx(Context ctx, int dp) {
		return (int) toPx(ctx, dp);
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

	@IdRes
	public static int getArrayItemId(int idx) {
		switch (idx) {
			case 0:
				return R.id.array_item_id_0;
			case 1:
				return R.id.array_item_id_1;
			case 2:
				return R.id.array_item_id_2;
			case 3:
				return R.id.array_item_id_3;
			case 4:
				return R.id.array_item_id_4;
			case 5:
				return R.id.array_item_id_5;
			case 6:
				return R.id.array_item_id_6;
			case 7:
				return R.id.array_item_id_7;
			case 8:
				return R.id.array_item_id_8;
			case 9:
				return R.id.array_item_id_9;
			default:
				return R.id.array_item_id_unknown;
		}
	}

	public static Bitmap getBitmap(Drawable d) {
		return getBitmap(d, 0, 0);
	}

	public static Bitmap getBitmap(Drawable d, int width, int height) {
		if (d instanceof BitmapDrawable) return ((BitmapDrawable) d).getBitmap();

		if (d instanceof VectorDrawable) {
			return drawBitmap(d, Color.TRANSPARENT, Color.TRANSPARENT, 0, 0);
		}

		if (SDK_INT < VERSION_CODES.O) return null;
		if (!(d instanceof AdaptiveIconDrawable)) return null;

		AdaptiveIconDrawable ad = (AdaptiveIconDrawable) d;
		Drawable bg = ad.getBackground();
		Drawable fg = ad.getForeground();
		LayerDrawable ld = new LayerDrawable(new Drawable[]{bg, fg});
		return drawBitmap(ld, Color.TRANSPARENT, Color.TRANSPARENT, width, height);
	}

	public static Bitmap drawBitmap(Drawable d, @ColorInt int bgColor, @ColorInt int fgColor) {
		return drawBitmap(d, bgColor, fgColor, 0, 0);
	}

	public static Bitmap drawBitmap(Drawable d, @ColorInt int bgColor, @ColorInt int fgColor,
																	int width, int height) {
		int w = (width != 0) ? width : d.getIntrinsicWidth();
		int h = (height != 0) ? height : d.getIntrinsicHeight();
		Bitmap bm = Bitmap.createBitmap(w, h, ARGB_8888);
		Canvas c = new Canvas(bm);
		d.setBounds(0, 0, c.getWidth(), c.getHeight());
		if (bgColor != Color.TRANSPARENT) bm.eraseColor(bgColor);
		if (fgColor != Color.TRANSPARENT) d.setTint(fgColor);
		d.draw(c);
		return bm;
	}

	public static Bitmap resizedBitmap(Bitmap bm, int maxSize) {
		int width = bm.getWidth();
		int height = bm.getHeight();
		if ((width <= maxSize) && (height <= maxSize)) return bm;

		float ratio = (float) width / (float) height;

		if (ratio > 1) {
			width = maxSize;
			height = (int) (width / ratio);
		} else {
			height = maxSize;
			width = (int) (height * ratio);
		}

		return Bitmap.createScaledBitmap(bm, width, height, true);
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
