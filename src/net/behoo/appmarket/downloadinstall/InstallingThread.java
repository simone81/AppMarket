package net.behoo.appmarket.downloadinstall;

import android.content.Context;
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
				mPkgInfo.mState = Constants.PackageState.installing;
				mPkgInfo.sendPackageStateBroadcast(mContext);
				PackageInstaller pi = new PackageInstaller(mContext);
				ret = pi.installPackage( mPkgInfo.mInstallUri );
			} catch ( Throwable tr ) {
				ret = false;
			}
			mPkgInfo.mState = ( ret ? Constants.PackageState.install_succeeded : Constants.PackageState.install_failed );
			mPkgInfo.sendPackageStateBroadcast(mContext);
			Log.i(TAG, "InstallingThread exit");
		}
	}
}
