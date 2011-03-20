package net.behoo.appmarket.downloadinstall;

import java.io.File;
import java.util.Date;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.http.UrlHelpers;

import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;
import behoo.providers.MessageDb;
import behoo.providers.InstalledAppDb.PackageState;

import junit.framework.Assert;
import android.R;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.provider.Downloads;
import android.content.pm.PackageParser;
import android.content.pm.PackageParser.Package;

public class InstallThread extends Thread {
	
	private static final String TAG = "InstallingThread";
	private static final Object SYNC_OBJ = new Object();
	
	private Context mContext = null;
	private AppInfo mAppInfo = null;
	private String mPkgSrcUri = null;
	
	public InstallThread(Context context, AppInfo appInfo, String uri) {
		mContext = context;
		mAppInfo = appInfo;
		mPkgSrcUri = uri;
	}
	
	public void run() {
		synchronized( SYNC_OBJ ) {
			// installation
			boolean ret = false;
			try {
				// update application state
				ContentValues cv = new ContentValues();
				cv.put(InstalledAppDb.COLUMN_STATE, PackageState.installing.name());
				String where = InstalledAppDb.COLUMN_CODE+"=?";
				String [] whereArgs = {mAppInfo.mAppCode};
				mContext.getContentResolver().update(BehooProvider.INSTALLED_APP_CONTENT_URI, 
						cv, where, whereArgs);
				// broadcast the action of application state changed
				PackageStateSender.sendPackageStateBroadcast(mContext, 
						mAppInfo.mAppCode, InstalledAppDb.PackageState.installing.name());
				
				// install it
				PackageInstaller installer = new PackageInstaller(mContext);
				ret = installer.installPackage(Uri.parse(mPkgSrcUri));
			} catch ( Throwable tr ) {
				ret = false;
			}
			
			// update the record of the local database due to the result of installation
			try {
				String status = (ret ? PackageState.install_succeeded.name()
						: PackageState.install_failed.name());
				ContentValues cv2 = new ContentValues();
				cv2.put(InstalledAppDb.COLUMN_STATE, status);
				Date date = new Date();
				cv2.put(InstalledAppDb.COLUMN_INSTALL_DATE, date.getTime());
				if (ret) {
					PackageParser.Package pkgInfo = PackageUtils.getPackageInfo(Uri.parse(mPkgSrcUri));
					cv2.put(InstalledAppDb.COLUMN_PKG_NAME, pkgInfo.packageName);
				}
				
				String where = InstalledAppDb.COLUMN_CODE+"=?";
				String [] whereArgs = {mAppInfo.mAppCode};
				mContext.getContentResolver().update(BehooProvider.INSTALLED_APP_CONTENT_URI, 
						cv2, where, whereArgs);
				
				// send broadcast
				PackageStateSender.sendPackageStateBroadcast(mContext, mAppInfo.mAppCode, status);
	
				// delete the source apk file
				File srcFile = new File(Uri.parse(mPkgSrcUri).getPath());
				if (srcFile.exists()) {
					srcFile.delete();
				}
			} catch (Throwable tr) {
				tr.printStackTrace();
			}
			
			// report to message spot
			if (ret) {
				try {
					String appCode = mAppInfo.mAppCode;
					String messageBody = mAppInfo.mAppName+" "
						+mContext.getString(R.string.message_provider_body);
					
					ContentValues values = new ContentValues();
					Date now = new Date();
					values.put(MessageDb.COLUMN_INTENT_ACTION, Intent.ACTION_VIEW);
					values.put(MessageDb.COLUMN_INTENT_DATA, AppInfo.makeUri(appCode).toString());
					values.put(MessageDb.COLUMN_DATE, (int)(now.getTime()/1000)); // seconds
					values.put(MessageDb.COLUMN_MSG_TITLE, mContext.getString(R.string.message_provider_title));
					values.put(MessageDb.COLUMN_MSG_BODY, messageBody);
					values.put(MessageDb.COLUMN_MSG_PIC_URL, UrlHelpers.getImageUrl(mAppInfo.mAppImageUrl));
					mContext.getContentResolver().insert(BehooProvider.MESSAGE_CONTENT_URI, values);
				} catch (Throwable tr) {
					tr.printStackTrace();
				}
			}
		}
	}
}
