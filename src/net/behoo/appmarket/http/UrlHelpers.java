package net.behoo.appmarket.http;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
	public static String getUpdateRequestString(Map<String, String> codeVersionMap) {
		if (null == codeVersionMap) {
			throw new NullPointerException("codeVersionMap should not be null");
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		sb.append("<BH_S_App_Code_List count=\"");
		sb.append(codeVersionMap.size());
		sb.append("\">");
		Set<String> keys = codeVersionMap.keySet();
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			String code = it.next();
			
			sb.append("<BH_S_App_Code_Version>");
			sb.append("<BH_D_App_Code>");
			sb.append(code);
			sb.append("</BH_D_App_Code>");
			
			sb.append("<BH_D_App_Version_Code>");
			sb.append(codeVersionMap.get(code));
			sb.append("</BH_D_App_Version_Code>");
			sb.append("</BH_S_App_Code_Version>");
		}
		sb.append("</BH_S_App_Code_List>");
		return sb.toString();
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
