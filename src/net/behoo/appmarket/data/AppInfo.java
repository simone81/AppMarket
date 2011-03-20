package net.behoo.appmarket.data;

import android.net.Uri;


public class AppInfo {
	
	public static final String MIMETYPE = "application/vnd.android.package-archive";
	public static final String AppScheme = "acode";
	public static final String AppAuthority = "behoo_apps";
	public static final String AppPath = "code";
	
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
	public String mAppScreenShorts1 = "";
	public String mAppScreenShorts2 = "";
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
		mAppScreenShorts1 = new String(source.mAppScreenShorts1);
		mAppScreenShorts2 = new String(source.mAppScreenShorts2);
		mAppReview = new String(source.mAppReview);
		mAppChangelog = new String(source.mAppChangelog);
	}
	
	public boolean equalsAppCode(AppInfo appInfo) {
		return (null != appInfo && appInfo.mAppCode.equals(mAppCode));
	}
	
	public static final Uri makeUri(String appCode) {
		if (null == appCode) {
			throw new NullPointerException();
		}
		StringBuilder builder = new StringBuilder();
		builder.append(AppScheme);
		builder.append("://");
		builder.append(AppAuthority);
		builder.append('/');
		builder.append(AppPath);
		builder.append('/');
		builder.append(appCode);
		return Uri.parse(builder.toString());
	}
}
