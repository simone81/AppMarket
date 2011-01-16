package net.behoo.appmarket.data;


public class AppInfo {
	
	public static final String MIMETYPE = "application/vnd.android.package-archive";
	
	// summary info
	public String mAppName = "";
	public String mAppVersion = "";
	public String mAppCode = "";
	public String mAppAuthor = "";
	public String mAppImageUrl = "";
	public String mAppShortDesc = "";
	
	// details info
	public String mAppDesc = "";
	public String mAppSize = "";
	public String mAppRemoteCntlScore = "";// remote control score
	public String mAppScreenShorts = "";
	public String mAppReview = "";
	public String mAppChangelog = "";
	
	public AppInfo() {
	}
	
	public AppInfo(String appName, 
			String appVersion,
			String appCode,
			String appAuthor,
			String appImageUrl,
			String appShortDesc) {
		mAppName = appName;
		mAppVersion = appVersion;
		mAppCode = appCode;
		mAppAuthor = appAuthor;
		mAppImageUrl = appImageUrl;
		mAppShortDesc = appShortDesc;
	}
	
	public AppInfo(AppInfo source) {
		mAppName = new String(source.mAppName);
		mAppVersion = new String(source.mAppVersion);
		mAppCode = new String(source.mAppCode);
		mAppAuthor = new String(source.mAppAuthor);
		mAppImageUrl = new String(source.mAppImageUrl);
		mAppShortDesc = new String(source.mAppShortDesc);
		
		// details info
		mAppDesc = new String(source.mAppDesc);
		mAppSize = new String(source.mAppSize);
		mAppRemoteCntlScore = new String(source.mAppRemoteCntlScore);// remote control score
		mAppScreenShorts = new String(source.mAppScreenShorts);
		mAppReview = new String(source.mAppReview);
		mAppChangelog = new String(source.mAppChangelog);
	}
}
