package me.aap.utils.ui.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import me.aap.utils.R;
import me.aap.utils.ui.UiUtils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static me.aap.utils.ui.UiUtils.ID_NULL;
import static me.aap.utils.ui.UiUtils.toPx;

/**
 * @author Andrey Pavlenko
 */
public class ListView<I extends ListView.ListItem<I>> extends RecyclerView {
	@ColorInt
	private final int textColor;
	@StyleRes
	private final int textAppearance;
	@Nullable
	private final ColorStateList iconTint;
	@DrawableRes
	private final int itemBackground;
	@AttrRes
	private int defStyleAttr;
	private I parent;
	private List<I> items = Collections.emptyList();
	private OnItemClickListener<I> onItemClickListener;

	public ListView(Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
		this(context, attrs, defStyleAttr, ID_NULL);
	}

	public ListView(Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
		super(context, attrs, defStyleAttr);
		this.defStyleAttr = defStyleAttr;

		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ListView, defStyleAttr, defStyleRes);
		iconTint = ta.getColorStateList(R.styleable.ListView_tint);
		itemBackground = ta.getResourceId(R.styleable.ListView_background, ID_NULL);
		textColor = ta.getColor(R.styleable.ListView_android_textColor, Color.BLACK);
		textAppearance = ta.getResourceId(R.styleable.ListView_android_textAppearance, R.attr.textAppearanceBody1);
		ta.recycle();

		setAdapter(createAdapter());
		addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
		setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
	}

	@ColorInt
	public int getTextColor() {
		return textColor;
	}

	@StyleRes
	public int getTextAppearance() {
		return textAppearance;
	}

	@Nullable
	public ColorStateList getIconTint() {
		return iconTint;
	}

	@DrawableRes
	public int getItemBackground() {
		return itemBackground;
	}

	public List<I> getItems() {
		return items;
	}

	public I getParentItem() {
		return parent;
	}

	public void setItems(I parent) {
		setItems(parent, parent.getChildren());
	}

	@SuppressWarnings("unchecked")
	public void setItems(I parent, List<I> items) {
		this.parent = parent;
		this.items = items;
		RecyclerView.Adapter<Holder> a = getAdapter();
		if (a != null) a.notifyDataSetChanged();
	}

	public void setOnItemClickListener(OnItemClickListener<I> onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	protected void onItemClick(I item) {
		if ((onItemClickListener != null) && (onItemClickListener.onListItemClick(item))) return;
		if (item.hasChildren()) setItems(item, item.getChildren());
	}

	protected RecyclerView.Adapter<Holder> createAdapter() {
		return new RecyclerView.Adapter<Holder>() {

			@NonNull
			@Override
			public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
				return ListView.this.createViewHolder();
			}

			@Override
			public void onBindViewHolder(@NonNull Holder holder, int position) {
				holder.setItem(getItems().get(position));
			}

			@Override
			public int getItemCount() {
				return getItems().size();
			}
		};
	}

	protected Holder createViewHolder() {
		return new Holder();
	}

	public interface OnItemClickListener<I extends ListItem<I>> {
		boolean onListItemClick(I item);
	}

	public interface ListItem<I extends ListItem<I>> {

		@Nullable
		Drawable getIcon();

		@NonNull
		CharSequence getText();

		@Nullable
		default I getParent() {
			return null;
		}

		@NonNull
		default List<I> getChildren() {
			return Collections.emptyList();
		}

		default boolean hasChildren() {
			return false;
		}
	}

	protected class Holder extends ViewHolder implements OnClickListener {
		private I item;

		public Holder() {
			this(new TextView(getContext(), null, defStyleAttr));
		}

		public Holder(@NonNull View itemView) {
			super(itemView);
			RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
			itemView.setFocusable(true);
			itemView.setLayoutParams(lp);
			itemView.setOnClickListener(this);
			itemView.setBackgroundResource(getItemBackground());

			if (itemView instanceof TextView) {
				TextView t = (TextView) itemView;
				t.setTextAppearance(getTextAppearance());
				t.setTextColor(getTextColor());
				t.setCompoundDrawableTintList(getIconTint());
				int pad = (int) toPx(getContext(), 5);
				t.setCompoundDrawablePadding(pad);
				t.setPadding(pad, 2 * pad, pad, 2 * pad);
			}
		}

		public I getItem() {
			return item;
		}

		public void setItem(I item) {
			this.item = item;
			if (item != null) initView(item);
		}

		protected void initView(I item) {
			TextView t = (TextView) itemView;
			t.setText(item.getText());
			t.setCompoundDrawablesWithIntrinsicBounds(item.getIcon(), null, null, null);
		}

		@Override
		public void onClick(View v) {
			I i = getItem();
			if (i != null) onItemClick(i);
		}
	}
}
