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
}
