package net.behoo.appmarket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.database.PackageDbHelper;
import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.provider.Downloads;

public class AppDownloadPage extends AsyncTaskActivity implements OnItemSelectedListener {
	private static final String TAG = "AppDownloadPage";
	
	private ListView mListView = null;
	private PackageDbHelper mPkgDBHelper = null;
	private InstallButtonGuard mInstallButtonGuard = null;
	private Button mInstallButton = null;
	private ImageView mAppImage = null;

	private Map<String, AppInfo> mAppMap = new HashMap<String, AppInfo>();
	private DownloadInstallService mInstallService = null;
	private Cursor mDownloadCursor = null;
	
	private ServiceConnection mServiceConn = new ServiceConnection() {
    	
    	public void onServiceConnected(ComponentName cname, IBinder binder){
    		mInstallService = ((DownloadInstallService.LocalServiceBinder)binder).getService();
    		
    		initList();
    		createInstallButtonGuard();
    	}
    	
    	public void onServiceDisconnected(ComponentName cname){
    		mInstallService = null;
    	}
    };
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceive");
			if (null != mInstallButtonGuard) {
				mInstallButtonGuard.updateAppState();
			}
		}
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_page);
		
		mDownloadCursor = managedQuery(Downloads.CONTENT_URI, 
                new String [] {"_id", Downloads.COLUMN_TITLE, Downloads.COLUMN_STATUS,
                Downloads.COLUMN_TOTAL_BYTES, Downloads.COLUMN_CURRENT_BYTES, 
                Downloads._DATA, Downloads.COLUMN_DESCRIPTION}, 
                null, null);
		
		mInstallButton = (Button)findViewById(R.id.downloadpage_btn_to_install);
		mAppImage = (ImageView)findViewById(R.id.main_app_logo);
	}
	
	public void onResume() {
    	super.onResume();
    	bindService(new Intent(this, DownloadInstallService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    	registerReceiver(mReceiver, new IntentFilter(Constants.ACTION_DWONLOAD_INSTALL_STATE));
    }
    
    public void onPause() {
    	super.onPause();
    	unbindService( mServiceConn );
    	unregisterReceiver( mReceiver );
    }
    
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		// get the app code of the selected item
		createInstallButtonGuard();
		updateUIState();
	}
	
	public void onNothingSelected(AdapterView<?> view) {
	}
	
	private void updateUIState() {
		int pos = mListView.getSelectedItemPosition();
		if (ListView.INVALID_POSITION != pos) {
			String code = (String)mListView.getItemAtPosition(pos);
			if (null != code) {
				AppInfo appInfo = mAppMap.get(code);
				
				TextView tv = (TextView)findViewById(R.id.main_app_title);
				tv.setText(appInfo.mAppName);
				
				tv = (TextView)findViewById(R.id.main_app_author);
				tv.setText(appInfo.mAppAuthor);
				
				tv = (TextView)findViewById(R.id.main_app_version);
				tv.setText(appInfo.mAppVersion);
				
				tv = (TextView)findViewById(R.id.downloadpage_desc);
				tv.setText(appInfo.mAppShortDesc);
				
				updateImage(appInfo);
			}
		}
	}
	
	protected void onImageCompleted(boolean result, String url, String appcode) {
		if (result) {
			updateImage(mAppMap.get(appcode));
        } 
	}
	
	private void updateImage(AppInfo appInfo) {
		if (null == ImageLib.inst().getDrawable(appInfo.mAppImageUrl)) {
			if (false == ImageLib.inst().isImageDownloading(appInfo.mAppImageUrl)) {
				executeImageTask(appInfo.mAppImageUrl, appInfo.mAppCode);
			}
			else {
				mAppImage.setImageResource(R.drawable.test);
			}
		}
		else {
			mAppImage.setImageDrawable(ImageLib.inst().getDrawable(appInfo.mAppImageUrl));
		}
	}
	
	private void initList() {
		mListView = (ListView)findViewById(R.id.downloadpage_list);
		mListView.setAdapter(new ListAdapter(this, android.R.layout.simple_list_item_1, mDownloadCursor));
		mListView.setOnItemSelectedListener(this);
		mListView.requestFocus();
	}
	
	private void createInstallButtonGuard() {
		int pos = mListView.getSelectedItemPosition();
		if (ListView.INVALID_POSITION != pos) {
			String code = (String)mListView.getItemAtPosition(pos);
			if (null != code) {
				if (null == mInstallButtonGuard) {
					mInstallButtonGuard = new InstallButtonGuard(mInstallButton, 
							mAppMap.get(code), mInstallService);
				}
				else {
					mInstallButtonGuard.setAppInfo(mAppMap.get(code));
				}
			}
		}		
	}
	
	private void addAppInfo(String code) {
		if (!mAppMap.containsKey(code)) {
    		String where = PackageDbHelper.COLUMN_CODE + "=?";
    		String [] whereArgs = {code};
    		ArrayList<AppInfo> appList = mInstallService.getAppList(where, whereArgs);
    		Assert.assertTrue(null != appList && appList.size() == 1);
    		mAppMap.put(code, appList.get(0));
    	}
	}
	
	private class ListAdapter extends ResourceCursorAdapter {   
		private int mRowId = -1;
		private int mStatusId = -1;
        private int mTotalBytesId = -1;
        private int mCurBytesId = -1;
        private int mDataId = -1;
        private int mDescId = -1;
        private Cursor mCursor = null;
        
        public ListAdapter(Context context, int layout, Cursor c) {
        	super(context, layout, c);
             
        	mRowId = c.getColumnIndexOrThrow("_id");
        	mDescId = c.getColumnIndexOrThrow(Downloads.COLUMN_DESCRIPTION);
            mTotalBytesId = c.getColumnIndexOrThrow(Downloads.COLUMN_TOTAL_BYTES);
            mCurBytesId = c.getColumnIndexOrThrow(Downloads.COLUMN_CURRENT_BYTES);
        	
            mCursor = c;
        }
        
        public Object getItem(int position) {
        	int iIndex = mCursor.getPosition();
        	if (mCursor.moveToPosition(position)) {
        		String code = mCursor.getString(mDescId);
        		// can't go back to -1 :(
        		if (-1 != iIndex) {
        			mCursor.moveToPosition(iIndex);
        		}
        		AppDownloadPage.this.addAppInfo(code);
        		return code;
        	}
        	return null;
        }
        
        public void bindView(View view, Context context, Cursor cursor) {
        	String str = cursor.getString(mDescId);
        	str += "--";
        	str += cursor.getString(mCurBytesId);
        	str += "--";
        	str += cursor.getString(mTotalBytesId);
        	
        	TextView tv = (TextView)view;
        	tv.setText(str);
        	
        	AppDownloadPage.this.addAppInfo(cursor.getString(mDescId));
        }
    }
}
