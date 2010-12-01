package net.behoo.DownloadInstall;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

public class InstallingThread extends Thread {
	
	private static final String TAG = "InstallingThread";
	
	private Context mContext = null;
	private Object mSyncObject = null;
	private PackageInfo mPkgInfo = null;
	
	public InstallingThread(Context context, Object syncObject, PackageInfo info) {
		mContext = context;
		mSyncObject = syncObject;
		mPkgInfo = info;
	}
	
	public void Run() {
		synchronized( mSyncObject ) {
			boolean ret = false;
			try {
				PackageInstaller pi = new PackageInstaller(mContext);
				ret = pi.installPackage( Uri.parse("file://" + "/sdcard/ScreenTests.apk") );
			} catch ( Throwable tr ) {
				ret = false;
			}
			mPkgInfo.mState = ( ret ? Constants.PackageState.install_succeeded : Constants.PackageState.install_failed );
			// broadcast
			Intent i = new Intent(Constants.ACTION_STATE);
			i.putExtra(Constants.PKG_ID, 123);
			i.putExtra(Constants.PAK_URI, mPkgInfo.mUrl);
			i.putExtra(Constants.DOWNLOAD_STAGE, ret);
			mContext.sendBroadcast(i);
			Log.i(TAG, "InstallingThread exit");
		}
	}
}
