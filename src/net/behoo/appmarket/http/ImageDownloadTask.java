package net.behoo.appmarket.http;

import java.io.InputStream;

import net.behoo.appmarket.ImageLib;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ImageDownloadTask implements Runnable {
	private static final String TAG = "ImageDownloadTask";
	
	private String mUserData = null;
	private String mImageUrl = null;
	private Handler mHandler = null;
	
	public ImageDownloadTask(String url, String data, Handler handler) {		
		mUserData = data;
		mImageUrl = url;
		mHandler = handler;
	}
	
	public void run() {
		boolean ret = false;
		HttpUtil httpUtil = new HttpUtil();
		try {
			if (null != mImageUrl && mImageUrl.length() > 0) {
				InputStream stream = httpUtil.httpGet(UrlHelpers.getImageUrl(mImageUrl));
				Bitmap bm = BitmapFactory.decodeStream(stream);
				ImageLib.inst().setBitmap(mImageUrl, bm);
				ret = true;
			}
			else {
				ret = false;
			}
		} catch (Throwable e) {
			Log.w(TAG, "download image: "+e.getLocalizedMessage());
			ret = false;
		} finally {
			httpUtil.disconnect();
		}
		
		if (null != mHandler) {
			Message msg = new Message();
			msg.what = ret ? DownloadConstants.MSG_IMG_SUCCEED : DownloadConstants.MSG_IMG_FAILURE;
			Bundle b = new Bundle();
			b.putString(DownloadConstants.MSG_DATA_APPCODE, mUserData);
			b.putString(DownloadConstants.MSG_DATA_URL, mImageUrl);
			msg.setData(b);
			mHandler.sendMessage(msg);
		}
	}
}
