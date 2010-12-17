package net.behoo.appmarket.downloadinstall;

import java.net.URI;
import java.util.ArrayList;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.database.PackageDbHelper;
import net.behoo.appmarket.downloadinstall.Constants.PackageState;

import junit.framework.Assert;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.webkit.URLUtil;
import android.net.WebAddress;
import android.provider.Downloads;

public class DownloadInstallService extends Service {
	
	private static final String TAG = "DownloadInstallService";
	private static final String BEHOO_APP_MARKET = "behoo_app_market";
	
	private final IBinder mBinder = new LocalServiceBinder();

	private PackageDbHelper mPkgDBHelper = null;
	private AppUpdateDemonThread mUpdateDaemonThread = null;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			// update the local database
			Bundle bundle = intent.getExtras();
			String code = bundle.getCharSequence(Downloads.COLUMN_NOTIFICATION_EXTRAS).toString();
			Log.i(TAG, "onReceive code: "+code);
			
			// find the local record of "code", and update some field of the table
			boolean bDownloadRet = false;
			String filename = null;
			if (mPkgDBHelper.isCodeExists(code)) {
				// get the file name from database. tbd how to determine the record of table
				// if duplicated, maybe the uri should be used, but I don't know its meaning
				String where = Downloads.COLUMN_NOTIFICATION_EXTRAS + "=?";
				String [] whereArgs = {code};
				String [] projects = {Downloads._DATA, Downloads.COLUMN_DESCRIPTION, Downloads.COLUMN_STATUS};
				Cursor c = context.getContentResolver().query(Downloads.CONTENT_URI, 
						projects, where, whereArgs, null);
				if (null != c) {
					int filenameId = c.getColumnIndexOrThrow(Downloads._DATA);
					int descriptionId = c.getColumnIndexOrThrow(Downloads.COLUMN_DESCRIPTION);
					int statusId = c.getColumnIndexOrThrow(Downloads.COLUMN_STATUS);
					for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
						if (0 == c.getString(descriptionId).compareTo(BEHOO_APP_MARKET)) {
							int status = c.getInt(statusId);
							if (Downloads.isStatusSuccess(status)) {
								bDownloadRet = true;
							}
							filename = "file://"+c.getString(filenameId);
							Log.i(TAG, "The downloaded file: "+filename+" status: "+Integer.toString(status));
							
							ContentValues cv = new ContentValues();
							String statusStr = null;
							if (bDownloadRet) {
								statusStr = Constants.PackageState.download_succeeded.name();
							}
							else {
								statusStr = Constants.PackageState.download_failed.name();
							}
							cv.put(PackageDbHelper.COLUMN_STATE, statusStr);
							cv.put(PackageDbHelper.COLUMN_SRC_PATH, filename);
							mPkgDBHelper.update(code, cv);	
							PackageStateSender.sendPackageStateBroadcast(DownloadInstallService.this, 
									code, statusStr);
							break;
						}
					}
					c.close();
				}
				else {
					Log.w(TAG, "can't find the record from table of download provider database. code "+code);
				}
			}	
			
			// the download has completed successfully!
			if (bDownloadRet) {
				InstallingThread thrd = new InstallingThread(context, code, filename);
				thrd.start();
			}
		}
	};
	
	public class LocalServiceBinder extends Binder {
		public DownloadInstallService getService() {
            return DownloadInstallService.this;
        }
    }
	
	public void onCreate() {
		super.onCreate();
		mPkgDBHelper = new PackageDbHelper(this);
		
		// receiver for download status from download provider
		IntentFilter filter = new IntentFilter(DownloadReceiver.DOWNLOAD_COMPLETED);
		this.registerReceiver(mReceiver, filter);
		
		// the update daemon thread
		mUpdateDaemonThread = new AppUpdateDemonThread(this);
		mUpdateDaemonThread.start();
	}
	
	public void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(mReceiver);
		
		// stop the update daemon thread
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}
	
	public void downloadAndInstall(String url, String mimetype,
            AppInfo appInfo) {
		// check the argument
		if (null == url || null == mimetype || null == appInfo) {
			throw new NullPointerException();
		}
		if (null == appInfo.mAppCode
			|| null == appInfo.mAppVersion
			|| null == appInfo.mAppName) {
			throw new IllegalArgumentException();
		}
		
		// check the application state
		String [] columns = {PackageDbHelper.COLUMN_CODE, PackageDbHelper.COLUMN_STATE};
		String where = PackageDbHelper.COLUMN_CODE + "=?";
		String[] whereValue = {appInfo.mAppCode};
		Cursor c = mPkgDBHelper.select(columns, where, whereValue, null);
		Assert.assertTrue(c.getCount() >= 0 && c.getCount() <= 1);//unique COLUMN_CODE
		boolean bExists = (c.getCount() == 1);
		String statusStr = null;
		PackageState state = PackageState.unknown;
		if (bExists) {
			int index = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_STATE);
			statusStr = c.getString(index);
			try {
				state = PackageState.valueOf(statusStr);
			} catch (Throwable tr){
			}
		}
		c.close();
		
		if (bExists && isDownloadingOrInstalling(state)) {
			Log.i(TAG, "downloading or installing now, code "+appInfo.mAppCode);
			PackageStateSender.sendPackageStateBroadcast(this, appInfo.mAppCode, state.name());
		}
		else {
			// validate the downlaod uri
			URI uri = getURI(url);
			
	        // add/update values to local database
	        ContentValues valuesLocal = new ContentValues();
	        valuesLocal.put(PackageDbHelper.COLUMN_VERSION, appInfo.mAppVersion);
	        valuesLocal.put(PackageDbHelper.COLUMN_APP_NAME, appInfo.mAppName);
	        valuesLocal.put(PackageDbHelper.COLUMN_AUTHOR, appInfo.mAppAuthor);
	        valuesLocal.put(PackageDbHelper.COLUMN_DESC, appInfo.mAppDesc);
	        valuesLocal.put(PackageDbHelper.COLUMN_SRC_PATH, "");
	        valuesLocal.put(PackageDbHelper.COLUMN_PKG_NAME, "");
	        valuesLocal.put(PackageDbHelper.COLUMN_STATE, Constants.PackageState.downloading.name());
	        valuesLocal.put(PackageDbHelper.COLUMN_IMAGE_URL, appInfo.mAppImageUrl);
	        if (bExists) {
	        	mPkgDBHelper.update(appInfo.mAppCode, valuesLocal);
	        }
	        else {
	        	valuesLocal.put(PackageDbHelper.COLUMN_CODE, appInfo.mAppCode);
	        	mPkgDBHelper.insert(valuesLocal);
	        }
	        PackageStateSender.sendPackageStateBroadcast(this, appInfo.mAppCode, 
	        		Constants.PackageState.downloading.name());
	        
			int delCount = getContentResolver().delete(Downloads.CONTENT_URI, where, whereValue);
			Log.i(TAG, "row deleted of code: "+code+" is "+Integer.toString(delCount));
			
	        // add values to download database
	        ContentValues values = new ContentValues();
	        values.put(Downloads.COLUMN_URI, uri.toString());
	        values.put(Downloads.COLUMN_NOTIFICATION_PACKAGE, this.getPackageName());
	        values.put(Downloads.COLUMN_NOTIFICATION_CLASS, DownloadReceiver.class.getCanonicalName());
	        values.put(Downloads.COLUMN_VISIBILITY, Downloads.VISIBILITY_HIDDEN);
	        values.put(Downloads.COLUMN_MIME_TYPE, mimetype);
	        values.put(Downloads.COLUMN_DESCRIPTION, BEHOO_APP_MARKET);
	        values.put(Downloads.COLUMN_NOTIFICATION_EXTRAS, appInfo.mAppCode);
	        values.put(Downloads.COLUMN_DESTINATION, Downloads.DESTINATION_CACHE_PARTITION_PURGEABLE);
	        values.put(Downloads.COLUMN_TOTAL_BYTES, -1);
	        Uri uriInserted = getContentResolver().insert(Downloads.CONTENT_URI, values);
	        Log.i(TAG, "inserted uri "+uriInserted.toString());
		}
	}
	
	public void checkUpdate() {
		mUpdateDaemonThread.checkUpdate();
	}
	
	public ArrayList<AppInfo> getUpdateList() {
		String [] columns = {
			PackageDbHelper.COLUMN_CODE, PackageDbHelper.COLUMN_VERSION,
			PackageDbHelper.COLUMN_APP_NAME, PackageDbHelper.COLUMN_AUTHOR,
			PackageDbHelper.COLUMN_DESC, PackageDbHelper.COLUMN_IMAGE_URL,
		};
		
		String where = PackageDbHelper.COLUMN_STATE + "=?";
		String [] whereArgs = {Constants.PackageState.need_update.name()};
		
		ArrayList<AppInfo> appList = null;
		Cursor c = mPkgDBHelper.select(columns, where, whereArgs, null);
		if (c != null) {
			int codeId = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_CODE);
			int pkgNameId = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_PKG_NAME);
			int versionId = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_VERSION);
			int authorId = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_AUTHOR);
			int descId = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_DESC);
			int imageId = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_IMAGE_URL);
			appList = new ArrayList<AppInfo>();
			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				AppInfo appInfo = new AppInfo(c.getString(pkgNameId),
					c.getString(versionId),
					c.getString(codeId),
					c.getString(authorId),
					c.getString(imageId),
					c.getString(descId));
				appList.add(appInfo);
			}
			c.close();
		}	
		return appList;
	}
	
	public PackageState getAppState(String code) {
		try {
			PackageState state = PackageState.unknown;
			String [] columns = {PackageDbHelper.COLUMN_STATE};
			String where = PackageDbHelper.COLUMN_CODE + "=?";
			String [] whereArgs = {code};
			Cursor c = mPkgDBHelper.select(columns, where, whereArgs, null);
			int index = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_STATE);
			if (c.moveToFirst())
				state = PackageState.valueOf(c.getString(index));
			c.close();
			return state;
		} catch (Throwable tr){
			Log.i(TAG, "getAppState "+tr.getMessage());
			return PackageState.unknown;
		}
	}
	
	public boolean install( String uri ) {
		return true;
	}
	
	private URI getURI(String url) {
		try {
            // Undo the percent-encoding that KURL may have done.
            String newUrl = new String(URLUtil.decode(url.getBytes()));
            // Parse the url into pieces
            WebAddress w = new WebAddress(newUrl);
            String frag = null;
            String query = null;
            String path = w.mPath;
            // Break the path into path, query, and fragment
            if (path.length() > 0) {
                // Strip the fragment
                int idx = path.lastIndexOf('#');
                if (idx != -1) {
                    frag = path.substring(idx + 1);
                    path = path.substring(0, idx);
                }
                idx = path.lastIndexOf('?');
                if (idx != -1) {
                    query = path.substring(idx + 1);
                    path = path.substring(0, idx);
                }
            }
            URI uri = new URI(w.mScheme, w.mAuthInfo, w.mHost, w.mPort, path,
                    query, frag);
            return uri;
        } catch (Exception e) {
            Log.e(TAG, "Could not parse url for download: " + url, e);
            throw new IllegalArgumentException();
        }
	}
	
	private boolean isDownloadingOrInstalling(PackageState state) {
		if (PackageState.download_succeeded == state
			|| PackageState.downloading == state 
			|| PackageState.installing == state ) {
			return true;
		}
		return false;
	}
}
