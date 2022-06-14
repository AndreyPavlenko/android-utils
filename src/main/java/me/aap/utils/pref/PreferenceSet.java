package me.aap.utils.pref;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.aap.utils.function.Consumer;
import me.aap.utils.function.Supplier;
import me.aap.utils.ui.menu.OverlayMenu;

/**
 * @author Andrey Pavlenko
 */
public class PreferenceSet implements Supplier<PreferenceView.Opts> {
	final List<Supplier<? extends PreferenceView.Opts>> preferences = new ArrayList<>();
	private final int id;
	private final PreferenceSet parent;
	private final Consumer<PreferenceView.Opts> builder;
	private int idCounter = 1;
	private PreferenceViewAdapter adapter;

	public PreferenceSet() {
		this(null, null);
	}

	private PreferenceSet(PreferenceSet parent, Consumer<PreferenceView.Opts> builder) {
		int id = 1;
		for (PreferenceSet p = parent; p != null; p = p.parent) {
			if (p.parent == null) {
				id = ++p.idCounter;
				break;
			}
		}
		this.id = id;
		this.parent = parent;
		this.builder = builder;
	}

	public int getId() {
		return id;
	}

	public PreferenceSet getParent() {
		return parent;
	}

	@Nullable
	public PreferenceSet find(int id) {
		if (id == getId()) return this;
		for (Supplier<? extends PreferenceView.Opts> s : preferences) {
			if (s instanceof PreferenceSet) {
				PreferenceSet p = ((PreferenceSet) s).find(id);
				if (p != null) return p;
			}
		}
		return null;
	}

	List<Supplier<? extends PreferenceView.Opts>> getPreferences() {
		return preferences;
	}

	@Override
	public PreferenceView.Opts get() {
		PreferenceView.Opts opts = new PreferenceView.Opts();
		builder.accept(opts);
		return opts;
	}

	public void addBooleanPref(Consumer<PreferenceView.BooleanOpts> builder) {
		add(() -> {
			PreferenceView.BooleanOpts o = new PreferenceView.BooleanOpts();
			builder.accept(o);
			return o;
		});
	}

	public void addStringPref(Consumer<PreferenceView.StringOpts> builder) {
		add(() -> {
			PreferenceView.StringOpts o = new PreferenceView.StringOpts();
			builder.accept(o);
			return o;
		});
	}

	public void addFilePref(Consumer<PreferenceView.FileOpts> builder) {
		add(() -> {
			PreferenceView.FileOpts o = new PreferenceView.FileOpts();
			builder.accept(o);
			return o;
		});
	}

	public void addIntPref(Consumer<PreferenceView.IntOpts> builder) {
		add(() -> {
			PreferenceView.IntOpts o = new PreferenceView.IntOpts();
			builder.accept(o);
			return o;
		});
	}

	public void addFloatPref(Consumer<PreferenceView.FloatOpts> builder) {
		add(() -> {
			PreferenceView.FloatOpts o = new PreferenceView.FloatOpts();
			builder.accept(o);
			return o;
		});
	}

	public void addTimePref(Consumer<PreferenceView.TimeOpts> builder) {
		add(() -> {
			PreferenceView.TimeOpts o = new PreferenceView.TimeOpts();
			builder.accept(o);
			return o;
		});
	}

	public void addListPref(Consumer<PreferenceView.ListOpts> builder) {
		add(() -> {
			PreferenceView.ListOpts o = new PreferenceView.ListOpts();
			builder.accept(o);
			return o;
		});
	}

	public void addButton(Consumer<PreferenceView.ButtonOpts> builder) {
		add(() -> {
			PreferenceView.ButtonOpts o = new PreferenceView.ButtonOpts();
			builder.accept(o);
			return o;
		});
	}

	public PreferenceSet subSet(Consumer<PreferenceView.Opts> builder) {
		PreferenceSet sub = new PreferenceSet(this, builder);
		add(sub);
		return sub;
	}

	public void addToView(RecyclerView v) {
		v.setHasFixedSize(true);
		v.setLayoutManager(new LinearLayoutManager(v.getContext()));
		v.setAdapter((adapter != null) ? adapter : new PreferenceViewAdapter(this));
	}

	public void addToMenu(OverlayMenu.Builder b, boolean setMinWidth) {
		RecyclerView v = createView(b.getMenu().getContext(), setMinWidth);
		b.setCloseHandlerHandler(m -> ((PreferenceViewAdapter) v.getAdapter()).onDestroy());
		b.setView(v);
		v.requestFocus();
	}

	public RecyclerView createView(Context ctx, boolean setMinWidth) {
		RecyclerView v = new RecyclerView(ctx) {
			@Override
			public View focusSearch(View focused, int direction) {
				View v = super.focusSearch(focused, direction);
				for (ViewParent p = (v == null) ? null : v.getParent(); p != null; p = p.getParent()) {
					if (p == this) return v;
				}
				return null;
			}
		};
		addToView(v);
		if (setMinWidth) {
			v.setMinimumWidth(Resources.getSystem().getDisplayMetrics().widthPixels * 2 / 3);
		}
		return v;
	}

	public void configure(Consumer<PreferenceSet> c) {
		preferences.clear();
		c.accept(this);
		notifyChanged();
	}

	void setAdapter(PreferenceViewAdapter adapter) {
		this.adapter = adapter;
	}

	private void add(Supplier<? extends PreferenceView.Opts> supplier) {
		preferences.add(supplier);
	}

	private void notifyChanged() {
		if ((adapter != null) && (adapter.getPreferenceSet() == this)) {
			adapter.setPreferenceSet(this);
		}
	}
}
