package net.behoo.appmarket.http;

import java.util.ArrayList;

public class UrlHelpers {
	private static final String SERVER_URL = "http://www.behoo.net/behoo";
	
	// get the promotion list
	public static String getPromotionUrl(String token) {
		return SERVER_URL+"/GetAppPromotions.php?token="+token;
	}
	
	// get the update list
	public static String getUpdateUrl(String token) {
		return SERVER_URL+"/CheckAppUpgrade.php?token="+token;
	}
	
	// make the update request string
	public static String getUpdateRequestString(ArrayList<String> codeArr, ArrayList<String> versArr) {
		if (codeArr == null || versArr == null)
			throw new NullPointerException(codeArr == null ? "codeArr should not be null"
					: "versArr should not be null");
		if (codeArr.size() != versArr.size()) 
			throw new IllegalArgumentException("codeArr.size() should equals to versArr.size()");
		
		if (codeArr.size() > 0) {
			String reqStr = new String();
			reqStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
			reqStr += "<BH_S_App_Code_List count=\""+Integer.toString(codeArr.size())+"\">";
			for (int i = 0; i < codeArr.size(); ++i) {
				reqStr += "<BH_S_App_Code_Version>";
					reqStr += ("<BH_D_App_Code>"+codeArr.get(i)+"</BH_D_App_Code>");
					
					reqStr += "<BH_D_App_Version>"+versArr.get(i)+"</BH_D_App_Version>";
				reqStr += "</BH_S_App_Code_Version>";
			}
			reqStr += "</BH_S_App_Code_List>";
			return reqStr;
		}
		return null;
	}
	
	// get the application list
	public static String getAppListUrl(String token, int startIndex, int pageCount) {
		return SERVER_URL+"/GetAppList.php?token="+token
			+"&BH_I_Start_Index="+Integer.toString(startIndex)
			+"&BH_I_Page_Count="+Integer.toString(pageCount);
	}
	
	// get the application detail
	public static String getAppDetailUrl(String token, String appCode) {
		return makeCommonUrl("GetAppDetail.php", token, appCode);
	}
	
	// download application
	public static String getDownloadUrl(String token, String appCode) {
		return makeCommonUrl("DownloadApp.php", token, appCode);
	}
	
	// uninstall application 
	public static String getUnintallUrl(String token, String appCode) {
		return makeCommonUrl("UninstallApp.php", token, appCode);
	}
	
	// image url
	public static String getImageUrl(String relativePos) {
		return SERVER_URL + "/" + relativePos;
	}
	
	private static String makeCommonUrl(String action, String token, String appCode) {
		return SERVER_URL+"/"+action+"?token="+token+"&BH_D_App_Code="+appCode;
	}
}
