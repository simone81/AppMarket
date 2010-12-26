package net.behoo.appmarket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.graphics.drawable.Drawable;

public class ImageLib {
	private static ImageLib mImageLib = null;
	
	private Map<String, Drawable> mRealLib = new HashMap<String, Drawable>();
	private Set<String> mDownloadFlags = new HashSet<String>();
	
	public static ImageLib inst() {
		if (null == mImageLib) {
			mImageLib =  new ImageLib();
		}
		return mImageLib;
	}
	
	public Drawable getDrawable(String url) {
		synchronized (this) {
			if (mRealLib.containsKey(url)) {
				return mRealLib.get(url);
			}
			return null;
		}
	}
	
	public void setDrawable(String url, Drawable d) {
		synchronized (this) {
			mRealLib.put(url, d);
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
