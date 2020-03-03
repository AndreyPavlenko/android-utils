package me.aap.utils.ui.fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import me.aap.utils.ui.activity.ActivityDelegate;
import me.aap.utils.ui.menu.OverlayMenu;
import me.aap.utils.ui.menu.OverlayMenuItem;
import me.aap.utils.ui.view.FloatingButton;
import me.aap.utils.ui.view.NavBarView;
import me.aap.utils.ui.view.ToolBarView;

/**
 * @author Andrey Pavlenko
 */
public abstract class ActivityFragment extends Fragment {

	public abstract int getFragmentId();

	public CharSequence getTitle() {
		return "";
	}

	public ToolBarView.Mediator getToolBarMediator() {
		return ToolBarView.Mediator.BackTitle.instance;
	}

	public NavBarView.Mediator getNavBarMediator() {
		return (nb, f) -> {
		};
	}

	public FloatingButton.Mediator getFloatingButtonMediator() {
		return FloatingButton.Mediator.Back.instance;
	}

	public boolean isRootPage() {
		return true;
	}

	public boolean onBackPressed() {
		return false;
	}

	public void navBarItemReselected(int itemId) {
	}

	public ActivityDelegate getActivityDelegate() {
		return ActivityDelegate.get(getContext());
	}

	public void switchingFrom(@Nullable ActivityFragment currentFragment) {
	}

	public void switchingTo(@NonNull ActivityFragment newFragment) {
	}
}
