package me.aap.utils.pref;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import me.aap.utils.R;
import me.aap.utils.app.App;
import me.aap.utils.ui.UiUtils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;


/**
 * @author Andrey Pavlenko
 */
public class PreferenceViewAdapter extends RecyclerView.Adapter<PreferenceViewAdapter.PrefViewHolder> {
	private PreferenceSet set;
	private RecyclerView recyclerView;

	public PreferenceViewAdapter(PreferenceSet set) {
		this.set = set;
	}

	@Override
	public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
		this.recyclerView = recyclerView;
	}

	@NonNull
	@Override
	public PrefViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context ctx = parent.getContext();
		PreferenceView v = new PreferenceView(ctx);
		RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
		int margin = (int) UiUtils.toPx(ctx, 1);
		int padding = (int) UiUtils.toPx(ctx, 5);
		v.setLayoutParams(lp);
		lp.setMargins(margin, margin, margin, margin);
		v.setPadding(padding, padding, padding, padding);
		v.setFocusable(true);
		v.setBackgroundResource(R.drawable.box_secondary);
		return new PrefViewHolder(v);
	}

	@Override
	public void onBindViewHolder(@NonNull PrefViewHolder holder, int position) {
		holder.getPreferenceView().setPreference(this, set.preferences.get(position));
	}

	@Override
	public void onViewRecycled(@NonNull PrefViewHolder holder) {
		holder.getPreferenceView().setPreference(this, null);
	}

	@Override
	public int getItemCount() {
		return set.preferences.size();
	}

	public void setPreferenceSet(PreferenceSet set) {
		PreferenceSet old = getPreferenceSet();
		this.set = set;
		notifyDataSetChanged();

		if ((set != null) && (old != null) && (recyclerView != null)) {
			int idx = set.getPreferences().indexOf(old);

			if (idx != -1) {
				App.get().getHandler().post(() -> {
					if (set != this.set) return;
					PrefViewHolder h = (PrefViewHolder) recyclerView.findViewHolderForAdapterPosition(idx);
					if (h == null) return;
					h.getPreferenceView().requestFocus();
					recyclerView.smoothScrollToPosition(idx);
				});
			}
		}
	}

	public PreferenceSet getPreferenceSet() {
		return set;
	}

	public static class PrefViewHolder extends RecyclerView.ViewHolder {

		public PrefViewHolder(@NonNull PreferenceView pref) {
			super(pref);
		}

		public PreferenceView getPreferenceView() {
			return (PreferenceView) itemView;
		}
	}
}
