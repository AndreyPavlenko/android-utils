package me.aap.utils.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static me.aap.utils.text.TextUtils.toByteArray;
import static me.aap.utils.text.TextUtils.toHexString;

/**
 * @author Andrey Pavlenko
 */
public class SecurityUtils {

	public static MessageDigest sha256Digest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static byte[] sha256(byte... bytes) {
		return digest(sha256Digest(), bytes);
	}

	public static String sha256String(byte... bytes) {
		return digestString(sha256Digest(), bytes);
	}

	public static byte[] sha256(ByteBuffer bytes) {
		return digest(sha256Digest(), bytes);
	}

	public static String sha256String(ByteBuffer bytes) {
		return digestString(sha256Digest(), bytes);
	}

	public static byte[] sha256(CharSequence... text) {
		return sha256(UTF_8, text);
	}

	public static String sha256String(CharSequence... text) {
		return sha256String(UTF_8, text);
	}

	public static byte[] sha256(Charset charset, CharSequence... text) {
		return digest(sha256Digest(), charset, text);
	}

	public static String sha256String(Charset charset, CharSequence... text) {
		return digestString(sha256Digest(), charset, text);
	}

	public static byte[] sha256(File f) throws IOException {
		return digest(sha256Digest(), f);
	}

	public static String sha256String(File f) throws IOException {
		return digestString(sha256Digest(), f);
	}

	public static byte[] sha256(FileChannel c) throws IOException {
		return digest(sha256Digest(), c);
	}

	public static String sha256String(FileChannel c) throws IOException {
		return digestString(sha256Digest(), c);
	}

	public static MessageDigest sha1Digest() {
		try {
			return MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static byte[] sha1(byte... bytes) {
		return digest(sha1Digest(), bytes);
	}

	public static String sha1String(byte... bytes) {
		return digestString(sha1Digest(), bytes);
	}

	public static byte[] sha1(ByteBuffer bytes) {
		return digest(sha1Digest(), bytes);
	}

	public static String sha1String(ByteBuffer bytes) {
		return digestString(sha1Digest(), bytes);
	}

	public static byte[] sha1(CharSequence... text) {
		return sha1(UTF_8, text);
	}

	public static String sha1String(CharSequence... text) {
		return sha1String(UTF_8, text);
	}

	public static byte[] sha1(Charset charset, CharSequence... text) {
		return digest(sha1Digest(), charset, text);
	}

	public static String sha1String(Charset charset, CharSequence... text) {
		return digestString(sha1Digest(), charset, text);
	}

	public static byte[] sha1(File f) throws IOException {
		return digest(sha1Digest(), f);
	}

	public static String sha1String(File f) throws IOException {
		return digestString(sha1Digest(), f);
	}

	public static byte[] sha1(FileChannel c) throws IOException {
		return digest(sha1Digest(), c);
	}

	public static String sha1String(FileChannel c) throws IOException {
		return digestString(sha1Digest(), c);
	}

	public static MessageDigest md5Digest() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static byte[] md5(byte... bytes) {
		return digest(md5Digest(), bytes);
	}

	public static String md5String(byte... bytes) {
		return digestString(md5Digest(), bytes);
	}

	public static byte[] md5(ByteBuffer bytes) {
		return digest(md5Digest(), bytes);
	}

	public static String md5String(ByteBuffer bytes) {
		return digestString(md5Digest(), bytes);
	}

	public static byte[] md5(CharSequence... text) {
		return md5(UTF_8, text);
	}

	public static String md5String(CharSequence... text) {
		return md5String(UTF_8, text);
	}

	public static byte[] md5(Charset charset,
													 final CharSequence... text) {
		return digest(md5Digest(), charset, text);
	}

	public static String md5String(Charset charset,
																 final CharSequence... text) {
		return digestString(md5Digest(), charset, text);
	}

	public static byte[] md5(File f) throws IOException {
		return digest(md5Digest(), f);
	}

	public static String md5String(File f) throws IOException {
		return digestString(md5Digest(), f);
	}

	public static byte[] digest(MessageDigest md, byte... bytes) {
		md.update(bytes);
		return md.digest();
	}

	public static String digestString(MessageDigest md, byte... bytes) {
		return toHexString(digest(md, bytes));
	}

	public static byte[] digest(MessageDigest md, ByteBuffer bytes) {
		md.update(bytes);
		return md.digest();
	}

	public static String digestString(MessageDigest md, ByteBuffer bytes) {
		return toHexString(digest(md, bytes));
	}

	public static byte[] digest(MessageDigest md, CharSequence... text) {
		return digest(md, UTF_8, text);
	}

	public static String digestString(MessageDigest md, CharSequence... text) {
		return digestString(md, UTF_8, text);
	}

	public static byte[] digest(MessageDigest md, Charset charset, CharSequence... text) {
		for (CharSequence s : text) {
			md.update(toByteArray(s, charset));
		}

		return md.digest();
	}

	public static String digestString(MessageDigest md, Charset charset, CharSequence... text) {
		return toHexString(digest(md, charset, text));
	}

	public static byte[] digest(MessageDigest md, File f) throws IOException {
		long len = f.length();

		if (len > 0) {
			try (FileInputStream in = new FileInputStream(f)) {
				byte[] buf = new byte[(int) Math.min(8192, len)];

				for (int i = in.read(buf); i != -1; i = in.read(buf)) {
					md.update(buf, 0, i);
				}
			} catch (IOException ex) {
				throw new IOException(ex);
			}
		}

		return md.digest();
	}

	public static String digestString(MessageDigest md, File f)
			throws IOException {
		return toHexString(digest(md, f));
	}

	public static byte[] digest(MessageDigest md, FileChannel c) throws IOException {
		long size = c.size();

		if (size > 0) {
			ByteBuffer buf = ByteBuffer.allocate((int) Math.min(8192,
					size));
			long pos = 0;

			for (int i = c.read(buf, pos); i != -1; i = c.read(buf, pos)) {
				buf.flip();
				md.update(buf);
				buf.clear();
				pos += i;
			}
		}

		return md.digest();
	}

	public static String digestString(MessageDigest md,
																		FileChannel c) throws IOException {
		return toHexString(digest(md, c));
	}

	public static String digestString(MessageDigest md) {
		return toHexString(md.digest());
	}
}

