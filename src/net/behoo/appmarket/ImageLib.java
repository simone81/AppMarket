package net.behoo.appmarket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.graphics.Bitmap;

public class ImageLib {
	private static ImageLib mImageLib = null;
	
	private Map<String, Bitmap> mRealLib = new HashMap<String, Bitmap>();
	private Set<String> mDownloadFlags = new HashSet<String>();
	
	public static ImageLib inst() {
		if (null == mImageLib) {
			mImageLib =  new ImageLib();
		}
		return mImageLib;
	}
	
	public Bitmap getBitmap(String url) {
		synchronized (this) {
			if (mRealLib.containsKey(url)) {
				return mRealLib.get(url);
			}
			return null;
		}
	}
	
	public void setBitmap(String url, Bitmap bm) {
		synchronized (this) {
			mRealLib.put(url, bm);
		}
	}
	
	public void setDownloadFlag(String url) {
		synchronized (this) {
			mDownloadFlags.add(url);
		}
	}
	
	public boolean isImageDownloading(String url) {
		synchronized (this) {
			return mDownloadFlags.contains(url);
		}
	}
	
	private ImageLib() {}
}
