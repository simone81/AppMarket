package net.behoo.appmarket.downloadinstall;

import net.behoo.appmarket.database.PackageDbHelper;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class UninstallThread extends Thread {
	
	private static final String TAG = "UninstallThread";
	
	private Context mContext = null;
	private String mPkgCode = null;
	private String mPkgName = null;
	private String mDownloadUri = null;
	private PackageDbHelper mPkgDBHelper = null;

	public UninstallThread(Context context, String code, String pkgName, String downloadUri) {
		mContext = context;
		mPkgCode = code;
		mPkgName = pkgName;
		mDownloadUri = downloadUri;
		mPkgDBHelper = new PackageDbHelper(context);
	}
	
	public void run() {
		try {
			PackageInstaller installer = new PackageInstaller(mContext);
			installer.uninstallPackage(mPkgName);
			
			mPkgDBHelper.delete(mPkgCode);
			mContext.getContentResolver().delete(Uri.parse(mDownloadUri), null, null);
		} catch (Throwable tr) {
			Log.i(TAG, "run "+tr.getLocalizedMessage());
		}
	}
}
