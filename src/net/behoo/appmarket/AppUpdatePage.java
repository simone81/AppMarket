package net.behoo.appmarket;

import java.util.HashMap;
import java.util.Map;

import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;
import behoo.providers.InstalledAppDb.PackageState;

import net.behoo.appmarket.InstallButtonGuard.OnInstallClickListener;
import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class AppUpdatePage extends AsyncTaskActivity 
						   implements OnItemSelectedListener, 
						   			  OnInstallClickListener {
	
	private Cursor mCursor = null;
	private Map<String, AppInfo> mAppLib = new HashMap<String, AppInfo>();
	
	private ImageView mAppImage = null;
	private ListView mListView = null;
	private ListAdapter mListAdapter = null;
    private InstallButtonGuard mButtonGuard = null;
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			dismissDialog(WAITING_DIALOG);
		}
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_update_page);
		
		String [] columns = {InstalledAppDb.COLUMN_ID, 
    			InstalledAppDb.COLUMN_CODE, 
    			InstalledAppDb.COLUMN_APP_NAME,
    			InstalledAppDb.COLUMN_AUTHOR, 
    			InstalledAppDb.COLUMN_VERSION, 
    			InstalledAppDb.COLUMN_IMAGE_URL,
    			InstalledAppDb.COLUMN_DESC};
        String where = InstalledAppDb.COLUMN_STATE+"=?";
        String [] whereArgs = {PackageState.need_update.name()};
        mCursor = managedQuery(BehooProvider.INSTALLED_APP_CONTENT_URI, 
                columns, where, whereArgs, null);
		mListView = (ListView)findViewById(R.id.app_update_list);
		mListAdapter = new ListAdapter(this, R.layout.applist_item_layout, mCursor);
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemSelectedListener(this);
		
		Button button = (Button)findViewById(R.id.appupdate_btn_update);
		mButtonGuard = new InstallButtonGuard(this, button, null);
		mButtonGuard.setOnInstallClickListener(this);
		
		mAppImage = (ImageView)findViewById(R.id.main_app_logo);
		
		Intent i = new Intent(this, DownloadInstallService.class);
		i.setAction(Constants.ACTION_START_CHECK_UPDATE);
		startService(i);
		showDialog(WAITING_DIALOG);
	}
	
	public void onResume() {
		super.onResume();
		mButtonGuard.enableGuard();
		registerReceiver(mReceiver, new IntentFilter(Constants.ACTION_PKG_UPDATE_FINISHED));
		
		String code = (String)mListView.getSelectedItem();
		updateButtonAndUIs(code);
	}
	
	public void onPause() {
		super.onStop();
		unregisterReceiver(mReceiver);
		mButtonGuard.disableGuard();
	}
	
	public void onInstallClicked(AppInfo appInfo) {
		Intent i = new Intent();
		i.setClass(this, AppDownloadPage.class);
		startActivity(i);
	}
	
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		String code = (String)mListView.getItemAtPosition(position);
		updateButtonAndUIs(code);
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}
	
	protected void onImageCompleted(boolean result, String url, String appcode) {
		if (result) {
			int pos = mListView.getSelectedItemPosition();
			String code = (String)mListView.getItemAtPosition(pos);
			if (null != code && 0 == code.compareTo(appcode)) {
				updateImage(mAppLib.get(code));
			}
		}
	}
	
	private void updateButtonAndUIs(String code) {
		if (null != code) {
			AppInfo appInfo = mAppLib.get(code);
			if (null != appInfo) {
				mButtonGuard.setAppInfo(appInfo);
				updateUIState(appInfo);
			} else {
				mButtonGuard.setAppInfo(null);
			}
		} else {
			mButtonGuard.setAppInfo(null);
		}
	}
	
	private void updateUIState(AppInfo appInfo) {	
		TextView tv = (TextView)findViewById(R.id.main_app_title);
		tv.setText(appInfo.mAppName);
		
		tv = (TextView)findViewById(R.id.main_app_author);
		tv.setText(appInfo.mAppAuthor);
		
		tv = (TextView)findViewById(R.id.main_app_version);
		tv.setText(appInfo.mAppVersion);
		
		tv = (TextView)findViewById(R.id.app_update_desc);
		tv.setText(appInfo.mAppShortDesc);
		
		updateImage(appInfo);
	}
	
	private void updateImage(AppInfo appInfo) {
		if (null == ImageLib.inst().getBitmap(appInfo.mAppImageUrl)) {
			if (false == ImageLib.inst().isImageDownloading(appInfo.mAppImageUrl)) {
				executeImageTask(appInfo.mAppImageUrl, appInfo.mAppCode);
			}
			else {
				mAppImage.setImageResource(R.drawable.appicon_default);
			}
		}
		else {
			mAppImage.setImageBitmap(ImageLib.inst().getBitmap(appInfo.mAppImageUrl));
		}
	}
	
	private class ListAdapter extends ResourceCursorAdapter {
		private Cursor mCursor = null;
		private int mCodeId = -1;
		private int mAppNameId = -1;
		private int mAuthorId = -1;
		private int mVersionId = -1;
		private int mImageUrlId = -1;
		private int mShortDescId = -1;
		
        public ListAdapter(Context context, int layout, Cursor c) {
        	super(context, layout, c);
        	mCursor = c;
        	mCodeId = mCursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_CODE);
    		mAppNameId = mCursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_APP_NAME);
    		mAuthorId = mCursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_AUTHOR);
    		mVersionId = mCursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_VERSION);
    		mImageUrlId = mCursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_IMAGE_URL);
    		mShortDescId = mCursor.getColumnIndexOrThrow(InstalledAppDb.COLUMN_DESC);
        }
        
        public Object getItem(int position) {
        	if (mCursor.moveToPosition(position)) {
        		return mCursor.getString(mCodeId);
        	}
        	return null;
        }
        
        public void bindView(View view, Context context, Cursor cursor) {
        	AppInfo appInfo = null;
        	
        	String code = cursor.getString(mCodeId);
        	if (!mAppLib.containsKey(code)) {
        		appInfo = new AppInfo();
        		appInfo.mAppCode = cursor.getString(mCodeId);
        		appInfo.mAppName = cursor.getString(mAppNameId);
        		appInfo.mAppAuthor = cursor.getString(mAuthorId);
        		appInfo.mAppVersion = cursor.getString(mVersionId);
        		appInfo.mAppImageUrl = cursor.getString(mImageUrlId);
        		appInfo.mAppShortDesc = cursor.getString(mShortDescId);
        		
        		mAppLib.put(code, appInfo);
        	}
        	else {
        		appInfo = mAppLib.get(code);
        	}
        	
        	TextView tv = (TextView)view.findViewById(R.id.applist_item_title);
    		tv.setText(appInfo.mAppName);
        }
	}
}
