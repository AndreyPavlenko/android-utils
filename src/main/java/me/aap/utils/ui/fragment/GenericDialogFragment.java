package me.aap.utils.ui.fragment;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.aap.utils.R;
import me.aap.utils.function.BooleanConsumer;
import me.aap.utils.function.BooleanSupplier;
import me.aap.utils.ui.activity.ActivityDelegate;
import me.aap.utils.ui.view.FloatingButton;
import me.aap.utils.ui.view.ImageButton;
import me.aap.utils.ui.view.ToolBarView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static me.aap.utils.ui.activity.ActivityListener.FRAGMENT_CONTENT_CHANGED;

/**
 * @author Andrey Pavlenko
 */
public class GenericDialogFragment extends GenericFragment {
	private BooleanConsumer consumer;
	private BooleanSupplier validator;

	public GenericDialogFragment() {
		this(ToolBarMediator.instance);
	}

	public GenericDialogFragment(ToolBarView.Mediator toolBarMediator) {
		setToolBarMediator(toolBarMediator);
	}

	public GenericDialogFragment(ToolBarView.Mediator toolBarMediator,
															 FloatingButton.Mediator floatingButtonMediator) {
		setToolBarMediator(toolBarMediator);
		setFloatingButtonMediator(floatingButtonMediator);
	}

	public void setDialogConsumer(BooleanConsumer consumer) {
		this.consumer = consumer;
	}

	public void setDialogValidator(BooleanSupplier validator) {
		this.validator = validator;
	}

	@Override
	public int getFragmentId() {
		return R.id.generic_dialog_fragment;
	}

	@Override
	public void switchingTo(@NonNull ActivityFragment newFragment) {
		super.switchingTo(newFragment);
	}

	protected void onOkButtonClick() {
		complete(true);
	}

	protected void onCloseButtonClick() {
		complete(false);
	}

	protected void complete(boolean ok) {
		BooleanConsumer c = consumer;
		consumer = null;
		validator = null;
		if (c != null) c.accept(ok);
	}

	protected int getOkButtonVisibility() {
		return ((validator == null) || validator.getAsBoolean()) ? VISIBLE : GONE;
	}

	@Override
	public void switchingFrom(@Nullable ActivityFragment currentFragment) {
		super.switchingFrom(currentFragment);
	}

	interface ToolBarMediator extends ToolBarView.Mediator.BackTitle {
		GenericDialogFragment.ToolBarMediator instance = new GenericDialogFragment.ToolBarMediator() {
		};

		@Override
		default void enable(ToolBarView tb, ActivityFragment f) {
			ToolBarView.Mediator.BackTitle.super.enable(tb, f);
			GenericDialogFragment p = (GenericDialogFragment) f;

			ImageButton b = createOkButton(tb, p);
			addView(tb, b, getOkButtonId());
			setOkButtonVisibility(b, getOkButtonVisibility(p));

			b = createCloseButton(tb, p);
			addView(tb, b, getCloseButtonId());
		}

		@Override
		default void onActivityEvent(ToolBarView tb, ActivityDelegate a, long e) {
			if (e == FRAGMENT_CONTENT_CHANGED) {
				GenericDialogFragment p = (GenericDialogFragment) a.getActiveFragment();
				if (p == null) return;

				ImageButton b = tb.findViewById(getBackButtonId());
				b.setVisibility(getBackButtonVisibility(p));

				b = tb.findViewById(getOkButtonId());
				setOkButtonVisibility(b, getOkButtonVisibility(p));
			}
		}

		default void onOkButtonClick(GenericDialogFragment f) {
			f.onOkButtonClick();
		}

		default void onCloseButtonClick(GenericDialogFragment f) {
			f.onCloseButtonClick();
		}

		@IdRes
		default int getOkButtonId() {
			return R.id.file_picker_ok;
		}

		@DrawableRes
		default int getOkButtonIcon() {
			return R.drawable.check;
		}

		default ImageButton createOkButton(ToolBarView tb, GenericDialogFragment f) {
			ImageButton b = new ImageButton(tb.getContext(), null, R.attr.toolbarStyle);
			initButton(b, getOkButtonIcon(), v -> onOkButtonClick(f));
			return b;
		}

		default int getOkButtonVisibility(GenericDialogFragment f) {
			return f.getOkButtonVisibility();
		}

		default void setOkButtonVisibility(ImageButton b, int vis) {
			b.setVisibility(vis);

			if (vis == VISIBLE) {
				Animation shake = AnimationUtils.loadAnimation(b.getContext(), R.anim.shake_y_20);
				b.startAnimation(shake);
			} else {
				b.clearAnimation();
			}
		}

		@IdRes
		default int getCloseButtonId() {
			return R.id.file_picker_close;
		}

		@DrawableRes
		default int getCloseButtonIcon() {
			return R.drawable.close;
		}

		default ImageButton createCloseButton(ToolBarView tb, GenericDialogFragment f) {
			ImageButton b = new ImageButton(tb.getContext(), null, R.attr.toolbarStyle);
			initButton(b, getCloseButtonIcon(), v -> onCloseButtonClick(f));
			return b;
		}
	}
}
