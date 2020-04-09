package me.aap.utils.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import me.aap.utils.R;
import me.aap.utils.function.BiFunction;
import me.aap.utils.ui.view.FloatingButton;
import me.aap.utils.ui.view.NavBarView;
import me.aap.utils.ui.view.ToolBarView;

/**
 * @author Andrey Pavlenko
 */
public class GenericFragment extends ActivityFragment {
	private String title = "";
	private ToolBarView.Mediator toolBarMediator;
	private NavBarView.Mediator navBarMediator;
	private FloatingButton.Mediator floatingButtonMediator;
	private BiFunction<LayoutInflater, ViewGroup, View> viewFunction;

	@Override
	public int getFragmentId() {
		return R.id.generic_fragment;
	}

	public CharSequence getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ToolBarView.Mediator getToolBarMediator() {
		return (toolBarMediator != null) ? toolBarMediator : super.getToolBarMediator();
	}

	public void setToolBarMediator(ToolBarView.Mediator toolBarMediator) {
		this.toolBarMediator = toolBarMediator;
	}

	public NavBarView.Mediator getNavBarMediator() {
		return (navBarMediator != null) ? navBarMediator : super.getNavBarMediator();
	}

	public void setNavBarMediator(NavBarView.Mediator navBarMediator) {
		this.navBarMediator = navBarMediator;
	}

	public FloatingButton.Mediator getFloatingButtonMediator() {
		return (floatingButtonMediator != null) ? floatingButtonMediator : super.getFloatingButtonMediator();
	}

	public void setFloatingButtonMediator(FloatingButton.Mediator floatingButtonMediator) {
		this.floatingButtonMediator = floatingButtonMediator;
	}

	public boolean isRootPage() {
		return true;
	}

	public boolean onBackPressed() {
		return false;
	}

	public void setViewFunction(BiFunction<LayoutInflater, ViewGroup, View> viewFunction) {
		this.viewFunction = viewFunction;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return (viewFunction != null) ? viewFunction.apply(inflater, container)
				: super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void switchingFrom(@Nullable ActivityFragment currentFragment) {
		if ((navBarMediator != null) || (currentFragment == null)) return;
		navBarMediator = currentFragment.getNavBarMediator();
	}

	@Override
	public void switchingTo(@NonNull ActivityFragment newFragment) {
		title = "";
		toolBarMediator = null;
		navBarMediator = null;
		floatingButtonMediator = null;
		viewFunction = null;
	}
}
