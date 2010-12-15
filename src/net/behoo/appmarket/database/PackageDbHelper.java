package net.behoo.appmarket.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PackageDbHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "packages_db";
	public static final int DATABASE_VERSION = 1;
	public static final String TABLE_NAME = "packages_table";
	
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_CODE = "code";
	public static final String COLUMN_VERSION = "version";
	public static final String COLUMN_PKG_NAME = "pkg_name"; // the full package name
	public static final String COLUMN_APP_NAME = "app_name"; // the software display name
	public static final String COLUMN_AUTHOR = "author";
	public static final String COLUMN_DESC = "description";
	public static final String COLUMN_SRC_PATH = "full_name";// file name with path
	public static final String COLUMN_STATE = "state";
	public static final String COLUMN_IMAGE_URL = "image_url";
	
	public PackageDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		String sql = "CREATE TABLE " + TABLE_NAME + "(" + COLUMN_ID + " integer primary key autoincrement," 
			+ COLUMN_CODE + " text unique,"
			+ COLUMN_VERSION + " text," 
		  	+ COLUMN_PKG_NAME + " text,"
		  	+ COLUMN_APP_NAME + " text,"
		  	+ COLUMN_AUTHOR + " text,"
		  	+ COLUMN_DESC + " text,"
		  	+ COLUMN_SRC_PATH + " text,"
		  	+ COLUMN_STATE + " text,"
		  	+ COLUMN_IMAGE_URL + " text"
			+ ");";

		db.execSQL(sql);
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		String sql = " DROP TABLE IF EXISTS " + TABLE_NAME;
		db.execSQL(sql);
		onCreate(db);
	}
	
	public Cursor select(String[] columns, String selection, String[] selectionArgs, String orderBy) {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, orderBy);
		return cursor;
	}
	
	public boolean isCodeExists(String code) {
		String [] columns = {PackageDbHelper.COLUMN_ID};
		String where = COLUMN_CODE + "=?";
		String[] whereValue = { code };
		Cursor c = select(columns, where, whereValue, null);
		boolean ret = (null != c && c.getCount() > 0);
		c.close();
		return ret;
	}
	
	public long insert(ContentValues cv) {
		SQLiteDatabase db = this.getWritableDatabase();
		long row = db.insert(TABLE_NAME, null, cv);
		return row;
	}

	public void delete(String code) {
		SQLiteDatabase db = this.getWritableDatabase();
		if (null != code) {
			String where = COLUMN_CODE + "=?";
			String[] whereValue = { code };
			db.delete(TABLE_NAME, where, whereValue);
		}
		else {
			// delete all rows
			db.delete(TABLE_NAME, null, null);
		}
	}

	public void update(String code, ContentValues cv) {
		SQLiteDatabase db = this.getWritableDatabase();
		String where = COLUMN_CODE + "=?";
		String[] whereValue = { code };
		db.update(TABLE_NAME, cv, where, whereValue);
	}
}
