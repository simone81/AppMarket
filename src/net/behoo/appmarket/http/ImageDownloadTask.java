package net.behoo.appmarket.http;

import java.io.InputStream;

import net.behoo.appmarket.data.AppInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ImageDownloadTask implements Runnable {
	private static final String TAG = "ImageDownloadTask";
	
	private AppInfo mAppInfo = null;
	private Handler mHandler = null;
	
	public ImageDownloadTask(AppInfo appInfo, Handler handler) {
		if (null == appInfo) {
			throw new NullPointerException();
		}
		if (null == appInfo.mAppCode) {
			throw new IllegalArgumentException();
		}
		
		mAppInfo = appInfo;
		mHandler = handler;
	}
	
	public void run() {
		boolean ret = false;
		try {
			if (null != mAppInfo.mAppImageUrl && mAppInfo.mAppImageUrl.length() > 0) {
				HttpUtil httpUtil = new HttpUtil();
				InputStream stream = httpUtil.httpGet(UrlHelpers.getImageUrl(mAppInfo.mAppImageUrl));
				Drawable drawable = Drawable.createFromStream(stream, "src");
				mAppInfo.setDrawable(drawable);
				ret = true;
			}
			else {
				ret = false;
			}
		} catch (Throwable e) {
			String image = (mAppInfo.mAppImageUrl == null ? "null" : mAppInfo.mAppImageUrl);
			Log.w(TAG, "failed to download image of code: "+mAppInfo.mAppCode+" url: "+image);
			ret = false;
		}
		
		if (null != mHandler) {
			Message msg = new Message();
			msg.what = ret ? DownloadConstants.MSG_IMG_SUCCEED : DownloadConstants.MSG_IMG_FAILURE;
			Bundle b = new Bundle();
			b.putString(DownloadConstants.MSG_DATA_APPCODE, mAppInfo.mAppCode);
			msg.setData(b);
			mHandler.sendMessage(msg);
		}
	}
}
