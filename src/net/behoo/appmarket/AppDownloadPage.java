package net.behoo.appmarket;

import net.behoo.appmarket.database.PackageDbHelper;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

public class AppDownloadPage extends Activity {
	private static final String TAG = "AppDownloadPage";
	
	private ListView mListView = null;
	private PackageDbHelper mPkgDBHelper = null;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_page);
		
		mPkgDBHelper = new PackageDbHelper(this);
        String [] columns = { PackageDbHelper.COLUMN_ID, 
        		PackageDbHelper.COLUMN_CODE, 
        		PackageDbHelper.COLUMN_VERSION,
        		PackageDbHelper.COLUMN_APP_NAME};
        Cursor c = mPkgDBHelper.select(columns, null, null, null);
        this.startManagingCursor(c);
        
		mListView = (ListView)findViewById(R.id.downloadpage_list);
		mListView.setAdapter(new ListAdapter(this, android.R.layout.simple_list_item_1, c));
	}
	
	private class ListAdapter extends ResourceCursorAdapter {
		private int mIndexColCode = -1;
		private int mIndexColVersion = -1;
        private int mIndexColName = -1;
        
        public ListAdapter(Context context, int layout, Cursor c) {
        	super(context, layout, c);
        	
        	mIndexColCode = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_CODE);
        	mIndexColVersion = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_VERSION);
        	mIndexColName = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_APP_NAME);
        }

        public void bindView(View view, Context context, Cursor cursor) {
        	String str = cursor.getString(mIndexColName);
        	str += "--";
        	str += cursor.getString(mIndexColCode);
        	str += "--";
        	str += cursor.getString(mIndexColVersion);
        	Log.i(TAG, "bindView" + str);
        	
        	TextView tv = (TextView)view;
        	tv.setText(str);
        }
    }
}
