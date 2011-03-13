package net.behoo.appmarket.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.CursorWrapper;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;

public class InstalledPkgsProvider extends ContentProvider {

	private static final String TAG = "InstalledPkgsProvider";
	
	// tbd what's the meaning of these two statements
	private static final String DOWNLOAD_LIST_TYPE = "vnd.android.cursor.dir/apps";
	private static final String DOWNLOAD_TYPE = "vnd.android.cursor.item/apps";
	
	public static final String DATABASE_NAME = "installedapps.db";
	public static final int DATABASE_VERSION = 1;
	public static final String TABLE_NAME = "installedapps";
	
	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	private static final int APPS = 0;
	private static final int APP_ID = 1;
	static {
		 sURIMatcher.addURI("installedapps", "apps", APPS);
		 sURIMatcher.addURI("installedapps", "apps/#", APP_ID);
	}
	
	private PackageDbHelper mDatabaseHelper = null;
	
	private class PackageDbHelper extends SQLiteOpenHelper {
		public PackageDbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			createTable(db);	
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			dropTable(db);
			createTable(db);
		}
	}

	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = 0;
		int match = sURIMatcher.match(uri);
		switch (match) {
		case APPS:
		case APP_ID:
			SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
			count = db.delete(TABLE_NAME, selection, selectionArgs);
			break;
		default:
			Log.w(TAG, "deleting unknown/invalid URI: " + uri);
			throw new UnsupportedOperationException("Cannot delete URI: " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	public String getType(Uri uri) {
		int match = sURIMatcher.match(uri);
		switch (match) {
		case APPS:
			return DOWNLOAD_LIST_TYPE;
		case APP_ID:
			return DOWNLOAD_TYPE;
		default:
			Log.e(TAG, "calling getType on an unknown URI: " + uri);
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	public Uri insert(Uri uri, ContentValues values) {
		if (sURIMatcher.match(uri) != APPS) {
			Log.d(TAG, "calling insert on an unknown/invalid URI: " + uri);
			throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
		}
		
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		long rowId = db.insert(TABLE_NAME, null, values);
		
		Uri ret = null;
		if (-1 != rowId) {
			ret = Uri.parse(BehooProvider.INSTALLED_APP_CONTENT_URI + "/" + rowId);
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return ret;
	}

	public boolean onCreate() {
		mDatabaseHelper = new PackageDbHelper(getContext());
		return false;
	}

	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		int match = sURIMatcher.match(uri);
		switch (match) {
		case APPS: {
			SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
			Cursor cursor = db.query(TABLE_NAME, projection, selection, 
					selectionArgs, null, null, sortOrder);
			if (null != cursor) {
				cursor = new ReadOnlyCursorWrapper(cursor);
			}
			if (null != cursor) {
				cursor.setNotificationUri(getContext().getContentResolver(), uri);
			}
			return cursor;
		}
		case APP_ID:
			throw new UnsupportedOperationException("Cannot query URI: " + uri);
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int match = sURIMatcher.match(uri);
		switch (match) {
		case APPS: {
			SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
			int count = db.update(TABLE_NAME, values, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return count;
		}
		case APP_ID:
			throw new UnsupportedOperationException("Cannot query URI: " + uri);
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}
	
	private void createTable(SQLiteDatabase db) {
		try {
			String sql = "CREATE TABLE " + TABLE_NAME + 
			"(" 
			+ InstalledAppDb.COLUMN_ID + " integer primary key autoincrement," 
			+ InstalledAppDb.COLUMN_CODE + " text unique,"
			+ InstalledAppDb.COLUMN_VERSION + " text," 
		  	+ InstalledAppDb.COLUMN_PKG_NAME + " text,"
		  	+ InstalledAppDb.COLUMN_APP_NAME + " text,"
		  	+ InstalledAppDb.COLUMN_AUTHOR + " text,"
		  	+ InstalledAppDb.COLUMN_DESC + " text,"
		  	+ InstalledAppDb.COLUMN_STATE + " text,"
		  	+ InstalledAppDb.COLUMN_IMAGE_URL + " text,"
		  	+ InstalledAppDb.COLUMN_DOWNLOAD_URI + " text unique,"
		  	+ InstalledAppDb.COLUMN_INSTALL_DATE + " integer"
			+ ");";
	
			db.execSQL(sql);
		} catch (SQLException ex) {
			Log.e(TAG, "couldn't create table in installed applications database");
			throw ex;
		}
	}
	
	private void dropTable(SQLiteDatabase db) {
         try {
        	 String sql = " DROP TABLE IF EXISTS " + TABLE_NAME;
        	 db.execSQL(sql);
         } catch (SQLException ex) {
             Log.e(TAG, "couldn't drop table in installed applications database");
             throw ex;
         }
     }

	public class ReadOnlyCursorWrapper extends CursorWrapper implements CrossProcessCursor {
		public ReadOnlyCursorWrapper(Cursor cursor) {
		    super(cursor);
		    mCursor = (CrossProcessCursor) cursor;
		}

		public boolean deleteRow() {
		    throw new SecurityException("Installed application provider manager cursors are read-only");
		}

		public boolean commitUpdates() {
			throw new SecurityException("Installed application provider manager cursors are read-only");
		}

		public void fillWindow(int pos, CursorWindow window) {
			mCursor.fillWindow(pos, window);
		}

		public CursorWindow getWindow() {
			return mCursor.getWindow();
		}

		public boolean onMove(int oldPosition, int newPosition) {
			return mCursor.onMove(oldPosition, newPosition);
		}

		private CrossProcessCursor mCursor;
	}
}
