package net.behoo.appmarket.downloadinstall;

import net.behoo.appmarket.TokenWrapper;
import net.behoo.appmarket.http.UrlHelpers;
import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;
import behoo.util.HttpUtil;
import android.content.Context;
import android.net.Uri;

public class UninstallThread extends Thread {
	
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
			
			// delete the application record from local database
			String where = InstalledAppDb.COLUMN_CODE+"=?";
			String [] whereArgs = {mPkgCode};
			mContext.getContentResolver().delete(BehooProvider.INSTALLED_APP_CONTENT_URI, 
					where, whereArgs);
			// delete the application record from download provider
			mContext.getContentResolver().delete(Uri.parse(mDownloadUri), null, null);
			
			// report it to the server
			reportPkgUnintalled();
			
			// intent for inner-process broadcast
			PackageStateSender.sendPackageStateBroadcast(mContext, mPkgCode, 
					InstalledAppDb.PackageState.unknown.name());
			
			// intent for ipc broadcast
			PackageStateSender.sendPackageUninstallBroadcast(mContext, mPkgCode, mPkgName, true);
		} catch (Throwable tr) {
			tr.printStackTrace();
			PackageStateSender.sendPackageUninstallBroadcast(mContext, mPkgCode, mPkgName, false);
		}
	}
	
	private void reportPkgUnintalled() {
		HttpUtil http = new HttpUtil();
		try {
			String token = TokenWrapper.getToken(mContext);
			String url = UrlHelpers.getUnintallUrl(token, mPkgCode);
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
