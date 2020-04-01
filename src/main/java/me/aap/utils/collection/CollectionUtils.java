package me.aap.utils.collection;

import android.os.Build;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import me.aap.utils.function.Consumer;
import me.aap.utils.function.Function;
import me.aap.utils.function.IntBiConsumer;
import me.aap.utils.function.IntFunction;
import me.aap.utils.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * @author Andrey Pavlenko
 */
public class CollectionUtils {


	public static <T> int indexOf(T[] array, T value) {
		for (int i = 0; i < array.length; i++) {
			if (Objects.equals(value, array[i])) return i;
		}
		return -1;
	}

	public static int indexOf(int[] array, int value) {
		for (int i = 0; i < array.length; i++) {
			if (value == array[i]) return i;
		}
		return -1;
	}

	public static <T> boolean contains(T[] array, T value) {
		return indexOf(array, value) != -1;
	}

	public static <T> boolean contains(Iterable<T> i, Predicate<T> predicate) {
		for (T t : i) {
			if (predicate.test(t)) return true;
		}
		return false;
	}

	public static <T> T find(Iterable<T> i, Predicate<T> predicate) {
		for (T t : i) {
			if (predicate.test(t)) return t;
		}
		return null;
	}

	public static <T> boolean remove(Iterable<T> i, Predicate<T> predicate) {
		for (Iterator<T> it = i.iterator(); it.hasNext(); ) {
			if (predicate.test(it.next())) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	public static <T> T[] remove(T[] array, int idx) {
		Class<?> type = requireNonNull(array.getClass().getComponentType());
		@SuppressWarnings("unchecked") T[] a = (T[]) Array.newInstance(type, array.length - 1);
		System.arraycopy(array, 0, a, 0, idx);
		if (idx != (array.length - 1)) System.arraycopy(array, idx, a, idx + 1, a.length - idx);
		return a;
	}

	public static <T> void replace(List<T> list, T o, T with) {
		int i = list.indexOf(o);
		if (i != -1) list.set(i, with);
	}

	public static <T> void move(List<T> list, int fromPosition, int toPosition) {
		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				Collections.swap(list, i, i + 1);
			}
		} else {
			for (int i = fromPosition; i > toPosition; i--) {
				Collections.swap(list, i, i - 1);
			}
		}
	}

	public static <T> void move(T[] array, int fromPosition, int toPosition) {
		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				T t = array[i];
				array[i] = array[i + 1];
				array[i + 1] = t;
			}
		} else {
			for (int i = fromPosition; i > toPosition; i--) {
				T t = array[i];
				array[i] = array[i - 1];
				array[i - 1] = t;
			}
		}
	}

	public static <K, V> V putIfAbsent(Map<K, V> m, K key, V value) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			return m.putIfAbsent(key, value);
		} else {
			V v = m.get(key);
			if (v != null) return v;
			m.put(key, value);
			return null;
		}
	}

	public static boolean remove(Map<?, ?> m, Object key, Object value) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			return m.remove(key, value);
		} else if (Objects.equals(m.get(key), value)) {
			m.remove(key);
			return true;
		} else {
			return false;
		}
	}

	public static <T, R> R[] mapToArray(Collection<? extends T> collection,
																			Function<? super T, ? extends R> mapper,
																			IntFunction<R[]> generator) {
		return map(collection, (i, t, a) -> a[i] = mapper.apply(t), generator);
	}

	public static <T, R> R map(Collection<? extends T> collection,
														 IntBiConsumer<? super T, R> mapper,
														 IntFunction<R> generator) {
		return filterMap(collection, t -> true, mapper, generator);
	}

	public static <T, R> R filterMap(Collection<? extends T> collection,
																	 Predicate<? super T> predicate,
																	 IntBiConsumer<? super T, R> mapper,
																	 IntFunction<R> generator) {
		int size = collection.size();
		R a = generator.apply(size);
		int i = 0;

		for (T t : collection) {
			if (predicate.test(t)) mapper.accept(i++, t, a);
		}

		return a;
	}

	public static <T> void forEach(Iterable<T> it, Consumer<? super T> action) {
		for (T t : it) {
			action.accept(t);
		}
	}
}
