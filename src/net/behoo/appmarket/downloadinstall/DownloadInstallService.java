package net.behoo.appmarket.downloadinstall;

import java.net.URI;
import java.util.ArrayList;

import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;
import behoo.providers.InstalledAppDb.PackageState;
import behoo.sync.ISyncService;

import net.behoo.appmarket.TokenWrapper;
import net.behoo.appmarket.data.AppInfo;

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
	
	public static final String ACTION_INSTALL_APP = 
		"net.behoo.appmarket.downloadinstall.INSTALL_APP";
	public static final String EXTRA_HTTP_URL = "http_url";
	public static final String EXTRA_MIME = "mime";
	public static final String EXTRA_APP_CODE = "app_code";
	public static final String EXTRA_APP_VERSION = "app_version";
	public static final String EXTRA_APP_AUTHOR = "app_author";
	public static final String EXTRA_APP_NAME = "app_name";
	public static final String EXTRA_APP_SHORTDESC = "desc";
	public static final String EXTRA_APP_IMAGE_URL = "image_url";
	
	private static final String TAG = "DownloadInstallService";
	private static final String BEHOO_APP_MARKET = "behoo_app_market";
	
	private final IBinder mBinder = new LocalServiceBinder();
	private AppUpdateDemonThread mUpdateDaemonThread = null;
	
	public class LocalServiceBinder extends Binder {
		public DownloadInstallService getService() {
            return DownloadInstallService.this;
        }
    }
	
	public void onCreate() {
		super.onCreate();
		// validate the application state of the local database
		validatePackageState();
		
		// the update daemon thread
		mUpdateDaemonThread = new AppUpdateDemonThread(this);
		mUpdateDaemonThread.start();
	}
	
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public void onDestroy() {
		super.onDestroy();
		
		// stop the update daemon thread
		mUpdateDaemonThread.exit();
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (null != intent) {
			String action = intent.getAction();
		    if (null != action) {
		    	if (action.equals(ACTION_INSTALL_APP)) {
		    		AppInfo appInfo = new AppInfo(
		    				intent.getStringExtra(EXTRA_APP_NAME),
		    				intent.getStringExtra(EXTRA_APP_VERSION),
		    				intent.getStringExtra(EXTRA_APP_CODE),
		    				intent.getStringExtra(EXTRA_APP_AUTHOR),
		    				intent.getStringExtra(EXTRA_APP_IMAGE_URL),
		    				intent.getStringExtra(EXTRA_APP_SHORTDESC));
		    		try {
		    			downloadApp(intent.getStringExtra(EXTRA_HTTP_URL),
		    				intent.getStringExtra(EXTRA_MIME), appInfo);
		    		} catch (Throwable tr) {
		    			tr.printStackTrace();
		    		}
		    	}
		    	else if (action.equals(DownloadReceiver.DOWNLOAD_COMPLETED)) {
		    		String uri = intent.getStringExtra(DownloadReceiver.EXTRA_DOWNLOAD_URI);
		    		String appCode = intent.getStringExtra(DownloadReceiver.EXTRA_APP_CODE);
		    		Log.i(TAG, "onStartCommand downloaded completed: "+appCode+" uri: "+uri);
		    		installApp(uri, appCode);
		    	}
		    	else if (action.equals(behoo.content.Intent.ACTION_TO_UNINSTALL_PKG)) {
		    		String code = intent.getStringExtra(behoo.content.Intent.PKG_TO_UNINSTALL_CODE);
		    		uninstall(code);
		    	}
		    	else if (action.equals(Constants.ACTION_START_CHECK_UPDATE)) {
		    		mUpdateDaemonThread.checkUpdate();
		    	}
		    }
		}
	    return START_STICKY;
	}
	
	private void downloadApp(String url, String mimetype, AppInfo appInfo) {
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
		boolean bExists = false;
		String statusStr = null;
		PackageState state = PackageState.unknown;
		String downloadUri = null;
		
		String [] columns = {InstalledAppDb.COLUMN_CODE, 
				InstalledAppDb.COLUMN_STATE,
				InstalledAppDb.COLUMN_DOWNLOAD_URI};
		String where = InstalledAppDb.COLUMN_CODE + "=?";
		String[] whereValue = {appInfo.mAppCode};
		Cursor c = this.getContentResolver().query(BehooProvider.INSTALLED_APP_CONTENT_URI, 
				columns, where, whereValue, null);
		if (null != c) {
			// make sure there is only one record for every application in the local database
			Assert.assertTrue(c.getCount() >= 0 && c.getCount() <= 1);
			bExists = (c.getCount() == 1 && c.moveToFirst());
			if (bExists) {
				int index = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_STATE);
				statusStr = c.getString(index);
				state = PackageState.valueOf(statusStr);
				
				index = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_DOWNLOAD_URI);
				downloadUri = c.getString(index);	
			}
			c.close();
		}
		
		if (bExists && isDownloadingOrInstalling(state)) {
			// the application is donwloading, just send a broadcast
			Log.i(TAG, "downloading or installing now, code "+appInfo.mAppCode);
			PackageStateSender.sendPackageStateBroadcast(this, appInfo.mAppCode, state.name());
		}
		else {
			// validate the downlaod uri
			try {
				URI uri = getURI(url);
				Log.i(TAG, "downloadAndInstall "+uri.toString());
				
				// insert values to download provider database and start the download
		        ContentValues values = new ContentValues();
		        values.put(Downloads.COLUMN_URI, uri.toString());
		        values.put(Downloads.COLUMN_NOTIFICATION_PACKAGE, this.getPackageName());
		        values.put(Downloads.COLUMN_NOTIFICATION_CLASS, DownloadReceiver.class.getCanonicalName());
		        values.put(Downloads.COLUMN_VISIBILITY, Downloads.VISIBILITY_HIDDEN);
		        values.put(Downloads.COLUMN_MIME_TYPE, mimetype);
		        values.put(Downloads.COLUMN_DESCRIPTION, appInfo.mAppCode);
		        values.put(Downloads.COLUMN_NOTIFICATION_EXTRAS, appInfo.mAppCode);
		        values.put(Downloads.COLUMN_DESTINATION, Downloads.DESTINATION_CACHE_PARTITION_PURGEABLE);
		        values.put(Downloads.COLUMN_TOTAL_BYTES, -1);
		        Uri uriInserted = getContentResolver().insert(Downloads.CONTENT_URI, values);
		        
		        // add/update values to local database
		        if (null != uriInserted) {
		        	Log.i(TAG, "downloadAndInstall, add task "+uriInserted.toString());
			        ContentValues valuesLocal = new ContentValues(10);
			        valuesLocal.put(InstalledAppDb.COLUMN_VERSION, appInfo.mAppVersion);
			        valuesLocal.put(InstalledAppDb.COLUMN_APP_NAME, appInfo.mAppName);
			        valuesLocal.put(InstalledAppDb.COLUMN_AUTHOR, appInfo.mAppAuthor);
			        valuesLocal.put(InstalledAppDb.COLUMN_DESC, appInfo.mAppShortDesc);
			        valuesLocal.put(InstalledAppDb.COLUMN_STATE, PackageState.downloading.name());
			        valuesLocal.put(InstalledAppDb.COLUMN_DOWNLOAD_URI, uriInserted.toString());
			        valuesLocal.put(InstalledAppDb.COLUMN_IMAGE_URL, appInfo.mAppImageUrl);
			        if (bExists) {
			        	try {
			        		// we hope there is only one record for one application in the DownloadProvider database,
			        		// so delete the existing record and add a new one
			        		getContentResolver().delete(Uri.parse(downloadUri), null, null);
			        	} catch (Throwable tr) {
			        	}
			        	this.getContentResolver().update(BehooProvider.INSTALLED_APP_CONTENT_URI, 
			        			valuesLocal, where, whereValue);
			        }
			        else {
			        	valuesLocal.put(InstalledAppDb.COLUMN_CODE, appInfo.mAppCode);
			        	getContentResolver().insert(BehooProvider.INSTALLED_APP_CONTENT_URI, valuesLocal);
			        }
			        PackageStateSender.sendPackageStateBroadcast(this, appInfo.mAppCode, 
			        		InstalledAppDb.PackageState.downloading.name()); 
		        }
		        else {
		        	PackageStateSender.sendPackageStateBroadcast(this, appInfo.mAppCode, 
		        			InstalledAppDb.PackageState.download_failed.name()); 
		        }
			} catch (Throwable tr) {
				tr.printStackTrace();
			}
		}
	}
	
	private void uninstall(String code) {
		String [] columns = {InstalledAppDb.COLUMN_PKG_NAME,
				InstalledAppDb.COLUMN_DOWNLOAD_URI};
		String where = InstalledAppDb.COLUMN_CODE+"=?";
		String [] whereArgs = {code};
		Cursor c = this.getContentResolver().query(BehooProvider.INSTALLED_APP_CONTENT_URI, 
				columns, where, whereArgs, null);
		String pkgName = null;
		String downloadUri = null;
		if (null != c && c.moveToFirst()) {
			int pkgNameId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_PKG_NAME);
			int downloadUriId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_DOWNLOAD_URI);
			pkgName = c.getString(pkgNameId);
			downloadUri = c.getString(downloadUriId);
			c.close();
			
			Log.i(TAG, "uninstall, code: "+code+" download uri: "+downloadUriId);
			UninstallThread thrd = new UninstallThread(this, 
					code, pkgName, downloadUri);
			thrd.start();
		}
		else {
			String tempPkgName = (null != pkgName ? pkgName : "");
			PackageStateSender.sendPackageUninstallBroadcast(this, code, tempPkgName, false);
		}
	}
	
	public ArrayList<AppInfo> getUpdateList() {
		String where = InstalledAppDb.COLUMN_STATE + "=?";
		String [] whereArgs = {InstalledAppDb.PackageState.need_update.name()};
		return getAppList(where, whereArgs);
	}
	
	public ArrayList<AppInfo> getAppList(String where, String [] whereArgs) {
		String [] columns = {
				InstalledAppDb.COLUMN_CODE, InstalledAppDb.COLUMN_VERSION,
				InstalledAppDb.COLUMN_APP_NAME, InstalledAppDb.COLUMN_AUTHOR,
				InstalledAppDb.COLUMN_DESC, InstalledAppDb.COLUMN_IMAGE_URL,
		};
		
		ArrayList<AppInfo> appList = null;
		Cursor c = this.getContentResolver().query(BehooProvider.INSTALLED_APP_CONTENT_URI, 
				columns, where, whereArgs, null);
		if (c != null) {
			int codeId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_CODE);
			int appNameId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_APP_NAME);
			int versionId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_VERSION);
			int authorId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_AUTHOR);
			int descId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_DESC);
			int imageId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_IMAGE_URL);
			appList = new ArrayList<AppInfo>();
			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				AppInfo appInfo = new AppInfo(c.getString(appNameId),
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
	
	public InstalledAppDb.PackageState getAppState(String code) {
		try {
			InstalledAppDb.PackageState state = InstalledAppDb.PackageState.unknown;
			String [] columns = {InstalledAppDb.COLUMN_STATE};
			String where = InstalledAppDb.COLUMN_CODE + "=?";
			String [] whereArgs = {code};
			Cursor c = this.getContentResolver().query(BehooProvider.INSTALLED_APP_CONTENT_URI, 
					columns, where, whereArgs, null);
			int index = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_STATE);
			if (c.moveToFirst())
				state = InstalledAppDb.PackageState.valueOf(c.getString(index));
			c.close();
			return state;
		} catch (Throwable tr){
			Log.w(TAG, "getAppState "+tr.getMessage());
			return InstalledAppDb.PackageState.unknown;
		}
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
            e.printStackTrace();
            throw new IllegalArgumentException();
        }
	}
	
	private boolean isDownloadingOrInstalling(InstalledAppDb.PackageState state) {
		if (InstalledAppDb.PackageState.download_succeeded == state
			|| InstalledAppDb.PackageState.downloading == state 
			|| InstalledAppDb.PackageState.installing == state ) {
			return true;
		}
		return false;
	}
	
	private void validatePackageState() {
		// validate the package state if some extreme states happened, for example
		// power off by user .
		try {
			String [] columns = {InstalledAppDb.COLUMN_DOWNLOAD_URI,
					InstalledAppDb.COLUMN_CODE};
			String where = "("+InstalledAppDb.COLUMN_STATE +"=?) OR (" 
				+ InstalledAppDb.COLUMN_STATE + "=?) OR ("
				+ InstalledAppDb.COLUMN_STATE + "=?)";
			String [] whereArgs = {InstalledAppDb.PackageState.downloading.name(),
					InstalledAppDb.PackageState.download_succeeded.name(),
					InstalledAppDb.PackageState.installing.name()};
			Cursor c = this.getContentResolver().query(BehooProvider.INSTALLED_APP_CONTENT_URI, 
					columns, where, whereArgs, null);
			if (null != c) {
				int uriId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_DOWNLOAD_URI);
				int codeId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_CODE);
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					
					String uri = c.getString(uriId);
					String code = c.getString(codeId);
					Log.i(TAG, "validatePackageState code:"+code+ " uri: "+uri);
						
					getContentResolver().delete(Uri.parse(uri), null, null);
						
					String localwhere = InstalledAppDb.COLUMN_CODE + "=?";
					String[] whereValue = {code};
					this.getContentResolver().delete(BehooProvider.INSTALLED_APP_CONTENT_URI, 
						localwhere, whereValue);
				}
				c.close();
			}
		}catch (Throwable tr) {
			tr.printStackTrace();
		}
	}
	
	private boolean isPackageExistInDb(String code) {
		String [] columns = {InstalledAppDb.COLUMN_ID};
		String where = InstalledAppDb.COLUMN_CODE + "=?";
		String[] whereValue = {code};
		Cursor c = this.getContentResolver().query(BehooProvider.INSTALLED_APP_CONTENT_URI, 
				columns, where, whereValue, null);
		boolean ret = (null != c && c.getCount() > 0);
		if (null != c) {
			c.close();
		}
		return ret;
	}
	
	private void installApp(String uri, String code) {
		// get the downloaded file state
		boolean bDownloadRet = false;
		String filename = "";
		if (isPackageExistInDb(code)) {
			// get the file name from database. tbd how to determine the record of table
			// if duplicated, maybe the uri should be used, but I don't know its meaning
			String [] projects = {Downloads._DATA, Downloads.COLUMN_STATUS};
			Cursor c = this.getContentResolver().query(Uri.parse(uri), 
					projects, null, null, null);
			if (null != c && c.getCount() > 0 && c.moveToFirst()) {
				int filenameId = c.getColumnIndexOrThrow(Downloads._DATA);
				int statusId = c.getColumnIndexOrThrow(Downloads.COLUMN_STATUS);
				int status = c.getInt(statusId);
				if (Downloads.isStatusSuccess(status)) {
					bDownloadRet = true;
					filename = "file://"+c.getString(filenameId);
				}			
			}
			if (null != c) {
				c.close();
			}
		}	
		
		// update the installed applications database
		ContentValues cv = new ContentValues();
		String statusStr = null;
		if (bDownloadRet) {
			statusStr = InstalledAppDb.PackageState.download_succeeded.name();
		}
		else {
			statusStr = InstalledAppDb.PackageState.download_failed.name();
		}
		cv.put(InstalledAppDb.COLUMN_STATE, statusStr);
		String where = InstalledAppDb.COLUMN_CODE+"=?";
		String [] selectionArgs = {code};
		getContentResolver().update(BehooProvider.INSTALLED_APP_CONTENT_URI, 
				cv, where, selectionArgs);	
		PackageStateSender.sendPackageStateBroadcast(this, 
				code, statusStr);
		
		// the download has completed successfully!
		if (bDownloadRet) {
			InstallThread thrd = new InstallThread(this, code, filename);
			thrd.start();
		}
	}
}
