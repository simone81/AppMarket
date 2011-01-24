package net.behoo.appmarket.downloadinstall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;
import behoo.sync.ISyncService;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.UrlHelpers;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.util.Log;

public class AppUpdateDemonThread extends Thread {
	private static final String TAG = "AppUpdateDemonThread";
	
	private Context mContext = null;
	private Object mSyncObject = new Object();
	private boolean mExit = false;
	private ISyncService mSyncService = null;
	
	public AppUpdateDemonThread(Context context) {	
		mContext = context;
	}
	
	public void run() {
		while (!mExit) {
			Log.i(TAG, "wake up, begin to check update.");
			
			Map<String, String> codesVersionMap = shouldBeCheckedAppMap();
			Log.i(TAG, "packages need to be checked "+Integer.toString(codesVersionMap.size()));
			
			// check update
			if (0 < codesVersionMap.size()) {
				AppListParser appListParser = new AppListParser();
				String reqStr = UrlHelpers.getUpdateRequestString(codesVersionMap);
				Log.i(TAG, "update request string "+reqStr);
				try {
					String url = UrlHelpers.getUpdateUrl(mSyncService.getToken());
					ArrayList<AppInfo> appList =
						appListParser.getUpdateList(url, reqStr, codesVersionMap.size());
					Log.i(TAG, "number of apps should upgrade: "+Integer.toString(appList.size()));
					
					ContentValues cv = new ContentValues();
					for (int i = 0; i < appList.size(); ++i) {
						AppInfo appInfo = appList.get(i);
						cv.put(InstalledAppDb.COLUMN_STATE, 
								InstalledAppDb.PackageState.need_update.name());
						
						String where = InstalledAppDb.COLUMN_CODE+"=?";
						String [] selectionArgs = {appInfo.mAppCode};
						
						mContext.getContentResolver().update(BehooProvider.INSTALLED_APP_CONTENT_URI, 
								cv, where, selectionArgs);
					}
					
					if (0 < appList.size()) {
						Intent intent = new Intent(Constants.ACTION_UPDATE_STATE);
						mContext.sendBroadcast(intent);
					}
				} catch(Throwable tr) {
					Log.w(TAG, "check update failed "+tr.getMessage());
				} finally {
					appListParser.cancel();
				}
			}

			// wake up after 30 minutes
			synchronized (mSyncObject) {
				try {
					if (!mExit) {
						mSyncObject.wait(1000 * 60 * 30); // 30minutes
					}
				} catch (InterruptedException e) {
					// continue to wait ?
				}
			}
		}
	}
	
	public void checkUpdate(ISyncService syncService) {
		synchronized (mSyncObject) {
			mSyncService = syncService;
			mSyncObject.notify();
		}
	}
	
	public void exit() {
		synchronized (mSyncObject) {
			// may be we should interrupt some operation time-consuming
			mExit = true;
			mSyncObject.notify();
		}
	}
	
	private Map<String, String> shouldBeCheckedAppMap() {
		
		Map<String, String> codesVersionMap = new HashMap<String, String>();
		
		String [] columns = {InstalledAppDb.COLUMN_CODE, 
				InstalledAppDb.COLUMN_VERSION,
				InstalledAppDb.COLUMN_STATE,
				InstalledAppDb.COLUMN_PKG_NAME};
		
		// get all the applications that need to be checked
		try {
			ContentResolver cr = mContext.getContentResolver();
			Cursor c = cr.query(BehooProvider.INSTALLED_APP_CONTENT_URI, 
					columns, null, null, null);
			if (null != c) {
				Log.i(TAG, "packages installed by appmarket "+Integer.toString(c.getCount()));
				
				int codeId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_CODE);
				int versionId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_VERSION);
				int stateId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_STATE);
				int pkgNameId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_PKG_NAME);
				String code = null;
				String version = null;
				String state = null;
				String pkgName = null;
				
				PackageManager pm = mContext.getPackageManager();
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					code = c.getString(codeId);
					version = c.getString(versionId);
					state = c.getString(stateId);
					pkgName = c.getString(pkgNameId);
					InstalledAppDb.PackageState pkgstate = Constants.getStateByString(state);
					if (pkgUninstalled(pm, pkgName, pkgstate)) {
						String where = InstalledAppDb.COLUMN_CODE+"=?";
						String [] selectionArgs = {code};
						cr.delete(BehooProvider.INSTALLED_APP_CONTENT_URI, 
								where, selectionArgs);
					}
					else if (InstalledAppDb.PackageState.install_succeeded == pkgstate){
						codesVersionMap.put(code, version);
					}
				}
				c.close();
			}
		} catch (Throwable tr) {
			
		}
		return codesVersionMap;
	}
	
	private boolean pkgUninstalled(PackageManager pm, String pkgName, InstalledAppDb.PackageState state) {
		if (InstalledAppDb.PackageState.install_succeeded == state) {
			try {
				pm.getApplicationInfo(pkgName, PackageManager.GET_UNINSTALLED_PACKAGES);
				return false;
	        } catch (NameNotFoundException e) {
	            return true;
	        }
		}
		return false;
	}
}
