package net.behoo.appmarket.downloadinstall;


import java.net.URI;
import java.util.ArrayList;

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
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			// update the local database
			Bundle bundle = intent.getExtras();
			String code = bundle.getCharSequence(Downloads.COLUMN_NOTIFICATION_EXTRAS).toString();
			Log.i(TAG, "onReceive code: "+" "+code);
			
			// find the local record of "code", and update some field of the table
			boolean bDownloadRet = false;
			String filename = null;
			if (mPkgDBHelper.isCodeExists(code)) {
				// get the file name from database
				String where = Downloads.COLUMN_NOTIFICATION_EXTRAS + "=?";
				String [] whereArgs = {code};
				String [] projects = {Downloads._DATA, Downloads.COLUMN_DESCRIPTION, Downloads.COLUMN_STATUS};
				Cursor c = context.getContentResolver().query(Downloads.CONTENT_URI, 
						projects, where, whereArgs, null);
				Assert.assertNotNull(c);
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
						cv.put(PackageDbHelper.COLUMN_FULL_NAME, filename);
						mPkgDBHelper.update(code, cv);	
						PackageStateSender.sendPackageStateBroadcast(DownloadInstallService.this, 
								code, statusStr);
						break;
					}
				}
				c.close();
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
		IntentFilter filter = new IntentFilter(DownloadReceiver.DOWNLOAD_COMPLETED);
		this.registerReceiver(mReceiver, filter);
	}
	
	public void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(mReceiver);
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}
	
	public void downloadAndInstall(String url, String mimetype,
            String appCode, String version, String appName, String author,
            String desc) {
		// check the argument
		if (null == url || null == mimetype || null == appCode || null == version) {
			throw new IllegalArgumentException();
		}
		
		// check the app state
		String [] columns = {PackageDbHelper.COLUMN_CODE, PackageDbHelper.COLUMN_STATE};
		String where = PackageDbHelper.COLUMN_CODE + "=?";
		String[] whereValue = {appCode};
		Cursor c = mPkgDBHelper.select(columns, where, whereValue, null);
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
			Log.i(TAG, "is downloading or installing now");
			PackageStateSender.sendPackageStateBroadcast(this, appCode, state.name());
		}
		else {
			// validate the downlaod uri
			URI uri = getURI(url);
			
	        // add/update values to local database
	        ContentValues valuesLocal = new ContentValues();
	        valuesLocal.put(PackageDbHelper.COLUMN_VERSION, version);
	        valuesLocal.put(PackageDbHelper.COLUMN_APP_NAME, appName);
	        valuesLocal.put(PackageDbHelper.COLUMN_AUTHOR, author);
	        valuesLocal.put(PackageDbHelper.COLUMN_DESC, desc);
	        valuesLocal.put(PackageDbHelper.COLUMN_FULL_NAME, "");
	        valuesLocal.put(PackageDbHelper.COLUMN_PKG_NAME, "");
	        valuesLocal.put(PackageDbHelper.COLUMN_STATE, Constants.PackageState.downloading.name());
	        if (bExists) {
	        	mPkgDBHelper.update(appCode, valuesLocal);
	        }
	        else {
	        	valuesLocal.put(PackageDbHelper.COLUMN_CODE, appCode);
	        	mPkgDBHelper.insert(valuesLocal);
	        }
	        PackageStateSender.sendPackageStateBroadcast(this, appCode, Constants.PackageState.downloading.name());
	        
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
	        values.put(Downloads.COLUMN_NOTIFICATION_EXTRAS, appCode);
	        values.put(Downloads.COLUMN_DESTINATION, Downloads.DESTINATION_CACHE_PARTITION_PURGEABLE);
	        values.put(Downloads.COLUMN_TOTAL_BYTES, -1);
	        Uri uriInserted = getContentResolver().insert(Downloads.CONTENT_URI, values);
	        Log.i(TAG, "inserted uri "+uriInserted.toString());
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
