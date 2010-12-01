package net.behoo.DownloadInstall;

import android.net.Uri;

import net.behoo.DownloadInstall.Constants.PackageState;


public class PackageInfo {
	
	public String mUrl = null;
	public int mPackageCode = -1;
	public PackageState mState = PackageState.unknown;
	public Uri mApkUri = null;
	
	public PackageInfo( String url, int code, PackageState state ) {
		mUrl = url;
		mPackageCode = code;
		mState = state;
	}
}
