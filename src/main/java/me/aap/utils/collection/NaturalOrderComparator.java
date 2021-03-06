package me.aap.utils.collection;

import java.util.Comparator;

import me.aap.utils.log.Log;

import static java.lang.Character.isDigit;

/**
 * @author Andrey Pavlenko
 */
public class NaturalOrderComparator<S extends CharSequence> implements Comparator<S> {

	public static <S extends CharSequence> int compareNatural(S a, S b, boolean reverse) {
		return reverse ? compareNatural(b, a) : compareNatural(a, b);
	}

	public static <S extends CharSequence> int compareNatural(S a, S b) {
		try {
			return cmp(a, b);
		} catch (NumberFormatException ex) {
			Log.e(ex, "Failed to compare '" + a + "' and '" + b + '\'');
			return a.toString().compareTo(b.toString());
		}
	}

	private static <S extends CharSequence> int cmp(S a, S b) {
		int alen = a.length();
		int blen = b.length();

		for (int aidx = 0, bidx = 0; ; ) {
			if (aidx == alen) return (bidx == blen) ? 0 : -1;
			if (bidx == blen) return 1;

			char ca = a.charAt(aidx);
			char cb = b.charAt(bidx);

			if (isDigit(ca) && isDigit(cb)) {
				int aoff = aidx;
				int boff = bidx;

				for (aidx++; (aidx < alen) && isDigit(a.charAt(aidx)); aidx++) ;
				for (bidx++; (bidx < blen) && isDigit(b.charAt(bidx)); bidx++) ;

				int nalen = aidx - aoff;
				int nblen = bidx - boff;

				if ((nalen != 1) || (nblen != 1)) {
					long na = Long.parseLong(a.subSequence(aoff, aidx).toString());
					long nb = Long.parseLong(b.subSequence(boff, bidx).toString());

					if (na == nb) {
						if (nalen != nblen) return (nalen < nblen) ? -1 : 1;
					} else if (na < nb) {
						return -1;
					} else {
						return 1;
					}
				} else if (ca != cb) {
					return (ca < cb) ? -1 : 1;
				}
			} else if (ca != cb) {
				return (ca < cb) ? -1 : 1;
			} else {
				aidx++;
				bidx++;
			}
		}
	}

	public int compare(S a, S b) {
		return compareNatural(a, b);
	}
}

