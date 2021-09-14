package me.aap.utils.misc;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import me.aap.utils.function.Consumer;
import me.aap.utils.function.Supplier;

/**
 * @author Andrey Pavlenko
 */
public class MiscUtils {

	public static <T> T ifNull(T check, @NonNull T otherwise) {
		return (check != null) ? check : otherwise;
	}

	public static <T> T ifNull(T check, @NonNull Supplier<? extends T> otherwise) {
		return (check != null) ? check : otherwise.get();
	}

	public static <T> boolean ifNotNull(T check, @NonNull Consumer<T> c) {
		if (check != null) {
			c.accept(check);
			return true;
		} else {
			return false;
		}
	}

	public static boolean isPackageInstalled(Context ctx, String pkgName) {
		try {
			ctx.getPackageManager().getPackageInfo(pkgName, 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}
}
