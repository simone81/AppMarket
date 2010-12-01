package net.behoo.DownloadInstall;

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
	public static final String DOWNLOAD_STAGE = "downloads";
	
	/*
	 * Params for broadcast extra data. 
	 * the extra data is a boolean value which true stands for 
	 * installing succeeded, other wise.
	 */
	public static final String INSTALL_STAGE = "install";
	
	/*
	 * Params for broadcast extra data.
	 * 
	 */
	public static final String PKG_ID = "pkg_id";
	public static final String PAK_URI = "pkg_uri";
	
	public enum PackageState {
		unknown, 			downloading, 
		download_failed, 	installing, 
		install_failed, 	install_succeeded;
	}
}
