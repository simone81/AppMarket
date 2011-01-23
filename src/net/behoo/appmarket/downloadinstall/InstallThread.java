package net.behoo.appmarket.downloadinstall;

import java.io.File;

import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;

import junit.framework.Assert;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.provider.Downloads;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;

public class InstallThread extends Thread {
	
	private static final String TAG = "InstallingThread";
	private static final Object SYNC_OBJ = new Object();
	
	private Context mContext = null;
	private String mPkgCode = null;
	private String mPkgSrcUri = null;
	
	public InstallThread(Context context, String code, String uri) {
		mContext = context;
		mPkgCode = code;
		mPkgSrcUri = uri;
	}
	
	public void run() {
		synchronized( SYNC_OBJ ) {
			boolean ret = false;
			try {
				Log.i(TAG, "being installing file: " + mPkgSrcUri);
				
				// update application state
				ContentValues cv = new ContentValues();
				cv.put(InstalledAppDb.COLUMN_STATE, Constants.PackageState.installing.name());
				
				String where = InstalledAppDb.COLUMN_CODE+"=?";
				String [] whereArgs = {mPkgCode};
				mContext.getContentResolver().update(BehooProvider.INSTALLED_APP_CONTENT_URI, 
						cv, where, whereArgs);
				
				PackageStateSender.sendPackageStateBroadcast(mContext, 
						mPkgCode, Constants.PackageState.installing.name());
				
				// installing
				PackageInstaller installer = new PackageInstaller(mContext);
				ret = installer.installPackage(Uri.parse(mPkgSrcUri));
			} catch ( Throwable tr ) {
				ret = false;
			}
			
			try {
				// update the local data record
				String status = ( ret ? Constants.PackageState.install_succeeded.name()
						: Constants.PackageState.install_failed.name() );
				ContentValues cv2 = new ContentValues();
				cv2.put(InstalledAppDb.COLUMN_STATE, status);
				if (ret) {
					PackageParser.Package pkgInfo = PackageUtils.getPackageInfo(Uri.parse(mPkgSrcUri));
					cv2.put(InstalledAppDb.COLUMN_PKG_NAME, pkgInfo.packageName);
				}
				
				String where = InstalledAppDb.COLUMN_CODE+"=?";
				String [] whereArgs = {mPkgCode};
				mContext.getContentResolver().update(BehooProvider.INSTALLED_APP_CONTENT_URI, 
						cv2, where, whereArgs);
				
				PackageStateSender.sendPackageStateBroadcast(mContext, mPkgCode, status);
	
				// delete the source apk file
				File srcFile = new File(Uri.parse(mPkgSrcUri).getPath());
				if (srcFile.exists()) {
					srcFile.delete();
				}
				Log.i(TAG, "ret: "+status+" "+mPkgSrcUri);
			} catch (Throwable tr) {
				Log.w(TAG, "update install state and delete src file "+tr.getLocalizedMessage());
			}
		}
	}
}
