package me.aap.utils.ui.fragment;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import me.aap.utils.R;
import me.aap.utils.ui.activity.ActivityDelegate;
import me.aap.utils.ui.view.ImageButton;
import me.aap.utils.ui.view.ToolBarView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.LEFT;
import static me.aap.utils.ui.activity.ActivityListener.FRAGMENT_CONTENT_CHANGED;

/**
 * @author Andrey Pavlenko
 */
public class GenericDialogFragment extends GenericFragment {

	public GenericDialogFragment() {
		this(ToolBarMediator.instance);
	}

	public GenericDialogFragment(ToolBarView.Mediator toolBarMediator) {
		setToolBarMediator(toolBarMediator);
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
	}

	protected void onCloseButtonClick() {
	}

	protected int getOkButtonVisibility() {
		return VISIBLE;
	}

	interface ToolBarMediator extends ToolBarView.Mediator {
		GenericDialogFragment.ToolBarMediator instance = new GenericDialogFragment.ToolBarMediator() {
		};

		@Override
		default void enable(ToolBarView tb, ActivityFragment f) {
			GenericDialogFragment p = (GenericDialogFragment) f;

			ImageButton b = createBackButton(tb, p);
			addView(tb, b, getBackButtonId(), LEFT);
			b.setVisibility(getBackButtonVisibility(p));

			b = createOkButton(tb, p);
			addView(tb, b, getOkButtonId());
			b.setVisibility(getOkButtonVisibility(p));

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
				b.setVisibility(getOkButtonVisibility(p));
			}
		}

		default void onBackButtonClick(GenericDialogFragment f) {
			f.getActivityDelegate().onBackPressed();
		}

		default void onOkButtonClick(GenericDialogFragment f) {
			f.onOkButtonClick();
		}

		default void onCloseButtonClick(GenericDialogFragment f) {
			f.onCloseButtonClick();
		}

		@IdRes
		default int getBackButtonId() {
			return R.id.tool_bar_back_button;
		}

		@DrawableRes
		default int getBackButtonIcon() {
			return R.drawable.back;
		}

		default ImageButton createBackButton(ToolBarView tb, GenericDialogFragment f) {
			ImageButton b = new ImageButton(tb.getContext(), null, R.attr.toolbarStyle);
			initButton(b, getBackButtonIcon(), v -> onBackButtonClick(f));
			return b;
		}

		default int getBackButtonVisibility(GenericDialogFragment f) {
			return f.getActivityDelegate().isRootPage() ? GONE : VISIBLE;
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
