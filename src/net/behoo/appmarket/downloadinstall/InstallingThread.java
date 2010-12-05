package net.behoo.appmarket.downloadinstall;

import android.content.Context;
import android.util.Log;

public class InstallingThread extends Thread {
	
	private static final String TAG = "InstallingThread";
	private static final Object SYNC_OBJ = new Object();
	
	private Context mContext = null;
	private PackageInfo mPkgInfo = null;
	
	public InstallingThread(Context context, PackageInfo info) {
		mContext = context;
		mPkgInfo = info;
	}
	
	public void run() {
		synchronized( SYNC_OBJ ) {
			boolean ret = false;
			try {
				Log.i(TAG, "run file: "+mPkgInfo.mInstallUri.toString());
				mPkgInfo.mState = Constants.PackageState.installing;
				mPkgInfo.sendPackageStateBroadcast(mContext);
				PackageInstaller pi = new PackageInstaller(mContext);
				ret = pi.installPackage( mPkgInfo.mInstallUri );
			} catch ( Throwable tr ) {
				ret = false;
			}
			mPkgInfo.mState = ( ret ? Constants.PackageState.install_succeeded : Constants.PackageState.install_failed );
			mPkgInfo.sendPackageStateBroadcast(mContext);
			Log.i(TAG, "InstallingThread exit ret: "+(ret?"success":"fail"));
		}
	}
}
