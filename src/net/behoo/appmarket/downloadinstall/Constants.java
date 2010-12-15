package net.behoo.appmarket.downloadinstall;

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
	
	public enum PackageState {
		unknown, 			
		downloading, 
		download_failed, 	download_succeeded,
		installing, 
		install_failed, 	install_succeeded,
		uninstalled,		need_update,
	}
	
	static public PackageState getStateByString(String value) {
		try {
			return PackageState.valueOf(value);// need tests tbd
		} catch (Throwable tr) {
			return PackageState.unknown;
		}
	}
}
