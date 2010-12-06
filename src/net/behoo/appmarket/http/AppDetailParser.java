package net.behoo.appmarket.http;

import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;

import android.util.Xml;

import net.behoo.appmarket.data.AppInfo;

public class AppDetailParser {
	static public void parse(InputStream stream, AppInfo appInfo) {
		if(null == stream || null == appInfo) {
			throw new IllegalArgumentException();
		}
		
		XmlPullParser parser = Xml.newPullParser();
    	try {
    		parser.setInput(stream, null);
	        int eventType = parser.getEventType();
	        boolean done = false;
	        
	        String tagName = null;
	        boolean bDataValid = true;
	        while (eventType != XmlPullParser.END_DOCUMENT && !done) {
	        	switch (eventType) {
                case XmlPullParser.START_TAG:
                	tagName = parser.getName();
                	if( tagName.equalsIgnoreCase("BH_S_App_Detail")) {
                		bDataValid = true;
                	}
                	else if (bDataValid) {
                		if (tagName.equalsIgnoreCase("BH_D_App_Name")) {
                			bDataValid = appInfo.mAppName.equalsIgnoreCase(parser.nextText());
	                	}
	                	else if(tagName.equalsIgnoreCase("BH_D_App_Version")) {
	                		bDataValid = appInfo.mAppVersion.equalsIgnoreCase(parser.nextText());
	                	}
	                	else if(tagName.equalsIgnoreCase("BH_D_App_Author")) {
	                		bDataValid = appInfo.mAppAuthor.equalsIgnoreCase(parser.nextText());
	                	}
	                	else if(tagName.equalsIgnoreCase("BH_D_App_Pic_Url")) {
	                		bDataValid = appInfo.mAppImageUrl.equalsIgnoreCase(parser.nextText());
	                	}
	                	else if(tagName.equalsIgnoreCase("BH_D_App_Short_Desc")) {
	                		bDataValid = appInfo.mAppShortDesc.equalsIgnoreCase(parser.nextText());
	                	}
	                	else if(tagName.equalsIgnoreCase("BH_D_App_Code")) {
	                		bDataValid = appInfo.mAppCode.equalsIgnoreCase(parser.nextText());
	                	}
	                	else if(tagName.equalsIgnoreCase("BH_D_App_Long_Desc")) {
	                		appInfo.mAppDesc = parser.nextText();
	                	}
						else if(tagName.equalsIgnoreCase("BH_I_App_Size_KB")) {
							appInfo.mAppSize = parser.nextText();                		
						}
						else if(tagName.equalsIgnoreCase("BH_I_App_Remoter_Score")) {
							appInfo.mAppRemoteCntlScore = parser.nextText();
						}
						else if(tagName.equalsIgnoreCase("BH_D_App_Screenshots")) {
							appInfo.mAppScreenShorts = parser.nextText();
						}
						else if(tagName.equalsIgnoreCase("BH_D_App_Review")) {
							appInfo.mAppReview = parser.nextText();
						}
						else if(tagName.equalsIgnoreCase("BH_D_App_Change_Log")) {
							appInfo.mAppChangelog = parser.nextText();
						}
						else {
							done = true;
						}
                	}
                	break;
                case XmlPullParser.END_TAG:
                	tagName = parser.getName();
                	if (tagName.equalsIgnoreCase("BH_S_App_Detail") && bDataValid) {
                		appInfo.setSummaryInit(true);
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
	}
}
