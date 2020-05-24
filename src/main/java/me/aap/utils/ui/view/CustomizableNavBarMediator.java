package me.aap.utils.ui.view;

import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import me.aap.utils.ui.activity.ActivityDelegate;
import me.aap.utils.ui.fragment.ActivityFragment;
import me.aap.utils.ui.menu.OverlayMenuItem;
import me.aap.utils.ui.menu.OverlayMenuItemView;
import me.aap.utils.ui.menu.OverlayMenuView;

import static android.view.View.LAYOUT_DIRECTION_LTR;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
import static me.aap.utils.ui.UiUtils.ID_NULL;

/**
 * @author Andrey Pavlenko
 */
public abstract class CustomizableNavBarMediator implements NavBarView.Mediator {
	private final List<NavBarItem> ext = new ArrayList<>();
	private NavButtonView.Ext extButton;
	protected NavBarView navBar;

	protected abstract Collection<NavBarItem> getItems(NavBarView nb);

	@Override
	public void enable(NavBarView nb, ActivityFragment f) {
		navBar = nb;
		ext.clear();
		extButton = null;
		boolean selected = false;
		ActivityDelegate a = ActivityDelegate.get(nb.getContext());
		int activeId = a.getActiveNavItemId();
		if (activeId == ID_NULL) activeId = a.getActiveFragmentId();


		for (NavBarItem i : getItems(nb)) {
			if (i.isPinned()) {
				int id = i.getId();
				NavButtonView v = addButton(nb, i.getIcon(), i.getText(), id);
				if (id == activeId) {
					v.setSelected(true);
					selected = true;
				}
			} else {
				ext.add(i);
			}
		}

		if (ext.isEmpty()) return;

		if (!selected) {
			for (NavBarItem i : ext) {
				int id = i.getId();
				if (id == activeId) {
					extButton = createButton(nb, NavButtonView.Ext::new, i.getIcon(), i.getText());
					extButton.setSelected(true);
					extButton.setHasExt(ext.size() > 1);
					addView(nb, extButton, id, this);
					return;
				}
			}
		}

		NavBarItem i = ext.get(ext.size() - 1);
		extButton = createButton(nb, NavButtonView.Ext::new, i.getIcon(), i.getText());
		extButton.setHasExt(ext.size() > 1);
		addView(nb, extButton, i.getId(), this);
	}

	@Override
	public void disable(NavBarView nb) {
		NavBarView.Mediator.super.disable(nb);
		navBar = null;
		extButton = null;
	}

	@Override
	public void onClick(View v) {
		if ((v != extButton) || (ext.size() <= 1)) {
			NavBarView.Mediator.super.onClick(v);
			return;
		}

		ActivityDelegate a = ActivityDelegate.get(v.getContext());
		if (a == null) return;

		NavBarView nb = (NavBarView) v.getParent();
		OverlayMenuView menu = createOverlayMenu(nb);
		ColorStateList tint = extButton.getIcon().getImageTintList();
		ColorStateList textColor = extButton.getText().getTextColors();
		menu.setBackgroundColor(nb.getBgColor());
		menu.show(b -> {
			for (NavBarItem i : ext) {
				OverlayMenuItemView item = (OverlayMenuItemView) b.addItem(i.getId(),
						i.getIcon(), i.getText()).setData(i);
				item.setTextColor(textColor);
				item.setCompoundDrawableTintList(tint);
			}

			b.setSelectionHandler(this::extItemSelected);
			b.setCloseHandlerHandler(m -> ((ViewGroup) (nb.getParent())).removeView((View) m));
		});
	}

	protected OverlayMenuView createOverlayMenu(NavBarView nb) {
		ViewGroup parent = (ViewGroup) nb.getParent();
		OverlayMenuView menu = new OverlayMenuView(nb.getContext(), null);
		parent.addView(menu);
		ViewGroup.LayoutParams lp = menu.getLayoutParams();

		if (lp instanceof ConstraintLayout.LayoutParams) {
			ConstraintLayout.LayoutParams clp = (ConstraintLayout.LayoutParams) lp;
			clp.endToEnd = PARENT_ID;
			clp.bottomToBottom = PARENT_ID;
			clp.resolveLayoutDirection(LAYOUT_DIRECTION_LTR);
		}

		lp.height = WRAP_CONTENT;
		lp.width = WRAP_CONTENT;
		return menu;
	}

	protected boolean extItemSelected(OverlayMenuItem item) {
		NavBarItem i = item.getData();
		NavButtonView.Ext ext = setExtButton(null, i);
		itemSelected(ext, i.getId(), ActivityDelegate.get(ext.getContext()));
		return true;
	}

	@Override
	public void fragmentChanged(NavBarView nb, ActivityDelegate a, ActivityFragment f) {
		int id = f.getFragmentId();
		View v = nb.findViewById(id);

		if (v == null) {
			for (NavBarItem i : ext) {
				if (id == i.getId()) {
					v = setExtButton(nb, i);
					break;
				}
			}
		}

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

	protected NavButtonView.Ext setExtButton(@Nullable NavBarView nb, NavBarItem i) {
		if (extButton == null) {
			assert nb != null;
			extButton = createButton(nb, NavButtonView.Ext::new, i.getIcon(), i.getText());
			extButton.setHasExt(ext.size() > 1);
			addView(nb, extButton, i.getId(), this);
		} else {
			extButton.setId(i.getId());
			extButton.getIcon().setImageDrawable(i.getIcon());
			extButton.getText().setText(i.getText());
		}

		return extButton;
	}

	@Nullable
	protected NavButtonView.Ext getExtButton() {
		return extButton;
	}
}
