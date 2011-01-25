package net.behoo.appmarket.downloadinstall;

import android.content.Context;
import android.content.Intent;

public class PackageStateSender {
	
	static public void sendPackageStateBroadcast(Context c, String code, String state) {
		Intent i = new Intent(Constants.ACTION_DWONLOAD_INSTALL_STATE);
		i.putExtra(Constants.PACKAGE_CODE, code);
		i.putExtra(Constants.PACKAGE_STATE, state);
		c.sendBroadcast(i);
	}
	
	static public void sendPackageUninstallBroadcast(Context c, String code,
			String pkgName, boolean ret) {
		Intent i = new Intent(behoo.content.Intent.ACTION_PACKAGE_UNINSTALLED);
		i.putExtra(behoo.content.Intent.PKG_CODE, code);
		i.putExtra(behoo.content.Intent.PKG_NAME, pkgName);
		i.putExtra(behoo.content.Intent.PKG_RESULT, true);
		c.sendBroadcast(i);
	}
}
