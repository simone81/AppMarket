package net.behoo.appmarket.http;

import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.util.Log;
import android.util.Xml;

import net.behoo.appmarket.data.AppInfo;

public class AppListParser {
	private static final String TAG = "AppListParser";
	
	static public ArrayList<AppInfo> parse(InputStream stream) {
		if (null == stream)
			throw new NullPointerException("can't parse null input stream");
		
		// for testing tbd
//		try {
//			BufferedReader r = new BufferedReader(new InputStreamReader(stream));
//			StringBuilder total = new StringBuilder();
//			String line;
//			while ((line = r.readLine()) != null) {
//			    total.append(line);
//			}
//		} catch (IOException e) {
//		}
		
		String appCount = "0";
		ArrayList<AppInfo> appLib = new ArrayList<AppInfo>();
    	XmlPullParser parser = Xml.newPullParser();
    	try {
    		parser.setInput(stream, null);
	        int eventType = parser.getEventType();
	        boolean done = false;

	        String tagName = null;
	        AppInfo appInfo = null;
	        while (eventType != XmlPullParser.END_DOCUMENT && !done) {
	        	switch (eventType) {
                case XmlPullParser.START_TAG:
                	tagName = parser.getName();
                	if(tagName.equalsIgnoreCase("BH_S_App_Promotion_List")
                	   || tagName.equalsIgnoreCase("BH_S_App_List")) {
                		// do nothing
                		if (parser.getAttributeCount() == 1) {
                			appCount = parser.getAttributeValue(parser.getAttributeNamespace(0), parser.getAttributeName(0));
                		}
                	}
                	else if(tagName.equalsIgnoreCase("BH_S_Application")) {
                		appInfo = new AppInfo();
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
                		Log.i("AppListParser", "code "+appInfo.mAppCode);
                		appInfo.setSummaryInit(true);
                		appLib.add(appInfo);
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
    		Log.i(TAG, "exception "+tr.getMessage());
    	}
    	Log.i(TAG, "app count by protocol "+appCount+" and real count is "+Integer.toString(appLib.size()));
    	return appLib;
	}
}
