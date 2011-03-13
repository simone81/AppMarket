package net.behoo.appmarket.database;

import net.behoo.appmarket.data.AppInfo;
import android.content.Context;
import android.database.Cursor;
import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;
import behoo.providers.InstalledAppDb.PackageState;

public class PkgsProviderWrapper {
	public static PackageState getAppState(Context context, String code) {
		PackageState state = PackageState.unknown;
		Cursor cursor = null;
		try {
			String [] columns = {InstalledAppDb.COLUMN_STATE};
			String where = InstalledAppDb.COLUMN_CODE + "=?";
			String [] whereArgs = {code};
			cursor = context.getContentResolver().query(BehooProvider.INSTALLED_APP_CONTENT_URI, 
					columns, where, whereArgs, null);
			int index = cursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_STATE);
			cursor.moveToFirst();
			state = PackageState.valueOf(cursor.getString(index));
		} catch (Throwable tr){
			tr.printStackTrace();
		} finally {
			if (null != cursor) {
				cursor.close();
			}
		}
		return state;
	}
	
	public static String getAppPkgName(Context context, String code) {
		String pkgName = null;
		Cursor cursor = null;
		try {
			String [] columns = {InstalledAppDb.COLUMN_PKG_NAME};
			String where = InstalledAppDb.COLUMN_CODE + "=?";
			String [] whereArgs = {code};
			cursor = context.getContentResolver().query(BehooProvider.INSTALLED_APP_CONTENT_URI, 
					columns, where, whereArgs, null);
			int index = cursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_PKG_NAME);
			cursor.moveToFirst();
			pkgName = cursor.getString(index);
		} catch (Throwable tr) {
			
		} finally {
			if (null != cursor) {
				cursor.close();
			}
		}
		return pkgName;
	}
	
	public static boolean isAppExists(Context context, String code) {
		boolean bAppExists = false;
		Cursor cursor = null;
		try {
			String [] columns = {InstalledAppDb.COLUMN_ID};
			String where = InstalledAppDb.COLUMN_CODE + "=?";
			String[] whereValue = {code};
			cursor = context.getContentResolver().query(BehooProvider.INSTALLED_APP_CONTENT_URI, 
					columns, where, whereValue, null);
			bAppExists = (null != cursor && cursor.getCount() > 0);
		} catch (Throwable tr) {
			tr.printStackTrace();
		} finally {
			if (null != cursor) {
				cursor.close();
			}
		}
		return bAppExists;
	}
	
	public static AppInfo getAppInfo(Context context, String code) {
		String [] columns = {
				InstalledAppDb.COLUMN_CODE, InstalledAppDb.COLUMN_VERSION,
				InstalledAppDb.COLUMN_APP_NAME, InstalledAppDb.COLUMN_AUTHOR,
				InstalledAppDb.COLUMN_DESC, InstalledAppDb.COLUMN_IMAGE_URL,
		};
		String where = InstalledAppDb.COLUMN_CODE + "=?";
		String [] whereArgs = {code};
		AppInfo appInfo = null;
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(BehooProvider.INSTALLED_APP_CONTENT_URI, 
				columns, where, whereArgs, null);
			cursor.moveToFirst();
		
			int codeId = cursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_CODE);
			int appNameId = cursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_APP_NAME);
			int versionId = cursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_VERSION);
			int authorId = cursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_AUTHOR);
			int descId = cursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_DESC);
			int imageId = cursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_IMAGE_URL);
		
			appInfo = new AppInfo(cursor.getString(appNameId),
					cursor.getString(versionId),
					cursor.getString(codeId),
					cursor.getString(authorId),
					cursor.getString(imageId),
					cursor.getString(descId));
		} catch (Throwable tr) {
			tr.printStackTrace();
		} finally {
			if (null != cursor) {
				cursor.close();
			}
		}
		return appInfo;
	}
}
