package net.behoo.appmarket.downloadinstall;

import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class UninstallThread extends Thread {
	
	private static final String TAG = "UninstallThread";
	
	private Context mContext = null;
	private String mPkgCode = null;
	private String mPkgName = null;
	private String mDownloadUri = null;

	public UninstallThread(Context context, String code, String pkgName, String downloadUri) {
		mContext = context;
		mPkgCode = code;
		mPkgName = pkgName;
		mDownloadUri = downloadUri;
	}
	
	public void run() {
		try {
			PackageInstaller installer = new PackageInstaller(mContext);
			installer.uninstallPackage(mPkgName);
			
			String where = InstalledAppDb.COLUMN_CODE+"=?";
			String [] whereArgs = {mPkgCode};
			mContext.getContentResolver().delete(BehooProvider.INSTALLED_APP_CONTENT_URI, 
					where, whereArgs);
			
			mContext.getContentResolver().delete(Uri.parse(mDownloadUri), null, null);
			
			// intent for inner-process broadcast
			PackageStateSender.sendPackageStateBroadcast(mContext, mPkgCode, 
					InstalledAppDb.PackageState.unknown.name());
			
			// intent for ipc broadcast
			PackageStateSender.sendPackageUninstallBroadcast(mContext, mPkgCode, mPkgName, true);
		} catch (Throwable tr) {
			Log.i(TAG, "run "+tr.getLocalizedMessage());
			PackageStateSender.sendPackageUninstallBroadcast(mContext, mPkgCode, mPkgName, false);
		}
	}
}
