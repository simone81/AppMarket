package net.behoo.appmarket.downloadinstall;

import net.behoo.appmarket.http.UrlHelpers;
import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;
import behoo.util.HttpUtil;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class UninstallThread extends Thread {
	
	private static final String TAG = "UninstallThread";
	
	private Context mContext = null;
	private String mPkgCode = null;
	private String mPkgName = null;
	private String mDownloadUri = null;
	private String mUserToken = null;
	
	public UninstallThread(Context context, String code, String pkgName, String downloadUri, String userToken) {
		mContext = context;
		mPkgCode = code;
		mPkgName = pkgName;
		mDownloadUri = downloadUri;
		mUserToken = userToken;
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
			
			reportPkgUnintalled();
			
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
	
	private void reportPkgUnintalled() {
		String url = UrlHelpers.getUnintallUrl(mUserToken, mPkgCode);
		HttpUtil http = new HttpUtil();
		try {
			http.httpGet(url);
		}
		catch (Throwable tr) {
			tr.printStackTrace();
		}
		finally {
			http.disconnect();
		}
	}
}
