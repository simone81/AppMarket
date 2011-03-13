package net.behoo.appmarket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import behoo.providers.BehooProvider;
import behoo.providers.InstalledAppDb;
import behoo.providers.InstalledAppDb.PackageState;
import behoo.sync.ISyncService;
import net.behoo.appmarket.database.PkgsProviderWrapper;

import junit.framework.Assert;

import net.behoo.appmarket.InstallButtonGuard.OnInstallClickListener;
import net.behoo.appmarket.data.AppInfo;
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

public class AppDownloadPage extends AsyncTaskActivity 
							 implements OnItemSelectedListener, OnInstallClickListener {
	private static final String TAG = "AppDownloadPage";
	
	private ListAdapter mListApater = null;
	private ListView mListView = null;
	private InstallButtonGuard mInstallButtonGuard = null;
	private Button mInstallButton = null;
	private ImageView mAppImage = null;
	private ProgressBar mDownloadProgressBar = null;
	private TextView mTextViewSize = null;
	
	private Map<String, AppInfo> mCodeAppInfoMap = new HashMap<String, AppInfo>();
	private Map<String, String> mCodeSizeMap = new HashMap<String, String>();
	private Cursor mDownloadCursor = null;
	
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {			
			String code = intent.getStringExtra(Constants.PACKAGE_CODE);
			String state = intent.getStringExtra(Constants.PACKAGE_STATE);
			// update the progress bar
			switch (PackageState.valueOf(state)) {
			case installing:
			case install_failed:
			case install_succeeded:
			case need_update:
			case download_failed:
				mListApater.notifyDataSetChanged();
				break;
			default:
				break;
			}
		}
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_download_page);
		
		String order = "_id"+" DESC";
		mDownloadCursor = managedQuery(Downloads.CONTENT_URI, 
                new String [] {"_id", Downloads.COLUMN_TITLE, Downloads.COLUMN_STATUS,
                Downloads.COLUMN_TOTAL_BYTES, Downloads.COLUMN_CURRENT_BYTES, 
                Downloads._DATA, Downloads.COLUMN_DESCRIPTION}, 
                null, null, order);
		
		Button button = (Button)findViewById(R.id.downloadpage_btn_to_install);
		mInstallButtonGuard = new InstallButtonGuard(this, button, null);
		mInstallButtonGuard.setOnInstallClickListener(this);
		
		mAppImage = (ImageView)findViewById(R.id.main_app_logo);
		mDownloadProgressBar = (ProgressBar)findViewById(R.id.downloadpage_progress);
		
		mDownloadProgressBar.setVisibility(View.GONE);
		mTextViewSize = (TextView)findViewById(R.id.main_app_size);
		
		mListView = (ListView)findViewById(R.id.downloadpage_list);
		mListApater = new ListAdapter(this, R.layout.applist_item_layout, mDownloadCursor);
		mListView.setAdapter(mListApater);
		mListView.setOnItemSelectedListener(this);
		mListView.requestFocus();
	}
    
	public void onResume() {
		super.onResume();
		mInstallButtonGuard.enableGuard();
		IntentFilter filter = new IntentFilter(Constants.ACTION_PKG_STATE_CHANGED);
		registerReceiver(mReceiver, filter);
		
		String code = (String)mListView.getSelectedItem();
		updateButtonAndUIs(code);
	}
	
    public void onPause() {
    	super.onPause();
    	unregisterReceiver(mReceiver);
    	mInstallButtonGuard.disableGuard();
    }
    
    public void onInstallClicked(AppInfo appInfo) {
		// TODO Auto-generated method stub
		
	}
    
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		// get the app code of the selected item
		String code = (String)mListView.getItemAtPosition(position);
		updateButtonAndUIs(code);
	}
	
	public void onNothingSelected(AdapterView<?> view) {
	}
	
	private void updateButtonAndUIs(String code) {
		if (null != code) {
			AppInfo appInfo = mCodeAppInfoMap.get(code);
			if (null != appInfo) {
				mInstallButtonGuard.setAppInfo(appInfo);
				updateUIState(appInfo);
			}
			else {
				mInstallButtonGuard.setAppInfo(null);
				updateUIState(null);
			}
		}
		else {
			mInstallButtonGuard.setAppInfo(null);
			updateUIState(null);
		}
	}
	
	private void updateUIState(AppInfo appInfo) {
		TextView tvTitle = (TextView)findViewById(R.id.main_app_title);
		TextView tvAuthor = (TextView)findViewById(R.id.main_app_author);
		TextView tvVersion = (TextView)findViewById(R.id.main_app_version);
		TextView tvDesc = (TextView)findViewById(R.id.downloadpage_desc);
		if (null != appInfo) {
			tvTitle.setText(appInfo.mAppName);
			tvAuthor.setText(appInfo.mAppAuthor);
			tvVersion.setText(appInfo.mAppVersion);
			if (mCodeSizeMap.containsKey(appInfo.mAppCode)) {
				mTextViewSize.setVisibility(View.VISIBLE);
				mTextViewSize.setText(mCodeSizeMap.get(appInfo.mAppCode));
			}
			else {
				mTextViewSize.setVisibility(View.GONE);
			}
			tvDesc.setText(appInfo.mAppShortDesc);
		}
		else {
			tvTitle.setText("");
			tvAuthor.setText("");
			tvVersion.setText("");
			mTextViewSize.setVisibility(View.GONE);
			tvDesc.setText("");
		}
		updateImage(appInfo);
	}
	
	protected void onImageCompleted(boolean result, String url, String appcode) {
		if (result) {
			updateImage(mCodeAppInfoMap.get(appcode));
        } 
	}
	
	private void updateImage(AppInfo appInfo) {
		if (null != appInfo) {
			mAppImage.setVisibility(View.VISIBLE);
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
		else {
			mAppImage.setVisibility(View.INVISIBLE);
		}
	}
	
	private void updateAppStatesUIs(View view, String code,
			boolean completed, long currentBytes, long totalBytes) {

		boolean bIsSelected = false;
		int pos = mListView.getSelectedItemPosition();
		if (ListView.INVALID_POSITION != pos) {
			String selCode = (String)mListView.getItemAtPosition(pos);
			bIsSelected = (0 == selCode.compareTo(code));
			if ((completed || totalBytes > 0) && !mCodeSizeMap.containsKey(code)) {
				mCodeSizeMap.put(code, Formatter.formatFileSize(this, totalBytes));
			}
		}
		else {
			mDownloadProgressBar.setVisibility(View.GONE);
			mTextViewSize.setVisibility(View.GONE);
		}
		
		AppInfo appInfo = mCodeAppInfoMap.get(code);
		if (null != appInfo) {
			TextView titleView = (TextView)view.findViewById(R.id.applist_item_title);
			titleView.setText(appInfo.mAppName);
			
			TextView subTitleView = (TextView)view.findViewById(R.id.applist_item_subtitle);
			// update the item view of the list view
			if (completed) {
				// display string according to the state of the local database
				InstalledAppDb.PackageState state = PkgsProviderWrapper.getAppState(this, code);
				subTitleView.setText(getStateStringId(state));
				
				if (bIsSelected) {
					mDownloadProgressBar.setVisibility(View.GONE);
					mTextViewSize.setVisibility(View.VISIBLE);
					mTextViewSize.setText(Formatter.formatFileSize(this, totalBytes));
				}
			}
			else if (0 < totalBytes) {
				// 10% for installation
				long progress = currentBytes*90/totalBytes;
				StringBuilder sb = new StringBuilder();
				sb.append(progress);
				sb.append('%');
				subTitleView.setText(sb.toString());
	        	
	        	if (bIsSelected) {
	        		mDownloadProgressBar.setVisibility(View.VISIBLE);
	        		mDownloadProgressBar.setIndeterminate(false);
	        		mDownloadProgressBar.setProgress((int)progress);
	        		
	        		mTextViewSize.setVisibility(View.VISIBLE);
	        		mTextViewSize.setText(Formatter.formatFileSize(this, totalBytes));
	        	}
			}
			else {
				Log.w(TAG, "bindView: the apk's length is not known!");
				subTitleView.setText(R.string.downloadpage_beginning);
				if (bIsSelected) {
	        		mDownloadProgressBar.setVisibility(View.VISIBLE);
	        		mDownloadProgressBar.setIndeterminate(true);
	        		
	        		mTextViewSize.setVisibility(View.GONE);
	        	}
			}
		}
	}
	
	private int getStateStringId(InstalledAppDb.PackageState state) {
		switch (state) {
		case installing:
			return R.string.downloadpage_installing;
		case install_failed:
			return R.string.downloadpage_install_failure;
		case install_succeeded:
			return R.string.downloadpage_install_success;
		case need_update:
			return R.string.downloadpage_update;
		case download_failed:
			return R.string.downloadpage_download_failure;
		case download_succeeded:
		case downloading:
			return R.string.downloadpage_download_success;
		default:
			Log.i(TAG, "getStateStringId "+state.name());
			return R.string.downloadpage_install_failure;
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
        	if (!mCodeAppInfoMap.containsKey(code)) {
        		AppInfo appInfo = PkgsProviderWrapper.getAppInfo(AppDownloadPage.this, code);
        		if (null != appInfo) {
        			mCodeAppInfoMap.put(code, appInfo);
        		}
        	}
        	
    		int status = cursor.getInt(mStatusId);
    		long totalBytes = cursor.getLong(mTotalBytesId);
    		long currentBytes = cursor.getLong(mCurBytesId);
    		if (Downloads.isStatusCompleted(status)) {
    			// hide the progress bar, update the 
    			updateAppStatesUIs(view, code, true, totalBytes, totalBytes);
    		}
    		else { 
    			updateAppStatesUIs(view, code, false, currentBytes, totalBytes);		
    		}
        }
	}
}
