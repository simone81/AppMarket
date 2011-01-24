package net.behoo.appmarket.downloadinstall;

import behoo.providers.InstalledAppDb;

public class Constants {
	/*
	 * 
	 */
	public static final String ACTION_DWONLOAD_INSTALL_STATE = "net.behoo.appmarket.downloadinstall.DownloadAndInstall";
	public static final String PACKAGE_STATE = "package_state";
	public static final String PACKAGE_CODE = "pkg_uri";
	
	/*
	 * 
	 */
	public static final String ACTION_UPDATE_STATE = "net.behoo.appmarket.downloadinstall.PackageUpdate";
	
	static public InstalledAppDb.PackageState getStateByString(String value) {
		try {
			return InstalledAppDb.PackageState.valueOf(value);// need tests tbd
		} catch (Throwable tr) {
			return InstalledAppDb.PackageState.unknown;
		}
	}
}
