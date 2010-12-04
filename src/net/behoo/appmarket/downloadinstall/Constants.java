package net.behoo.appmarket.downloadinstall;

public class Constants {
	
	/*
	 * The broadcast intent filter
	 */
	public static final String ACTION_STATE = "net.behoo.AppMarket.DownloadAndInstall";
	
	/*
	 * Params for broadcast extra data. 
	 * When the download has finished.
	 * intent.putExtra(DOWNLOAD_STAGE, ret)
	 * ret: true or false
	 */
	public static final String PACKAGE_STATE = "package_state";
	
	/*
	 * Params for broadcast extra data.
	 */
	public static final String PACKAGE_URI = "pkg_uri";
	
	public enum PackageState {
		unknown, 			
		downloading, 
		download_failed, 	download_succeeded,
		installing, 
		install_failed, 	install_succeeded;
	}
}
