package me.aap.utils.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import me.aap.utils.R;

/**
 * @author Andrey Pavlenko
 */
public class NavButtonView extends LinearLayout {
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

	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
		getText().setVisibility(selected ? VISIBLE : GONE);
	}

	public ImageView getIcon() {
		return (ImageView) getChildAt(0);
	}

	public TextView getText() {
		return (TextView) getChildAt(1);
	}
}
