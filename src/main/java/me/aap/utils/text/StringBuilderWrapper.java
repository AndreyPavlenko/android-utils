package me.aap.utils.text;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author Andrey Pavlenko
 */
public class StringBuilderWrapper implements TextBuilder {
	private final StringBuilder sb;

	public StringBuilderWrapper() {
		this(new StringBuilder());
	}

	public StringBuilderWrapper(int capacity) {
		this(new StringBuilder(capacity));
	}

	public StringBuilderWrapper(StringBuilder sb) {
		this.sb = sb;
	}

	@NonNull
	public StringBuilderWrapper append(@Nullable Object obj) {
		sb.append(obj);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(@Nullable String str) {
		sb.append(str);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(@Nullable StringBuffer sb) {
		this.sb.append(sb);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(@Nullable CharSequence s) {
		sb.append(s);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(@Nullable CharSequence s, int start, int end) {
		sb.append(s, start, end);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(char[] str) {
		sb.append(str);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(char[] str, int offset, int len) {
		sb.append(str, offset, len);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(boolean b) {
		sb.append(b);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(char c) {
		sb.append(c);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(int i) {
		sb.append(i);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(long lng) {
		sb.append(lng);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(float f) {
		sb.append(f);
		return this;
	}

	@NonNull
	public StringBuilderWrapper append(double d) {
		sb.append(d);
		return this;
	}

	@NonNull
	public StringBuilderWrapper appendCodePoint(int codePoint) {
		sb.appendCodePoint(codePoint);
		return this;
	}

	@NonNull
	public StringBuilderWrapper delete(int start, int end) {
		sb.delete(start, end);
		return this;
	}

	@NonNull
	public StringBuilderWrapper deleteCharAt(int index) {
		sb.deleteCharAt(index);
		return this;
	}

	@NonNull
	public StringBuilderWrapper replace(int start, int end, @NonNull String str) {
		sb.replace(start, end, str);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int index, char[] str, int offset, int len) {
		sb.insert(index, str, offset, len);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int offset, @Nullable Object obj) {
		sb.insert(offset, obj);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int offset, @Nullable String str) {
		sb.insert(offset, str);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int offset, char[] str) {
		sb.insert(offset, str);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int dstOffset, @Nullable CharSequence s) {
		sb.insert(dstOffset, s);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int dstOffset, @Nullable CharSequence s, int start, int end) {
		sb.insert(dstOffset, s, start, end);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int offset, boolean b) {
		sb.insert(offset, b);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int offset, char c) {
		sb.insert(offset, c);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int offset, int i) {
		sb.insert(offset, i);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int offset, long l) {
		sb.insert(offset, l);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int offset, float f) {
		sb.insert(offset, f);
		return this;
	}

	@NonNull
	public StringBuilderWrapper insert(int offset, double d) {
		sb.insert(offset, d);
		return this;
	}

	public int indexOf(@NonNull String str) {
		return sb.indexOf(str);
	}

	public int indexOf(@NonNull String str, int fromIndex) {
		return sb.indexOf(str, fromIndex);
	}

	public int lastIndexOf(@NonNull String str) {
		return sb.lastIndexOf(str);
	}

	public int lastIndexOf(@NonNull String str, int fromIndex) {
		return sb.lastIndexOf(str, fromIndex);
	}

	@NonNull
	public StringBuilderWrapper reverse() {
		sb.reverse();
		return this;
	}

	@NonNull
	@Override
	public String toString() {
		return sb.toString();
	}

	public void trimToSize() {
		sb.trimToSize();
	}

	public int codePointAt(int index) {
		return sb.codePointAt(index);
	}

	public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
		sb.getChars(srcBegin, srcEnd, dst, dstBegin);
	}

	public int length() {
		return sb.length();
	}

	public void setCharAt(int index, char ch) {
		sb.setCharAt(index, ch);
	}

	@NonNull
	public CharSequence subSequence(int start, int end) {
		return sb.subSequence(start, end);
	}

	public String substring(int start) {
		return sb.substring(start);
	}

	public String substring(int start, int end) {
		return sb.substring(start, end);
	}

	public int capacity() {
		return sb.capacity();
	}

	public void setLength(int newLength) {
		sb.setLength(newLength);
	}

	public void ensureCapacity(int minimumCapacity) {
		sb.ensureCapacity(minimumCapacity);
	}

	public int codePointBefore(int index) {
		return sb.codePointBefore(index);
	}

	public char charAt(int index) {
		return sb.charAt(index);
	}

	public int codePointCount(int beginIndex, int endIndex) {
		return sb.codePointCount(beginIndex, endIndex);
	}

	public int offsetByCodePoints(int index, int codePointOffset) {
		return sb.offsetByCodePoints(index, codePointOffset);
	}
}
