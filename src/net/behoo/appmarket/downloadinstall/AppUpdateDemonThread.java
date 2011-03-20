package net.behoo.appmarket.downloadinstall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;
import behoo.providers.InstalledAppDb.PackageState;

import net.behoo.appmarket.TokenWrapper;
import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.UrlHelpers;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.util.Log;

public class AppUpdateDemonThread extends Thread {
	private static final String TAG = "AppUpdateDemonThread";
	
	private Context mContext = null;
	private Object mSyncObject = new Object();
	private boolean mExit = false;
	
	public AppUpdateDemonThread(Context context) {	
		mContext = context;
	}
	
	public void run() {
		while (!mExit) {
			Log.i(TAG, "begin to check update");
			
			Map<String, String> codesVersionMap = shouldBeCheckedAppMap();
			
			checkUpdate(codesVersionMap);

			sleepAndWait();
		}
	}
	
	public void checkUpdate() {
		synchronized (mSyncObject) {
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
	
	private void sleepAndWait() {
		// wake up after 30 minutes
		synchronized (mSyncObject) {
			try {
				if (!mExit) {
					mSyncObject.wait(1000 * 60 * 30);
				}
			} catch (InterruptedException e) {
			}
		}
	}
	
	private void checkUpdate(Map<String, String> codesVersionMap) {
		// check update
		if (0 == codesVersionMap.size()) {
			return;
		}
		
		AppListParser appListParser = new AppListParser();
		String reqStr = UrlHelpers.getUpdateRequestString(codesVersionMap);
		Log.d(TAG, "update request string "+reqStr);
		try {
			String token = TokenWrapper.getToken(mContext);
			String url = UrlHelpers.getUpdateUrl(token);
			Log.d(TAG, "checkUpdate url "+url);
			ArrayList<AppInfo> appList =
				appListParser.getUpdateList(url, reqStr, codesVersionMap.size());
			Log.i(TAG, "number of apps should be upgraded: "+Integer.toString(appList.size()));
			
			ContentValues cv = new ContentValues();
			for (int i = 0; i < appList.size(); ++i) {
				AppInfo appInfo = appList.get(i);
				cv.put(InstalledAppDb.COLUMN_STATE, PackageState.need_update.name());
				cv.put(InstalledAppDb.COLUMN_AUTHOR, appInfo.mAppAuthor);
				cv.put(InstalledAppDb.COLUMN_DESC, appInfo.mAppShortDesc);
				cv.put(InstalledAppDb.COLUMN_VERSION, appInfo.mAppVersion);
				cv.put(InstalledAppDb.COLUMN_IMAGE_URL, appInfo.mAppImageUrl);
				cv.put(InstalledAppDb.COLUMN_APP_NAME, appInfo.mAppName);
				String where = InstalledAppDb.COLUMN_CODE+"=?";
				String [] selectionArgs = {appInfo.mAppCode};
				mContext.getContentResolver().update(BehooProvider.INSTALLED_APP_CONTENT_URI, 
						cv, where, selectionArgs);
			}
			
			Intent intent = new Intent(Constants.ACTION_PKG_UPDATE_FINISHED);
			intent.putExtra(Constants.EXTRA_SIZE, appList.size());
			mContext.sendBroadcast(intent);
		} catch(Throwable tr) {
			tr.printStackTrace();
		} finally {
			appListParser.cancel();
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
				int codeId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_CODE);
				int stateId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_STATE);
				int pkgNameId = c.getColumnIndexOrThrow(InstalledAppDb.COLUMN_PKG_NAME);
				String code = null;
				String state = null;
				String pkgName = null;
				
				PackageManager pm = mContext.getPackageManager();
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					code = c.getString(codeId);
					state = c.getString(stateId);
					pkgName = c.getString(pkgNameId);
					PackageState pkgstate = Constants.getStateByString(state);
					if (pkgUninstalled(pm, pkgName, pkgstate)) {
						String where = InstalledAppDb.COLUMN_CODE+"=?";
						String [] selectionArgs = {code};
						cr.delete(BehooProvider.INSTALLED_APP_CONTENT_URI, 
								where, selectionArgs);
					}
					else if (PackageState.install_succeeded == pkgstate) {
						int versionCode = getPkgVersionCode(pm, pkgName);
						if (-1 != versionCode) {
							codesVersionMap.put(code, Integer.toString(versionCode));
						}
					}
				}
				c.close();
			}
		} catch (Throwable tr) {
			tr.printStackTrace();
		}
		Log.i(TAG, "packages should be checked "
				+Integer.toString(codesVersionMap.size()));
		return codesVersionMap;
	}
	
	private boolean pkgUninstalled(PackageManager pm, String pkgName, PackageState state) {
		if (PackageState.install_succeeded == state) {
			try {
				pm.getApplicationInfo(pkgName, PackageManager.GET_UNINSTALLED_PACKAGES);
				return false;
	        } catch (NameNotFoundException e) {
	            return true;
	        }
		}
		return false;
	}
	
	private int getPkgVersionCode(PackageManager pm, String pkgName) {
		int versionCode = -1;
		try {
			PackageInfo info = pm.getPackageInfo(pkgName, PackageManager.GET_UNINSTALLED_PACKAGES);
			versionCode = info.versionCode;
		} catch (Throwable tr) {
		}
		return versionCode;
	}
}
