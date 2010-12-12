package net.behoo.appmarket.downloadinstall;

import net.behoo.appmarket.database.PackageDbHelper;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.provider.Downloads;

public class InstallingThread extends Thread {
	
	private static final String TAG = "InstallingThread";
	private static final Object SYNC_OBJ = new Object();
	
	private Context mContext = null;
	private String mPkgCode = null;
	private String mPkgSrcUri = null;
	private PackageDbHelper mPkgDBHelper = null;
	
	public InstallingThread(Context context, String code, String uri) {
		mContext = context;
		mPkgCode = code;
		mPkgSrcUri = uri;
		mPkgDBHelper = new PackageDbHelper(context);
	}
	
	public void run() {
		synchronized( SYNC_OBJ ) {
			boolean ret = false;
			try {
				Log.i(TAG, "being installing file: " + mPkgSrcUri);
				
				ContentValues cv = new ContentValues();
				cv.put(PackageDbHelper.COLUMN_STATE, Constants.PackageState.installing.name());
				mPkgDBHelper.update(mPkgCode, cv);
				PackageStateSender.sendPackageStateBroadcast(mContext, 
						mPkgCode, Constants.PackageState.installing.name());
				
				PackageInstaller installer = new PackageInstaller(mContext);
				ret = installer.installPackage(Uri.parse(mPkgSrcUri));
			} catch ( Throwable tr ) {
				ret = false;
			}
			String status = ( ret ? Constants.PackageState.install_succeeded.name()
					: Constants.PackageState.install_failed.name() );
			ContentValues cv = new ContentValues();
			cv.put(PackageDbHelper.COLUMN_STATE, Constants.PackageState.installing.name());
			mPkgDBHelper.update(mPkgCode, cv);
			PackageStateSender.sendPackageStateBroadcast(mContext, mPkgCode, status);
			
			if (ret) {
				String where = Downloads.COLUMN_NOTIFICATION_EXTRAS + "=?";
				String [] whereArgs = {mPkgCode};
				int delCount = mContext.getContentResolver().delete(Downloads.CONTENT_URI, where, whereArgs);
				Log.i(TAG, "row deleted of code: "+code+" is "+Integer.toString(delCount));
			}
			
			Log.i(TAG, "ret: "+status+" "+mPkgSrcUri);
		}
	}
}
