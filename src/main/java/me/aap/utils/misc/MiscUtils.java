package me.aap.utils.misc;

import android.content.Context;
import android.content.pm.PackageManager;

import me.aap.utils.BuildConfig;
import me.aap.utils.app.App;
import me.aap.utils.function.Supplier;

/**
 * @author Andrey Pavlenko
 */
public class MiscUtils {

	public static <T> T ifNull(T check, T otherwise) {
		return (check != null) ? check : otherwise;
	}

	public static <T> T ifNull(T check, Supplier<? extends T> otherwise) {
		return (check != null) ? check : otherwise.get();
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
