package net.behoo.appmarket.http;

import java.io.InputStream;

import net.behoo.appmarket.data.AppInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ImageDownloadTask implements Runnable {
	private AppInfo mAppInfo = null;
	private Handler mHandler = null;
	
	public ImageDownloadTask(AppInfo appInfo, Handler handler) {
		mAppInfo = appInfo;
		mHandler = handler;
	}
	
	public void run() {
		boolean ret = false;
		try {
			HttpUtil httpUtil = new HttpUtil();
			InputStream stream = httpUtil.httpGet(mAppInfo.mAppImageUrl);
			Drawable drawable = Drawable.createFromStream(stream, "src");
			mAppInfo.setDrawable(drawable);
			ret = true;
		} catch (Throwable e) {
		}
		
		Message msg = new Message();
		msg.what = ret ? DownloadConstants.MSG_IMG_SUCCEED : DownloadConstants.MSG_IMG_FAILURE;
		Bundle b = new Bundle();
		b.putString(DownloadConstants.MSG_DATA_APPCODE, mAppInfo.mAppCode);
		msg.setData(b);
		mHandler.sendMessage(msg);
	}
}
