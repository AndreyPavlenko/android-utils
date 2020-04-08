package me.aap.utils.misc;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * @author Andrey Pavlenko
 */
public class MiscUtils {

	public static boolean isPackageInstalled(Context ctx, String pkgName) {
		try {
			ctx.getPackageManager().getPackageInfo(pkgName, 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}
}
