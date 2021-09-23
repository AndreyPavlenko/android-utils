package me.aap.utils.ui.view;

import static android.util.TypedValue.COMPLEX_UNIT_PX;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textview.MaterialTextView;

import me.aap.utils.ui.activity.ActivityDelegate;

/**
 * @author Andrey Pavlenko
 */
public class ScalableTextView extends MaterialTextView {

	public ScalableTextView(@NonNull Context context) {
		super(context);
		scale(context);
	}

	public ScalableTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		scale(context);
	}

	public ScalableTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		scale(context);
	}

	public ScalableTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		scale(context);
	}

	@Override
	public void setTextAppearance(int resId) {
		super.setTextAppearance(resId);
		scale(getContext());
	}

	private void scale(Context context) {
		float scale = ActivityDelegate.get(context).getTextIconSize();
		if (scale != 0F) setTextSize(COMPLEX_UNIT_PX, getTextSize() * scale);
	}
}
