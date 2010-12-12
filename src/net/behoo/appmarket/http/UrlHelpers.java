package net.behoo.appmarket.http;

import java.util.ArrayList;

import junit.framework.Assert;

public class UrlHelpers {
	private static final String SERVER_URL = "http://192.168.1.50/appmarket?";
	
	public static final String APP_MINE_TYPE = "application/vnd.android.package-archive";
	
	// get the promotion list
	public static String getPromotionUrl(String token) {
		//return SERVER_URL+"token="+token;
		return "http://192.168.1.50/promotion.xml";
	}
	
	// get the update list
	public static String getUpdateUrl(String token) {
		//return SERVER_URL+"token="+token;
		return "http://192.168.1.50/update.xml";
	}
	
	public static String getUpdateRequestString(ArrayList<String> codeArr, ArrayList<String> versArr) {
		if (codeArr == null || versArr == null)
			throw new IllegalArgumentException();
		Assert.assertEquals(codeArr.size(), versArr.size());
		
		if (codeArr.size() > 0) {
			String reqStr = new String();
			reqStr = "<BH_S_App_Code_List count=";
			reqStr += String.format("\"%d\"", codeArr.size());
			reqStr += ">";
			for (int i = 0; i < codeArr.size(); ++i) {
				reqStr += "<BH_S_App_Code_Version>";
					reqStr += "<BH_D_App_Code>";
					reqStr += codeArr.get(i);
					reqStr += "</BH_D_App_Code>";
					
					reqStr += "<BH_D_App_Version>";
					reqStr += versArr.get(i);
					reqStr += "</BH_D_App_Version>";
				reqStr += "</BH_S_App_Code_Version>";
			}
			reqStr += "</BH_S_App_Code_List>";
			return reqStr;
		}
		return null;
	}
	
	// get the application list
	public static String getAppListUrl(String token, int startIndex, int pageCount) {
		//return String.format("%stoken=%s&startIndex=%d&pageCount=%d", 
		//		SERVER_URL, token, startIndex, pageCount);
		return "http://192.168.1.50/promotion.xml";
	}
	
	// get the application detail
	public static String getAppDetailUrl(String token, String appCode) {
		return "http://192.168.1.50/appmarketdetail.xml";
		//return makeCommonUrl(token, appCode);
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
