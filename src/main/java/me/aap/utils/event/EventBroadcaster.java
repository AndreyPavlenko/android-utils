package me.aap.utils.event;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import me.aap.utils.app.App;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author Andrey Pavlenko
 */
public interface EventBroadcaster<L> {
	long ALL_EVENTS_MASK = 0xFFFFFFFFFFFFFFFFL;

	Collection<ListenerRef<L>> getBroadcastEventListeners();

	default void addBroadcastListener(L listener) {
		addBroadcastListener(listener, ALL_EVENTS_MASK);
	}

	default void addBroadcastListener(L listener, long eventMask) {
		boolean add = true;
		Collection<ListenerRef<L>> listeners = getBroadcastEventListeners();

		for (Iterator<ListenerRef<L>> it = listeners.iterator(); it.hasNext(); ) {
			ListenerRef<L> ref = it.next();
			L l = ref.get();

			if (l == null) {
				it.remove();
			} else if (l == listener) {
				if (ref.mask != eventMask) it.remove();
				else add = false;
			}
		}

		if (add) listeners.add(new ListenerRef<>(listener, eventMask));
	}

	default void removeBroadcastListener(L listener) {
		removeBroadcastListeners(l -> l == listener);
	}

	default void removeBroadcastListeners(Predicate<L> matcher) {
		for (Iterator<ListenerRef<L>> it = getBroadcastEventListeners().iterator(); it.hasNext(); ) {
			L l = it.next().get();
			if ((l == null) || (matcher.test(l))) it.remove();
		}
	}

	default void postBroadcastEvent(Consumer<L> broadcaster, long eventMask) {
		App.get().getHandler().post(() -> fireBroadcastEvent(broadcaster, eventMask));
	}

	default void fireBroadcastEvent(Consumer<L> broadcaster) {
		fireBroadcastEvent(broadcaster, ALL_EVENTS_MASK);
	}

	default void fireBroadcastEvent(Consumer<L> broadcaster, long eventMask) {
		Collection<ListenerRef<L>> listeners = getBroadcastEventListeners();
		List<L> list = new ArrayList<>(listeners.size());

		for (Iterator<ListenerRef<L>> it = getBroadcastEventListeners().iterator(); it.hasNext(); ) {
			ListenerRef<L> r = it.next();
			L l = r.get();
			if (l == null) it.remove();
			else if ((r.mask & eventMask) != 0) list.add(l);
		}

		for (L listener : list) {
			broadcaster.accept(listener);
		}
	}

	class ListenerRef<L> extends WeakReference<L> {
		private final long mask;

		public ListenerRef(L listener, long eventMask) {
			super(listener);
			mask = eventMask;
		}
	}
}
