package net.behoo.appmarket.http;

import java.util.ArrayList;

public class UrlHelpers {
	private static final String SERVER_URL = "http://www.behoo.net/behoo";
	
	// get the promotion list
	public static String getPromotionUrl(String token) {
		return SERVER_URL+"/GetAppPromotions.php?token="+token;
		//return "http://192.168.1.50/promotion.xml";
	}
	
	// get the update list
	public static String getUpdateUrl(String token) {
		return SERVER_URL+"/CheckAppUpgrade.php?token="+token;
		//return "http://192.168.1.50/update.xml";
	}
	
	public static String getUpdateRequestString(ArrayList<String> codeArr, ArrayList<String> versArr) {
		if (codeArr == null || versArr == null)
			throw new NullPointerException(codeArr == null ? "codeArr should not be null"
					: "versArr should not be null");
		if (codeArr.size() != versArr.size()) 
			throw new IllegalArgumentException("codeArr.size() should equals to versArr.size()");
		
		if (codeArr.size() > 0) {
			String reqStr = new String();
			reqStr = "<BH_S_App_Code_List count=\""+Integer.toString(codeArr.size())+"\">";
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
		//return "http://192.168.1.50/promotion.xml";
	}
	
	// get the application detail
	public static String getAppDetailUrl(String token, String appCode) {
		//return "http://192.168.1.50/appmarketdetail.xml";
		return makeCommonUrl("GetAppDetailtoken.php", token, appCode);
	}
	
	// download application
	public static String getDownloadUrl(String token, String appCode) {
		return makeCommonUrl("DownloadApp.php", token, appCode);
	}
	
	// uninstall application 
	public static String getUnintallUrl(String token, String appCode) {
		return makeCommonUrl("UninstallApp.php", token, appCode);
	}
	
	private static String makeCommonUrl(String action, String token, String appCode) {
		return SERVER_URL+"/"+action+"?token="+token+"&BH_D_App_Code="+appCode;
//		if (0 == appCode.compareTo("93213232")) {
//			return "http://192.168.1.50/suning.apk";
//		}
//		else if (0 == appCode.compareTo("12345678")) {
//			return "http://192.168.1.50/WuMart.apk";
//		}
//		else if (0 == appCode.compareTo("22345678")) {
//			return "http://192.168.1.50/ScreenTests.apk";
//		}
//		else if (0 == appCode.compareTo("32345678")) {
//			return "http://192.168.1.50/QQmusic v1.0.apk";
//		}
//		else if (0 == appCode.compareTo("42345678")) {
//			return "http://192.168.1.50/QIYI_Android_V1.0.2.apk";
//		}
//		else if (0 == appCode.compareTo("52345678")) {
//			return "http://192.168.1.50/MobileTV_v3.1.3.apk";
//		}
//		else if (0 == appCode.compareTo("62345678")) {
//			return "http://192.168.1.50/MaxTV1.0.3_Android.apk";
//		}
//		else if (0 == appCode.compareTo("72345678")) {
//			return "http://192.168.1.50/htsyj v1.0.apk";
//		}
//		else if (0 == appCode.compareTo("82345678")) {
//			return "http://192.168.1.50/fenghuang.apk";
//		}
//		return "";
	}
}
