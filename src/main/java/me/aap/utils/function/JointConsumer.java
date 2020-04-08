package me.aap.utils.function;

/**
 * @author Andrey Pavlenko
 */
public class JointConsumer<T> implements Consumer<T> {
	private final boolean firstCanBlock;
	private final boolean secondCanBlock;
	private Consumer<T> first;
	private Consumer<T> second;

	public JointConsumer(Consumer<T> first, Consumer<T> second) {
		firstCanBlock = (first != null) && first.canBlockThread();
		secondCanBlock = (second != null) && second.canBlockThread();
		this.first = first;
		this.second = second;
	}

	public Consumer<T> getFirst() {
		return new First();
	}

	public void accept(T t) {
		Consumer<T> first;
		Consumer<T> second;

		synchronized (this) {
			if ((first = this.first) != null) this.first = null;
			if ((second = this.second) != null) this.second = null;
		}

		if (first != null) first.accept(t);
		if (second != null) second.accept(t);
	}

	@Override
	public boolean canBlockThread() {
		return firstCanBlock || secondCanBlock;
	}

	private final class First implements Consumer<T> {

		@Override
		public void accept(T t) {
			Consumer<T> f;

			synchronized (JointConsumer.this) {
				if ((f = first) == null) return;
				first = null;
			}

			f.accept(t);
		}

		@Override
		public boolean canBlockThread() {
			return firstCanBlock;
		}
	}
}
