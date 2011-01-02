package net.behoo.appmarket.downloadinstall;

import java.io.InputStream;
import java.util.ArrayList;

import behoo.sync.ISyncService;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.database.PackageDbHelper;
import net.behoo.appmarket.downloadinstall.Constants.PackageState;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.UrlHelpers;
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
	private PackageDbHelper mPkgDBHelper = null;
	private Object mSyncObject = new Object();
	private boolean mExit = false;
	private ISyncService mSyncService = null;
	
	public AppUpdateDemonThread(Context context) {	
		mContext = context;
		mPkgDBHelper = new PackageDbHelper(context);
	}
	
	public void run() {
		while (!mExit) {
			// do the task
			String [] columns = {PackageDbHelper.COLUMN_CODE, 
					PackageDbHelper.COLUMN_VERSION,
					PackageDbHelper.COLUMN_STATE,
					PackageDbHelper.COLUMN_PKG_NAME};
			
			// get all the applications that need to be checked
			Cursor c = mPkgDBHelper.select(columns, null, null, null);
			Log.i(TAG, "begin to check update. packages installed by appmarket "+Integer.toString(c.getCount()));
			int codeId = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_CODE);
			int versionId = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_VERSION);
			int stateId = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_STATE);
			int pkgNameId = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_PKG_NAME);
			String code = null;
			String version = null;
			String state = null;
			String pkgName = null;
			PackageManager pm = mContext.getPackageManager();
			ArrayList<String> codes = new ArrayList<String>();
			ArrayList<String> versions = new ArrayList<String>();
			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				code = c.getString(codeId);
				version = c.getString(versionId);
				state = c.getString(stateId);
				pkgName = c.getString(pkgNameId);
				PackageState pkgstate = Constants.getStateByString(state);
				if (pkgUninstalled(pm, pkgName, pkgstate)) {
					mPkgDBHelper.delete(code);
				}
				else if (PackageState.install_succeeded == pkgstate){
					codes.add(code);
					versions.add(version);
				}
			}
			c.close();
			
			Log.i(TAG, "packages need to be checked "+Integer.toString(codes.size()));
			
			// check
			HttpUtil httpUtil = new HttpUtil();
			try {
				String reqStr = UrlHelpers.getUpdateRequestString(codes, versions);
				Log.i(TAG, "update request string "+reqStr);
				
				String url = UrlHelpers.getUpdateUrl(mSyncService.getToken());
				Log.i(TAG, "update url "+url);
				
				InputStream stream = httpUtil.httpPost(url, reqStr);
				ArrayList<AppInfo> appList = AppListParser.parse(stream);
				Log.i(TAG, "the app count need to upgrade "+Integer.toString(appList.size()));
				
				ContentValues cv = new ContentValues();
				for (int i = 0; i < appList.size(); ++i) {
					AppInfo appInfo = appList.get(i);
					cv.put(PackageDbHelper.COLUMN_STATE, PackageState.need_update.name());
					mPkgDBHelper.update(appInfo.mAppCode, cv);
				}
				
				if (0 < appList.size()) {
					Intent intent = new Intent(Constants.ACTION_UPDATE_STATE);
					mContext.sendBroadcast(intent);
				}
				
			} catch(Throwable tr) {
				Log.e(TAG, "check update failed "+tr.getMessage());
			} finally {
				httpUtil.disconnect();
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
}
