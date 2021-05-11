package me.aap.utils.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.LinearLayoutCompat;

import me.aap.utils.R;
import me.aap.utils.ui.UiUtils;

import static me.aap.utils.ui.UiUtils.toIntPx;
import static me.aap.utils.ui.UiUtils.toPx;

/**
 * @author Andrey Pavlenko
 */
public class NavButtonView extends LinearLayoutCompat {
	private boolean compact = false;

	public NavButtonView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, R.attr.bottomNavigationStyle);
	}

	public NavButtonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		inflate(context, R.layout.nav_button_layout, this);

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.NavButtonView,
				defStyleAttr, R.style.Theme_Utils_Base_NavBarStyle);
		getIcon().setImageDrawable(ta.getDrawable(R.styleable.NavButtonView_icon));
		getText().setText(ta.getText(R.styleable.NavButtonView_text));
		ta.recycle();

		setOrientation(VERTICAL);
		setClickable(true);
		setFocusable(true);
	}

	public void setCompact(boolean compact) {
		this.compact = compact;
		int pad = toIntPx(getContext(), compact ? 12 : 0);
		setPadding(pad, 0, pad, 0);
		setSelected(isSelected());
	}

	public boolean isCompact() {
		return compact;
	}

	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
		getIcon().setAlpha(selected ? 1f : 0.5f);
		if (isCompact()) getText().setVisibility(GONE);
		else getText().setVisibility(selected ? VISIBLE : GONE);
	}

	public ImageView getIcon() {
		return (ImageView) getChildAt(0);
	}

	public TextView getText() {
		return (TextView) getChildAt(1);
	}

	public static class Ext extends NavButtonView {
		private final Path path = new Path();
		private final CornerPathEffect corner;
		private boolean hasExt;

		public Ext(Context ctx, @Nullable AttributeSet attrs) {
			super(ctx, attrs);
			corner = new CornerPathEffect(toPx(ctx, 1));
		}

		public void setHasExt(boolean hasExt) {
			this.hasExt = hasExt;
			invalidate();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			if (!hasExt) return;

			Context ctx = getContext();
			NavBarView nb = (NavBarView) getParent();
			super.onDraw(canvas);

			float w = toPx(ctx, 2);
			float len = toPx(ctx, 4);
			path.reset();

			if (isCompact()) {
				float y2 = getHeight() - 2 * w;
				float y1 = y2 - len;
				float x2 = getWidth() / 2f;
				float x1 = x2 - len;
				float x3 = x2 + len;
				path.moveTo(x1, y1);
				path.lineTo(x2, y2);
				path.lineTo(x3, y1);
			} else {
				float x2 = getWidth() - 2 * w;
				float x1 = x2 - len;
				float y2 = getHeight() / 2f;
				float y1 = y2 - len;
				float y3 = y2 + len;
				path.moveTo(x1, y1);
				path.lineTo(x2, y2);
				path.lineTo(x1, y3);
			}

			Paint paint = UiUtils.getPaint();
			paint.setPathEffect(corner);
			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(w);
			paint.setColor(nb.getTint());
			paint.setAntiAlias(true);
			canvas.drawPath(path, paint);
		}
	}
}
