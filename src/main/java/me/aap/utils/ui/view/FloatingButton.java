package me.aap.utils.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Px;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import me.aap.utils.R;
import me.aap.utils.ui.UiUtils;
import me.aap.utils.ui.activity.ActivityDelegate;
import me.aap.utils.ui.activity.ActivityListener;
import me.aap.utils.ui.fragment.ActivityFragment;
import me.aap.utils.ui.fragment.ViewFragmentMediator;

import static me.aap.utils.ui.fragment.ViewFragmentMediator.attachMediator;

/**
 * @author Andrey Pavlenko
 */
public class FloatingButton extends FloatingActionButton implements ActivityListener {
	private Mediator mediator;
	@ColorInt
	private int borderColor;
	@ColorInt
	private int borderFocusColor;
	@Px
	private float borderWidth;

	public FloatingButton(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.floatingActionButtonStyle);
	}

	public FloatingButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.FloatingButton,
				defStyleAttr, R.style.Theme_Utils_Base_FloatingButtonStyle);
		borderWidth = ta.getDimension(R.styleable.FloatingButton_borderWidth, 0);
		borderColor = ta.getColor(R.styleable.FloatingButton_borderColor, Color.TRANSPARENT);
		borderFocusColor = ta.getColor(R.styleable.FloatingButton_borderFocusColor, Color.TRANSPARENT);
		ta.recycle();

		ActivityDelegate a = getActivity();
		a.addBroadcastListener(this, ToolBarView.Mediator.DEFAULT_EVENT_MASK);
		setMediator(a.getActiveFragment());
	}

	protected Mediator getMediator() {
		return mediator;
	}

	protected void setMediator(Mediator mediator) {
		this.mediator = mediator;
	}

	protected boolean setMediator(ActivityFragment f) {
		return attachMediator(this, f, (f == null) ? null : f::getFloatingButtonMediator,
				this::getMediator, this::setMediator);
	}

	@Px
	public float getBorderWidth() {
		return borderWidth;
	}

	public void setBorderWidth(@Px float borderWidthDpi) {
		this.borderWidth = borderWidthDpi;
	}

	@ColorInt
	public int getBorderColor() {
		return borderColor;
	}

	public void setBorderColor(@ColorInt int borderColor) {
		this.borderColor = borderColor;
	}

	@ColorInt
	public int getBorderFocusColor() {
		return borderFocusColor;
	}

	public void setBorderFocusColor(@ColorInt int borderFocusColor) {
		this.borderFocusColor = borderFocusColor;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		float borderWidth = getBorderWidth();
		if (borderWidth == 0f) return;

		float pos = getWidth() / 2f;
		float radius = pos - borderWidth / 2;
		Paint paint = UiUtils.getPaint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(borderWidth);
		paint.setColor(isFocused() ? getBorderFocusColor() : getBorderColor());
		canvas.drawCircle(pos, pos, radius, paint);
	}

	@Override
	public void onActivityEvent(ActivityDelegate a, long e) {
		if (!handleActivityDestroyEvent(a, e)) {
			if (e == FRAGMENT_CHANGED) {
				if (setMediator(a.getActiveFragment())) return;
			}

			Mediator m = getMediator();
			if (m != null) m.onActivityEvent(this, a, e);
		}
	}

	protected ActivityDelegate getActivity() {
		return ActivityDelegate.get(getContext());
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(@NonNull MotionEvent e) {
		return getActivity().interceptTouchEvent(e, super::onTouchEvent);
	}

	public interface Mediator extends ViewFragmentMediator<FloatingButton> {

		@Override
		default void disable(FloatingButton fb) {
			fb.setOnClickListener(null);
			fb.setOnLongClickListener(null);
		}

		interface Back extends Mediator, OnClickListener {
			Back instance = new Back() {
			};

			@Override
			default void enable(FloatingButton fb, ActivityFragment f) {
				fb.setImageResource(getIcon(fb));
				fb.setOnClickListener(this);
			}

			@Override
			default void onClick(View v) {
				ActivityDelegate a = ActivityDelegate.get(v.getContext());
				ActivityFragment f = a.getActiveFragment();
				if (f == null) return;
				a.onBackPressed();
			}

			@DrawableRes
			default int getIcon(FloatingButton fb) {
				return getBackIcon();
			}

			@DrawableRes
			default int getBackIcon() {
				return R.drawable.back;
			}
		}

		interface BackMenu extends Back, OnLongClickListener {
			BackMenu instance = new BackMenu() {
			};

			@Override
			default void enable(FloatingButton fb, ActivityFragment f) {
				Back.super.enable(fb, f);
				fb.setOnLongClickListener(this);
			}

			@Override
			default void onClick(View v) {
				FloatingButton fb = (FloatingButton) v;
				if (fb.getActivity().isRootPage()) showMenu(fb);
				else Back.super.onClick(v);
			}

			@Override
			default boolean onLongClick(View v) {
				showMenu((FloatingButton) v);
				return true;
			}

			default void showMenu(FloatingButton fb) {
				NavBarView nb = fb.getActivity().getNavBar();
				if (nb != null) nb.showMenu();
			}

			@Override
			default void onActivityEvent(FloatingButton fb, ActivityDelegate a, long e) {
				switch ((int) e) {
					case FRAGMENT_CHANGED:
					case FRAGMENT_CONTENT_CHANGED:
						fb.setImageResource(getIcon(fb));
						break;
				}
			}

			@Override
			default int getIcon(FloatingButton fb) {
				return fb.getActivity().isRootPage() ? getMenuIcon() : getBackIcon();
			}

			@DrawableRes
			default int getMenuIcon() {
				return R.drawable.menu;
			}
		}
	}
}
