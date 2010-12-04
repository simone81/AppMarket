package net.behoo.appmarket.downloadinstall;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import net.behoo.appmarket.downloadinstall.Constants.PackageState;


public class PackageInfo {
	
	public Uri mInstallUri = null;
	public Uri mDownloadUri = null;
	public int mPackageCode = -1;
	public PackageState mState = PackageState.unknown;
	
	public PackageInfo( Uri downloadUri, Uri installUri, int code, PackageState state ) {
		mInstallUri = installUri;
		mDownloadUri = downloadUri;
		mPackageCode = code;
		mState = state;
	}
	
	public void sendPackageStateBroadcast(Context c) {
		Intent i = new Intent(Constants.ACTION_STATE);
		i.putExtra(Constants.PACKAGE_URI, mDownloadUri.toString());
		i.putExtra(Constants.PACKAGE_STATE, mState.name());
		c.sendBroadcast(i);
	}
}
