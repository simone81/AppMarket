package net.behoo.appmarket.http;

import java.util.ArrayList;

public class UrlHelpers {
	private static final String SERVER_URL = "http://192.168.1.50/appmarket?";
	
	public static final String APP_MINE_TYPE = "application/vnd.android.package-archive";
	
	// get the promotion list
	public static String getPromotionUrl(String token) {
		return SERVER_URL+"token="+token;
	}
	
	// get the update list
	public static String getUpdateUrl(String token, ArrayList<String> versionsArr) {
		return SERVER_URL+"token="+token;
	}
	
	// get the application list
	public static String getAppListUrl(String token, int startIndex, int pageCount) {
		return String.format("%stoken=%s&startIndex=%d&pageCount=%d", 
				SERVER_URL, token, startIndex, pageCount);
	}
	
	// get the application detail
	public static String getAppDetailUrl(String token, String appCode) {
		return makeCommonUrl(token, appCode);
	}
	
	// download application
	public static String getDownloadUrl(String token, String appCode) {
		return makeCommonUrl(token, appCode);
	}
	
	// uninstall application 
	public static String getUnintallUrl(String token, String appCode) {
		return makeCommonUrl(token, appCode);
	}
	
	private static String makeCommonUrl(String token, String appCode) {
		return String.format("%stoken=%s&appCode=%s", SERVER_URL, token, appCode);
	}
}
