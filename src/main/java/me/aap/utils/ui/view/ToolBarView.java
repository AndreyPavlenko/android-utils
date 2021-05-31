package me.aap.utils.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.textview.MaterialTextView;

import java.util.Collection;
import java.util.LinkedList;

import me.aap.utils.R;
import me.aap.utils.event.EventBroadcaster;
import me.aap.utils.ui.UiUtils;
import me.aap.utils.ui.activity.ActivityDelegate;
import me.aap.utils.ui.activity.ActivityListener;
import me.aap.utils.ui.fragment.ActivityFragment;
import me.aap.utils.ui.fragment.ViewFragmentMediator;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.LEFT;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.RIGHT;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
import static me.aap.utils.ui.UiUtils.isVisible;
import static me.aap.utils.ui.UiUtils.toIntPx;
import static me.aap.utils.ui.fragment.ViewFragmentMediator.attachMediator;

/**
 * @author Andrey Pavlenko
 */
public class ToolBarView extends ConstraintLayout implements ActivityListener,
		EventBroadcaster<ToolBarView.Listener> {
	private Mediator mediator;
	private final Collection<ListenerRef<Listener>> listeners = new LinkedList<>();

	public ToolBarView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, R.attr.toolbarStyle);
	}

	public ToolBarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		TypedArray ta = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.colorBackground},
				R.attr.toolbarStyle, R.style.Theme_Utils_Base_ToolBarStyle);
		setBackgroundColor(ta.getColor(0, Color.TRANSPARENT));
		ta.recycle();

		ActivityDelegate a = getActivity();
		a.addBroadcastListener(this, Mediator.DEFAULT_EVENT_MASK);
		setMediator(a.getActiveFragment());
	}

	public Mediator getMediator() {
		return mediator;
	}

	protected void setMediator(Mediator mediator) {
		this.mediator = mediator;
	}

	protected boolean setMediator(ActivityFragment f) {
		return attachMediator(this, f, (f == null) ? null : f::getToolBarMediator,
				this::getMediator, this::setMediator);
	}

	public boolean onBackPressed() {
		Mediator m = getMediator();
		return (m != null) && m.onBackPressed(this);
	}

	public ActivityDelegate getActivity() {
		return ActivityDelegate.get(getContext());
	}

	public ActivityFragment getActiveFragment() {
		return getActivity().getActiveFragment();
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

	public EditText getFilter() {
		if (mediator instanceof Mediator.BackTitleFilter) {
			return findViewById(((Mediator.BackTitleFilter) mediator).getFilterId());
		} else {
			return findViewById(R.id.tool_bar_filter);
		}
	}

	@Override
	public Collection<ListenerRef<Listener>> getBroadcastEventListeners() {
		return listeners;
	}

	public View focusSearch() {
		View v = findViewById(R.id.tool_bar_back_button);
		return ((v != null) && isVisible(v)) ? v : this;
	}

	@Override
	public View focusSearch(View focused, int direction) {
		View v = getMediator().focusSearch(this, focused, direction);
		return (v != null) ? v : super.focusSearch(focused, direction);
	}

	public interface Listener {
		byte FILTER_CHANGED = 1;

		void onToolBarEvent(ToolBarView tb, byte event);
	}

	public interface Mediator extends ViewFragmentMediator<ToolBarView> {

		@Override
		default void enable(ToolBarView tb, ActivityFragment f) {
			tb.setVisibility(VISIBLE);
		}

		@Override
		default void disable(ToolBarView tb) {
			tb.removeAllViews();
		}

		default boolean onBackPressed(ToolBarView tb) {
			return false;
		}

		@Nullable
		default View focusSearch(ToolBarView tb, View focused, int direction) {
			if (direction != FOCUS_UP) return null;
			ActivityDelegate a = ActivityDelegate.get(tb.getContext());
			NavBarView nb = a.getNavBar();
			return nb.isBottom() && isVisible(nb) ? nb.focusSearch() : null;
		}

		default void addView(ToolBarView tb, View v, @IdRes int id) {
			addView(tb, v, id, RIGHT);
		}

		default void addView(ToolBarView tb, View v, @IdRes int id, int side) {
			ConstraintLayout.LayoutParams lp;
			v.setId(id);

			if (tb.getChildCount() == 0) {
				tb.addView(v);
				lp = (ConstraintLayout.LayoutParams) v.getLayoutParams();
				if (side == RIGHT) lp.endToEnd = PARENT_ID;
				else lp.startToStart = PARENT_ID;
			} else if (side == RIGHT) {
				View rv = tb.getChildAt(tb.getChildCount() - 1);
				ConstraintLayout.LayoutParams rlp = (ConstraintLayout.LayoutParams) rv.getLayoutParams();
				rlp.endToStart = id;
				rlp.endToEnd = UNSET;
				rlp.resolveLayoutDirection(LAYOUT_DIRECTION_LTR);
				tb.addView(v);
				lp = (ConstraintLayout.LayoutParams) v.getLayoutParams();
				lp.endToEnd = PARENT_ID;
			} else {
				View lv = tb.getChildAt(0);
				ConstraintLayout.LayoutParams llp = (ConstraintLayout.LayoutParams) lv.getLayoutParams();
				llp.startToEnd = id;
				llp.startToStart = UNSET;
				llp.resolveLayoutDirection(LAYOUT_DIRECTION_LTR);
				tb.addView(v, 0);
				lp = (ConstraintLayout.LayoutParams) v.getLayoutParams();
				lp.startToStart = PARENT_ID;
			}

			lp.topToTop = lp.bottomToBottom = PARENT_ID;
			lp.resolveLayoutDirection(LAYOUT_DIRECTION_LTR);
		}

		default ImageButton addButton(ToolBarView tb, @DrawableRes int icon, OnClickListener onClick,
																	@IdRes int id) {
			return addButton(tb, icon, onClick, id, RIGHT);
		}

		default ImageButton addButton(ToolBarView tb, @DrawableRes int icon, OnClickListener onClick,
																	@IdRes int id, int side) {
			ImageButton b = new ImageButton(tb.getContext(), null, R.attr.toolbarStyle);
			initButton(b, icon, onClick);
			addView(tb, b, id, side);
			return b;
		}

		default <B extends ImageButton> B initButton(B b, @DrawableRes int icon, OnClickListener onClick) {
			ConstraintLayout.LayoutParams lp = setLayoutParams(b, 0, WRAP_CONTENT);
			lp.horizontalWeight = 1;
			lp.dimensionRatio = "1:1";
			b.setImageResource(icon);
			b.setScaleType(ImageView.ScaleType.FIT_CENTER);
			b.setBackgroundResource(R.drawable.focusable_shape_transparent);
			if (onClick != null) b.setOnClickListener(onClick);
			setButtonPadding(b);
			return b;
		}

		default ConstraintLayout.LayoutParams setLayoutParams(View v, int width, int height) {
			ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(width, height);
			v.setLayoutParams(lp);
			return lp;
		}

		default void setButtonPadding(View v) {
			int pad = toIntPx(v.getContext(), 6);
			v.setPadding(pad, pad, pad, pad);
		}

		default EditText createEditText(ToolBarView tb) {
			Context ctx = tb.getContext();
			EditText t = ActivityDelegate.get(ctx).createEditText(ctx);
			t.setTextAppearance(getEditTextAppearance(ctx));
			return t;
		}

		@StyleRes
		default int getEditTextAppearance(Context ctx) {
			TypedArray ta = ctx.obtainStyledAttributes(null, new int[]{R.attr.editTextStyle},
					R.attr.toolbarStyle, R.style.Theme_Utils_Base_ToolBarStyle);
			int style = ta.getResourceId(0, R.style.Theme_Utils_Base_ToolBarEditTextStyle);
			ta.recycle();
			ta = ctx.obtainStyledAttributes(null, new int[]{android.R.attr.textAppearance},
					style, R.style.Theme_Utils_Base_ToolBarEditTextStyle);
			style = ta.getResourceId(0, R.style.Theme_Utils_Base_ToolBarEditTextAppearance);
			ta.recycle();
			return style;
		}

		default Mediator join(Mediator m) {
			class Joint extends JointMediator<ToolBarView, Mediator> implements Mediator {
				public Joint(Mediator m1, Mediator m2) {
					super(m1, m2);
				}

				@Override
				public boolean onBackPressed(ToolBarView tb) {
					return m1.onBackPressed(tb) | m2.onBackPressed(tb);
				}
			}

			return new Joint(Mediator.this, m);
		}

		interface Invisible extends Mediator {
			Invisible instance = new Invisible() {
			};

			@Override
			default void enable(ToolBarView tb, ActivityFragment f) {
				tb.setVisibility(GONE);
			}
		}

		/**
		 * Back button and title.
		 */
		interface BackTitle extends Mediator, OnClickListener {
			BackTitle instance = new BackTitle() {
			};

			@Override
			default void enable(ToolBarView tb, ActivityFragment f) {
				Mediator.super.enable(tb, f);
				TextView t = createTitleText(tb);
				addView(tb, t, getTitleId(), LEFT);
				t.setText(f.getTitle());
				if (backOnTitleClick()) t.setOnClickListener(this);

				ImageButton b = createBackButton(tb);
				addView(tb, b, getBackButtonId(), LEFT);
				b.setVisibility(getBackButtonVisibility(f));
			}

			@Override
			default void onActivityEvent(ToolBarView tb, ActivityDelegate a, long e) {
				switch ((int) e) {
					case FRAGMENT_CHANGED:
					case FRAGMENT_CONTENT_CHANGED:
						ImageButton b = tb.findViewById(getBackButtonId());
						TextView t = tb.findViewById(getTitleId());
						ActivityFragment f = tb.getActiveFragment();
						b.setVisibility(getBackButtonVisibility(f));
						t.setText(f.getTitle());
						break;
				}
			}

			@Override
			default void onClick(View v) {
				if (v.getId() == getTitleId()) {
					ToolBarView tb = ActivityDelegate.get(v.getContext()).getToolBar();
					ForcedVisibilityButton b = tb.findViewById(getBackButtonId());
					if (!b.isVisible()) return;
				}

				ActivityDelegate.get(v.getContext()).onBackPressed();
			}

			@IdRes
			default int getTitleId() {
				return R.id.tool_bar_title;
			}

			default TextView createTitleText(ToolBarView tb) {
				Context ctx = tb.getContext();
				MaterialTextView t = new MaterialTextView(ctx, null, R.attr.toolbarStyle);
				t.setTextAppearance(getTitleTextAppearance(ctx));
				t.setMaxLines(1);
				t.setFocusable(false);
				t.setEllipsize(TextUtils.TruncateAt.END);
				ConstraintLayout.LayoutParams lp = setLayoutParams(t, 0, WRAP_CONTENT);
				lp.horizontalWeight = 2;
				return t;
			}

			@StyleRes
			default int getTitleTextAppearance(Context ctx) {
				TypedArray ta = ctx.obtainStyledAttributes(null, new int[]{R.attr.textAppearanceHeadline6},
						R.attr.toolbarStyle, R.style.Theme_Utils_Base_ToolBarStyle);
				int style = ta.getResourceId(0, R.style.TextAppearance_MaterialComponents_Headline6);
				ta.recycle();
				return style;
			}

			default boolean backOnTitleClick() {
				return true;
			}

			@IdRes
			default int getBackButtonId() {
				return R.id.tool_bar_back_button;
			}

			@DrawableRes
			default int getBackButtonIcon() {
				return R.drawable.back;
			}

			default ForcedVisibilityButton createBackButton(ToolBarView tb) {
				ForcedVisibilityButton b = new ForcedVisibilityButton(tb.getContext(), null, R.attr.toolbarStyle);
				initButton(b, getBackButtonIcon(), this);
				return b;
			}

			default int getBackButtonVisibility(ActivityFragment f) {
				return f.getActivityDelegate().isRootPage() ? GONE : VISIBLE;
			}
		}

		/**
		 * Back button, title and filter.
		 */
		interface BackTitleFilter extends BackTitle {
			BackTitleFilter instance = new BackTitleFilter() {
			};

			@Override
			default void enable(ToolBarView tb, ActivityFragment f) {
				EditText t = createFilter(tb);
				t.setVisibility(GONE);
				addView(tb, t, getFilterId(), LEFT);

				BackTitle.super.enable(tb, f);

				ForcedVisibilityButton b = createFilterButton(tb);
				addView(tb, b, getFilterButtonId());

				ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) tb.findViewById(getTitleId()).getLayoutParams();
				lp.startToEnd = getBackButtonId();
				lp.endToStart = getFilterId();

				lp = (ConstraintLayout.LayoutParams) t.getLayoutParams();
				lp.startToEnd = getTitleId();
				lp.endToStart = getFilterButtonId();
			}

			@Override
			default void onClick(View v) {
				if (v.getId() == getFilterButtonId()) {
					ActivityDelegate a = ActivityDelegate.get(v.getContext());
					ToolBarView tb = a.getToolBar();
					TextView t = tb.findViewById(getTitleId());
					EditText f = tb.findViewById(getFilterId());
					ForcedVisibilityButton b = tb.findViewById(getBackButtonId());
					b.forceVisibility(true);
					tb.findViewById(getTitleId()).setVisibility(GONE);
					t.setVisibility(GONE);
					v.setVisibility(GONE);
					f.setVisibility(VISIBLE);
					f.requestFocus();
				} else {
					BackTitle.super.onClick(v);
				}
			}

			@Override
			default boolean onBackPressed(ToolBarView tb) {
				return hideEditText(tb);
			}

			@Override
			default void onActivityEvent(ToolBarView tb, ActivityDelegate a, long e) {
				BackTitle.super.onActivityEvent(tb, a, e);
				if (e == FRAGMENT_CHANGED) hideEditText(a.getToolBar());
			}

			default boolean hideEditText(ToolBarView tb) {
				EditText t = tb.findViewById(getFilterId());

				if (t.getVisibility() != GONE) {
					ForcedVisibilityButton b = tb.findViewById(getBackButtonId());
					t.setText("");
					t.clearFocus();
					t.setVisibility(GONE);
					b.forceVisibility(false);
					tb.findViewById(getTitleId()).setVisibility(VISIBLE);
					tb.findViewById(getFilterButtonId()).setVisibility(VISIBLE);
					return true;
				} else {
					return false;
				}
			}

			@IdRes
			default int getFilterId() {
				return R.id.tool_bar_filter;
			}

			default EditText createFilter(ToolBarView tb) {
				EditText t = createEditText(tb);
				ConstraintLayout.LayoutParams lp = setLayoutParams(t, 0, WRAP_CONTENT);
				TextChangedListener l = s -> tb.fireBroadcastEvent(r -> r.onToolBarEvent(tb, Listener.FILTER_CHANGED));
				t.addTextChangedListener(l);
				t.setBackgroundResource(R.drawable.tool_bar_edittext_bg);
				t.setOnKeyListener(UiUtils::dpadFocusHelper);
				t.setMaxLines(1);
				lp.horizontalWeight = 2;
				setFilterPadding(t);
				return t;
			}

			default void setFilterPadding(EditText t) {
				int p = toIntPx(t.getContext(), 2);
				t.setPadding(p, p, p, p);
			}

			@IdRes
			default int getFilterButtonId() {
				return R.id.tool_bar_filter_button;
			}

			@DrawableRes
			default int getFilterButtonIcon() {
				return R.drawable.filter;
			}

			default ForcedVisibilityButton createFilterButton(ToolBarView tb) {
				ForcedVisibilityButton b = new ForcedVisibilityButton(tb.getContext(), null, R.attr.toolbarStyle);
				initButton(b, getFilterButtonIcon(), this);
				return b;
			}
		}
	}
}
