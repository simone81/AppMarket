package net.behoo.appmarket.http;

import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.util.Log;
import android.util.Xml;

import net.behoo.appmarket.data.AppInfo;

public class AppListParser {
	private static final String TAG = "AppListParser";
	
	private HttpUtil mHttpUtil = new HttpUtil();
	
	public ArrayList<AppInfo> getAppList(String url, int maxCount) {
		try {
			InputStream is = mHttpUtil.httpGet(url);
			return parse(is, maxCount);
		} catch (Throwable tr) {
			Log.w(TAG, "getAppList "+tr.getLocalizedMessage());
		} finally {
			cancel();
		}
		return null;
	}
	
	public void cancel() {
		mHttpUtil.disconnect();
	}
	
	public static ArrayList<AppInfo> parse(InputStream stream, int maxCount) {
		if (null == stream) {
			throw new NullPointerException("null stream is not expected");
		}
		
		String numOfApp = "0";
		int appCounter = 0;
		ArrayList<AppInfo> appLib = null;
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
                		appLib = new ArrayList<AppInfo>();
                		if (parser.getAttributeCount() == 1) {
                			numOfApp = parser.getAttributeValue(parser.getAttributeNamespace(0), parser.getAttributeName(0));
                		}
                	}
                	else if(tagName.equalsIgnoreCase("BH_S_Application") && appCounter < maxCount) {
                		appInfo = new AppInfo();
                		++appCounter;
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
                	if (tagName.equalsIgnoreCase("BH_S_Application") && null != appInfo && null != appLib) {
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
    	} catch (Throwable tr) {
    		Log.i(TAG, "parse "+tr.getMessage());
    	}
    	Log.i(TAG, "app count by protocol "+numOfApp+" and real count is "+Integer.toString(appLib.size()));
    	return appLib;
	}
}
