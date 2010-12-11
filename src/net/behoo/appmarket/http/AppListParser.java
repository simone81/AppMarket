package net.behoo.appmarket.http;

import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.util.Xml;

import net.behoo.appmarket.data.AppInfo;

public class AppListParser {
	static public ArrayList<AppInfo> parse(InputStream stream) {
		if (null == stream)
			throw new IllegalArgumentException();
		
		ArrayList<AppInfo> appLib = new ArrayList<AppInfo>();
    	XmlPullParser parser = Xml.newPullParser();
    	try {
    		parser.setInput(stream, null);
	        int eventType = parser.getEventType();
	        boolean done = false;
	        
	        boolean bAppUpdate = false;
	        String tagName = null;
	        AppInfo appInfo = null;
	        while (eventType != XmlPullParser.END_DOCUMENT && !done) {
	        	switch (eventType) {
                case XmlPullParser.START_TAG:
                	tagName = parser.getName();
                	if( tagName.equalsIgnoreCase("BH_S_Application")) {
                		appInfo = new AppInfo();
                	}
                	else if (tagName.equalsIgnoreCase("BH_S_App_Promotion_List")) {
                		bAppUpdate = false;
                	}
                	else if (tagName.equalsIgnoreCase("BH_S_App_List")) {
                		bAppUpdate = true;
                	}
                	else if (null != appInfo){
                		if (tagName.equalsIgnoreCase("BH_D_App_Name")) {
                			appInfo.mAppName = parser.nextText();
                    	}
                    	else if(tagName.equalsIgnoreCase("BH_D_App_Version")) {
                    		appInfo.mAppVersion = parser.nextText();
                    	}
                    	else if(tagName.equalsIgnoreCase("BH_D_App_Author")) {
                    		appInfo.mAppAuthor = parser.nextText();
                    	}
                    	else if(tagName.equalsIgnoreCase("BH_D_App_Pic_Url")) {
                    		appInfo.mAppImageUrl = parser.nextText();
                    	}
                    	else if(tagName.equalsIgnoreCase("BH_D_App_Short_Desc")) {
                    		appInfo.mAppShortDesc = parser.nextText();
                    	}
                    	else if(tagName.equalsIgnoreCase("BH_D_App_Code")) {
                    		appInfo.mAppCode = parser.nextText();
                    	}
                	}
                	break;
                case XmlPullParser.END_TAG:
                	tagName = parser.getName();
                	if (tagName.equalsIgnoreCase("BH_S_Application") && null != appInfo) {
                		appInfo.setSummaryInit(true);
                		if(bAppUpdate || containsApp(appLib, appInfo.mAppCode)) {
                			appLib.add(appInfo);
                		}
                		appInfo = null;
                	}
                	break;
                case XmlPullParser.START_DOCUMENT:
                case XmlPullParser.END_DOCUMENT:
                default:
                	break;
	        	}
	        	
	        	eventType = parser.next();
	        }
    	} catch ( Throwable tr ) {
    	}
    	return appLib;
	}
	
	static private boolean containsApp(ArrayList<AppInfo> appLib, String appCode) {
		for (int i = 0; i < appLib.size(); ++i) {
			if (appCode == appLib.get(i).mAppCode) {
				return true;
			}
		}
		return false;
	}
}
