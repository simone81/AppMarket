package net.behoo.appmarket.http;

import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import behoo.util.HttpUtil;

import android.util.Log;
import android.util.Xml;

import net.behoo.appmarket.data.AppInfo;

public class AppListParser {
	public static final int INVALID_TOTAL = -1;
	
	private static final String TAG = "AppListParser";
	
	private int mProtocolType = 0;
	private HttpUtil mHttpUtil = new HttpUtil();
	private ArrayList<AppInfo> mAppList = null;
	private int mTotalCount = 0;
	
	public ArrayList<AppInfo> getPromotionList(String url, int maxCount) {
		try {
			mProtocolType = 0;
			mTotalCount = INVALID_TOTAL;
			mAppList = new ArrayList<AppInfo>();
			InputStream is = mHttpUtil.httpGet(url);
			return parse(is, maxCount);
		} catch (Throwable tr) {
			tr.printStackTrace();
		} finally {
			cancel();
		}
		return mAppList;
	}
	
	public ArrayList<AppInfo> getAppList(String url, int maxCount) {
		try {
			mProtocolType = 1;
			mTotalCount = INVALID_TOTAL;
			mAppList = new ArrayList<AppInfo>();
			InputStream is = mHttpUtil.httpGet(url);
			return parse(is, maxCount);
		} catch (Throwable tr) {
			Log.w(TAG, "getAppList "+tr.getLocalizedMessage());
		} finally {
			cancel();
		}
		return mAppList;
	}
	
	public ArrayList<AppInfo> getUpdateList(String url, String requestStr, int maxCount) {
		try {
			mProtocolType = 2;
			mTotalCount = INVALID_TOTAL;
			mAppList = new ArrayList<AppInfo>();
			InputStream stream = mHttpUtil.httpPost(url, requestStr);
			return parse(stream, maxCount);
		} catch (Throwable tr) {
			Log.w(TAG, "getUpdateList "+tr.getLocalizedMessage());
		} finally {
			cancel();
		}
		return mAppList;
	}
	
	public int getAppListTotal() {
		return mTotalCount;
	}
	
	public void cancel() {
		mHttpUtil.disconnect();
	}
	
	private ArrayList<AppInfo> parse(InputStream stream, int maxCount) {
		String numOfApp = "0";
		int appCounter = 0;
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
                	if (tagName.equalsIgnoreCase("BH_S_App_Promotion_List")
                	   || tagName.equalsIgnoreCase("BH_S_App_List")) {
                		
                		// GetAppList
                		String namespace = parser.getAttributeNamespace(0);
                		if (1 == mProtocolType && 2 == parser.getAttributeCount()) {
                			for (int i = 0; i < 2; ++i) {
                				String attrName = parser.getAttributeName(0);
                				if (0 == attrName.compareToIgnoreCase("total")) {
                					String strCount = parser.getAttributeValue(namespace, attrName);
                    				mTotalCount = Integer.valueOf(strCount);
                    			}
                				else if (0 == attrName.compareToIgnoreCase("count")) {
                					numOfApp = parser.getAttributeValue(namespace, attrName);
                				}
                			}
                		}
                		if ((0 == mProtocolType || 2 == mProtocolType)
                			&& (1 == parser.getAttributeCount())) {
                			numOfApp = parser.getAttributeValue(namespace, parser.getAttributeName(0));
                		}
                	}
                	else if(tagName.equalsIgnoreCase("BH_S_Application")) {
                		if (appCounter < maxCount) {
	                		appInfo = new AppInfo();
	                		++appCounter;
                		}
                		else {
                			done = true;
                		}
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
                    	else {
                    		parser.nextText();
                    	}
                	}
                	break;
                case XmlPullParser.END_TAG:
                	tagName = parser.getName();
                	if (tagName.equalsIgnoreCase("BH_S_Application") && null != appInfo) {
                		mAppList.add(appInfo);
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
    		tr.printStackTrace();
    	}
    	Log.i(TAG, "app count by protocol "+numOfApp+
    			" and real count is "+Integer.toString(mAppList.size()));
    	return mAppList;
	}
}
