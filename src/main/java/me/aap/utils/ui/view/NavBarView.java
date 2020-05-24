package me.aap.utils.ui.view;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.LinearLayoutCompat;

import me.aap.utils.R;
import me.aap.utils.function.BiFunction;
import me.aap.utils.ui.activity.ActivityDelegate;
import me.aap.utils.ui.activity.ActivityListener;
import me.aap.utils.ui.fragment.ActivityFragment;
import me.aap.utils.ui.fragment.ViewFragmentMediator;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static me.aap.utils.ui.fragment.ViewFragmentMediator.attachMediator;

/**
 * @author Andrey Pavlenko
 */
public class NavBarView extends LinearLayoutCompat implements ActivityListener {
	@ColorInt
	private final int bgColor;
	@ColorInt
	private final int tint;
	private Mediator mediator;

	public NavBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, R.attr.bottomNavigationStyle);
	}

	public NavBarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		TypedArray ta = context.obtainStyledAttributes(attrs,
				new int[]{android.R.attr.colorBackground, R.attr.tint},
				R.attr.bottomNavigationStyle, R.style.Theme_Utils_Base_NavBarStyle);
		bgColor = ta.getColor(0, Color.TRANSPARENT);
		tint = ta.getColor(1, Color.TRANSPARENT);
		setBackgroundColor(bgColor);
		ta.recycle();

		ActivityDelegate a = getActivity();
		a.addBroadcastListener(this, Mediator.DEFAULT_EVENT_MASK);
		setMediator(a.getActiveFragment());
	}

	public void showMenu() {
		Mediator m = getMediator();
		if (m != null) m.showMenu(this);
	}

	protected Mediator getMediator() {
		return mediator;
	}

	protected void setMediator(Mediator mediator) {
		this.mediator = mediator;
	}

	protected boolean setMediator(ActivityFragment f) {
		return attachMediator(this, f, (f == null) ? null : f::getNavBarMediator,
				this::getMediator, this::setMediator);
	}

	protected ActivityDelegate getActivity() {
		return ActivityDelegate.get(getContext());
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		return getActivity().interceptTouchEvent(e, super::onTouchEvent);
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

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Mediator m = getMediator();
		if (m != null) {
			m.disable(this);
			m.enable(this, getActivity().getActiveFragment());
		}
	}

	protected int getBgColor() {
		return bgColor;
	}

	protected int getTint() {
		return tint;
	}

	public interface Mediator extends ViewFragmentMediator<NavBarView>, OnClickListener {

		@Override
		default void disable(NavBarView nb) {
			nb.removeAllViews();
		}

		default void showMenu(NavBarView nb) {
		}

		@Override
		default void onClick(View v) {
			ActivityDelegate a = ActivityDelegate.get(v.getContext());
			int id = v.getId();
			int activeId = a.getActiveNavItemId();

			if (id == activeId) {
				itemReselected(v, id, a);
			} else {
				itemSelected(v, id, a);
			}
		}

		@Override
		default void onActivityEvent(NavBarView nb, ActivityDelegate a, long e) {
			if (e == FRAGMENT_CHANGED) {
				ActivityFragment f = a.getActiveFragment();
				if (f != null) fragmentChanged(nb, a, f);
			}
		}

		default void fragmentChanged(NavBarView nb, ActivityDelegate a, ActivityFragment f) {
			int id = f.getFragmentId();
			View v = nb.findViewById(id);
			if (v == null) return;

			v.setSelected(true);
			View active = nb.findViewById(a.getActiveNavItemId());

			if (active == null) {
				a.setActiveNavItemId(id);
			} else if (v != active) {
				a.setActiveNavItemId(id);
				active.setSelected(false);
			}
		}

		default void itemSelected(View item, @IdRes int id, ActivityDelegate a) {
			View active = a.getNavBar().findViewById(a.getActiveNavItemId());
			if (active != null) active.setSelected(false);

			item.setSelected(true);
			a.setActiveNavItemId(id);
			a.showFragment(id);
		}

		default void itemReselected(View item, @IdRes int id, ActivityDelegate a) {
			ActivityFragment f = a.getActiveFragment();

			if ((f != null) && (f.getFragmentId() == id)) {
				f.navBarItemReselected(id);
				return;
			}

			itemSelected(item, id, a);
		}

		default void addView(NavBarView nb, View v, @IdRes int id) {
			addView(nb, v, id, this);
		}

		default void addView(NavBarView nb, View v, @IdRes int id, OnClickListener onClick) {
			ActivityDelegate a = nb.getActivity();
			v.setId(id);
			v.setOnClickListener(onClick);
			nb.addView(v);

			if (id == a.getActiveNavItemId()) {
				v.setSelected(true);
			} else if (id == a.getActiveFragmentId()) {
				v.setSelected(true);
				a.setActiveNavItemId(id);
			}
		}

		default NavButtonView addButton(NavBarView nb, @DrawableRes int icon, @StringRes int text,
																		@IdRes int id) {
			return addButton(nb, icon, text, id, this);
		}

		default NavButtonView addButton(NavBarView nb, @DrawableRes int icon, @StringRes int text,
																		@IdRes int id, OnClickListener onClick) {
			NavButtonView b = createButton(nb, icon, text);
			addView(nb, b, id, onClick);
			return b;
		}

		default NavButtonView addButton(NavBarView nb, Drawable icon, CharSequence text, @IdRes int id) {
			return addButton(nb, icon, text, id, this);
		}

		default NavButtonView addButton(NavBarView nb, Drawable icon, CharSequence text,
																		@IdRes int id, OnClickListener onClick) {
			NavButtonView b = createButton(nb, icon, text);
			addView(nb, b, id, onClick);
			return b;
		}

		default NavButtonView createButton(NavBarView nb, @DrawableRes int icon, @StringRes int text) {
			Context ctx = nb.getContext();
			return createButton(nb, ctx.getDrawable(icon), ctx.getText(text));
		}

		default NavButtonView createButton(NavBarView nb, Drawable icon, CharSequence text) {
			return createButton(nb, NavButtonView::new, icon, text);
		}

		default <B extends NavButtonView> B createButton(
				NavBarView nb, BiFunction<Context, AttributeSet, B> constructor, Drawable icon, CharSequence text) {
			B b = constructor.apply(nb.getContext(), null);
			initButton(b, icon, text);
			return b;
		}

		default void initButton(NavButtonView b, Drawable icon, CharSequence text) {
			LinearLayoutCompat.LayoutParams lp = new LinearLayoutCompat.LayoutParams(MATCH_PARENT, MATCH_PARENT, 1.0f);
			b.setLayoutParams(lp);
			b.getIcon().setImageDrawable(icon);
			b.getText().setText(text);
			b.setBackgroundResource(R.drawable.focusable_shape_transparent);
		}
	}
}
