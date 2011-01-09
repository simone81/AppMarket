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
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.provider.Downloads;

public class AppDownloadPage extends AsyncTaskActivity implements OnItemSelectedListener {
	private static final String TAG = "AppDownloadPage";
	
	private ListAdapter mListApater = null;
	private ListView mListView = null;
	private PackageDbHelper mPkgDBHelper = null;
	private InstallButtonGuard mInstallButtonGuard = null;
	private Button mInstallButton = null;
	private ImageView mAppImage = null;
	private ProgressBar mDownloadProgressBar = null;
	
	private Map<String, AppInfo> mCodeAppInfoMap = new HashMap<String, AppInfo>();
	private Cursor mDownloadCursor = null;
	
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceive application state changed");
			if (null != mInstallButtonGuard) {
				mInstallButtonGuard.updateAppState();
			}
			
			Bundle bundle = intent.getExtras();
			String code = bundle.getString(Constants.PACKAGE_CODE);
			String state = bundle.getString(Constants.PACKAGE_STATE);
			// update the progress bar
			switch (Constants.PackageState.valueOf(state)) {
			case installing:
			case install_failed:
			case install_succeeded:
			case need_update:
				mListApater.notifyDataSetChanged();
				break;
			default:
				break;
			}
		}
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_page);
		
		String order = "_id"+" DESC";
		mDownloadCursor = managedQuery(Downloads.CONTENT_URI, 
                new String [] {"_id", Downloads.COLUMN_TITLE, Downloads.COLUMN_STATUS,
                Downloads.COLUMN_TOTAL_BYTES, Downloads.COLUMN_CURRENT_BYTES, 
                Downloads._DATA, Downloads.COLUMN_DESCRIPTION}, 
                null, null, order);
		
		mInstallButton = (Button)findViewById(R.id.downloadpage_btn_to_install);
		mAppImage = (ImageView)findViewById(R.id.main_app_logo);
		mDownloadProgressBar = (ProgressBar)findViewById(R.id.downloadpage_progress);
		
		initList();
		createInstallButtonGuard();
	}
	
	public void onResume() {
    	super.onResume();
    	registerReceiver(mReceiver, new IntentFilter(Constants.ACTION_DWONLOAD_INSTALL_STATE));
    }
    
    public void onPause() {
    	super.onPause();
    	unregisterReceiver(mReceiver);
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
				AppInfo appInfo = mCodeAppInfoMap.get(code);
				
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
			updateImage(mCodeAppInfoMap.get(appcode));
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
		mListApater = new ListAdapter(this, android.R.layout.simple_list_item_1, mDownloadCursor);
		mListView.setAdapter(mListApater);
		mListView.setOnItemSelectedListener(this);
		mListView.requestFocus();
	}
	
	private void createInstallButtonGuard() {
		int pos = mListView.getSelectedItemPosition();
		if (ListView.INVALID_POSITION != pos) {
			String code = (String)mListView.getItemAtPosition(pos);
			if (null == mInstallButtonGuard) {
				mInstallButtonGuard = new InstallButtonGuard(mInstallButton, 
						mCodeAppInfoMap.get(code), ServiceManager.inst().getDownloadHandler());
			}
			else {
				mInstallButtonGuard.setAppInfo(mCodeAppInfoMap.get(code));
			}
		}		
	}
	
	private void addAppInfo(String code) {
		if (!mCodeAppInfoMap.containsKey(code)) {
    		String where = PackageDbHelper.COLUMN_CODE + "=?";
    		String [] whereArgs = {code};
    		ArrayList<AppInfo> appList = ServiceManager.inst().getDownloadHandler().getAppList(where, whereArgs);
    		Assert.assertTrue(null != appList && appList.size() == 1);
    		mCodeAppInfoMap.put(code, appList.get(0));
    	}
	}
	
	private void updateAppStatesUIs(View view, String code,
			boolean completed, long currentBytes, long totalBytes) {

		boolean bIsSelected = false;
		int pos = mListView.getSelectedItemPosition();
		if (ListView.INVALID_POSITION != pos) {
			String selCode = (String)mListView.getItemAtPosition(pos);
			bIsSelected = (0 == selCode.compareTo(code));
		}
		else {
			mDownloadProgressBar.setVisibility(View.INVISIBLE);
		}
		
		AppInfo appInfo = mCodeAppInfoMap.get(code);
		StringBuilder sb = new StringBuilder();
		sb.append(appInfo.mAppName);
		sb.append(' ');
		
		TextView tv = (TextView)view;
		// update the item view of the list view
		if (completed) {
			// display string according to the state of the local database
			Constants.PackageState state = 
				ServiceManager.inst().getDownloadHandler().getAppState(code);
			sb.append(state.name());
			tv.setText(sb.toString());
			
			if (bIsSelected) {
				mDownloadProgressBar.setVisibility(View.INVISIBLE);
			}
		}
		else if (0 < totalBytes) {
			// 10% for installation
			long progress = currentBytes*90/totalBytes;
			sb.append(progress);
			sb.append('%');
        	tv.setText(sb.toString());
        	
        	if (bIsSelected) {
        		mDownloadProgressBar.setVisibility(View.VISIBLE);
        		mDownloadProgressBar.setIndeterminate(false);
        		mDownloadProgressBar.setProgress((int)progress);
        	}
		}
		else {
			Log.w(TAG, "bindView: the apk's length is not known!");
			sb.append(getString(R.string.downloadpage_beginning));
			tv.setText(sb.toString());
			if (bIsSelected) {
        		mDownloadProgressBar.setVisibility(View.VISIBLE);
        		mDownloadProgressBar.setIndeterminate(true);
        	}
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
        	mStatusId = c.getColumnIndexOrThrow(Downloads.COLUMN_STATUS);
        	mDescId = c.getColumnIndexOrThrow(Downloads.COLUMN_DESCRIPTION);
            mTotalBytesId = c.getColumnIndexOrThrow(Downloads.COLUMN_TOTAL_BYTES);
            mCurBytesId = c.getColumnIndexOrThrow(Downloads.COLUMN_CURRENT_BYTES);
        	
            mCursor = c;
        }
        
        public Object getItem(int position) {
        	if (mCursor.moveToPosition(position)) {
        		return mCursor.getString(mDescId);
        	}
        	return null;
        }
        
        public void bindView(View view, Context context, Cursor cursor) {
        	String code = cursor.getString(mDescId);
        	addAppInfo(code);
        	
    		int status = cursor.getInt(mStatusId);
    		if (Downloads.isStatusCompleted(status)) {
    			// hide the progress bar, update the 
    			updateAppStatesUIs(view, code, true, 0, 0);
    		}
    		else {
    			long totalBytes = cursor.getLong(mTotalBytesId);
        		long currentBytes = cursor.getLong(mCurBytesId); 
    			updateAppStatesUIs(view, code, false, currentBytes, totalBytes);		
    		}
        }
    }
}
