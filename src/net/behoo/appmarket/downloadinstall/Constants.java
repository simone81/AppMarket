package net.behoo.appmarket.downloadinstall;

import behoo.providers.InstalledAppDb.PackageState;

public class Constants {
	
	// broadcast action that will be sent when the state of 
	// a package changed.
	public static final String ACTION_PKG_STATE_CHANGED = 
		"net.behoo.appmarket.downloadinstall.PKG_STATE_CHANGED";
	public static final String PACKAGE_CODE = "pkg_uri";
	public static final String PACKAGE_STATE = "pkg_state";
	
	// action to start update checking
	public static final String ACTION_START_CHECK_UPDATE = 
		"net.behoo.appmarket.downloadinstall.START_CHECK_UPDATE";
	
	// broadcast action that will be sent when update checking is finished
	public static final String ACTION_PKG_UPDATE_FINISHED = 
		"net.behoo.appmarket.downloadinstall.PKG_UPDATE_FINISHED";
	public static final String EXTRA_SIZE = "size";
	
	static public PackageState getStateByString(String value) {
		try {
			return PackageState.valueOf(value);
		} catch (Throwable tr) {
			tr.printStackTrace();
			return PackageState.unknown;
		}
	}
}
