package net.behoo.appmarket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

public class AppDownloadPage extends AsyncTaskActivity implements OnItemSelectedListener {
	private static final String TAG = "AppDownloadPage";
	
	private ListView mListView = null;
	private PackageDbHelper mPkgDBHelper = null;
	private InstallButtonGuard mInstallButtonGuard = null;
	private Button mInstallButton = null;
	private ImageView mAppImage = null;
	private Set<String> mImageDownloadFlags = new HashSet<String>();
	private Map<String, AppInfo> mAppMap = new HashMap<String, AppInfo>();
	private DownloadInstallService mInstallService = null;
	
	private ServiceConnection mServiceConn = new ServiceConnection() {
    	
    	public void onServiceConnected(ComponentName cname, IBinder binder){
    		mInstallService = ((DownloadInstallService.LocalServiceBinder)binder).getService();
    		
    		int pos = mListView.getSelectedItemPosition();
    		if (ListView.INVALID_POSITION != pos) {
    			String code = (String)mListView.getItemAtPosition(pos);
    			if (null != code) {
    				mInstallButtonGuard = new InstallButtonGuard(mInstallButton, 
    						mAppMap.get(code), mInstallService);
    			}
    		}
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
		
		mPkgDBHelper = new PackageDbHelper(this);
        String [] columns = { PackageDbHelper.COLUMN_ID, 
        		PackageDbHelper.COLUMN_CODE, 
        		PackageDbHelper.COLUMN_VERSION,
        		PackageDbHelper.COLUMN_APP_NAME,
        		PackageDbHelper.COLUMN_AUTHOR,
        		PackageDbHelper.COLUMN_DESC,
        		PackageDbHelper.COLUMN_IMAGE_URL};
        Cursor c = mPkgDBHelper.select(columns, null, null, null);
        this.startManagingCursor(c);
        
		mListView = (ListView)findViewById(R.id.downloadpage_list);
		mListView.setAdapter(new ListAdapter(this, android.R.layout.simple_list_item_1, c));
		mListView.setOnItemSelectedListener(this);
		mListView.requestFocus();
		
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
				
				tv = (TextView)findViewById(R.id.app_list_desc);
				tv.setText(appInfo.mAppShortDesc);
				
				updateImage(appInfo);
			}
		}
	}
	
	protected void onImageCompleted(boolean result, String appcode) {
		mImageDownloadFlags.add(appcode);
		if (result) {
			updateImage(mAppMap.get(appcode));
        } 
	}
	
	private void updateImage(AppInfo appInfo) {
		if (null == appInfo.getDrawable()) {
			if (false == mImageDownloadFlags.contains(appInfo.mAppCode)) {
				executeImageTask(appInfo);
			}
			else {
				mAppImage.setImageResource(R.drawable.test);
			}
		}
		else {
			mAppImage.setImageDrawable(appInfo.getDrawable());
		}
	}
	
	private class ListAdapter extends ResourceCursorAdapter {
		private int mIndexColCode = -1;
		private int mIndexColVersion = -1;
        private int mIndexColName = -1;
        private int mIndexAuthor = -1;
        private int mIndexDesc = -1;
        private int mIndexImage = -1;
        private Cursor mCursor = null;
        public ListAdapter(Context context, int layout, Cursor c) {
        	super(context, layout, c);
        	
        	mIndexColCode = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_CODE);
        	mIndexColVersion = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_VERSION);
        	mIndexColName = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_APP_NAME);
        	mIndexAuthor = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_AUTHOR);
            mIndexDesc = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_DESC);
            mIndexImage = c.getColumnIndexOrThrow(PackageDbHelper.COLUMN_IMAGE_URL);
            
            mCursor = c;
        }
        
        public Object getItem(int position) {
        	int iIndex = mCursor.getPosition();
        	if (mCursor.moveToPosition(position)) {
        		// can't go back to -1 :(
        		if (-1 != iIndex) {
        			mCursor.moveToPosition(iIndex);
        		}
        		return mCursor.getString(mIndexColCode);
        	}
        	return null;
        }
        
        public void bindView(View view, Context context, Cursor cursor) {
        	String str = cursor.getString(mIndexColName);
        	str += "--";
        	str += cursor.getString(mIndexColCode);
        	str += "--";
        	str += cursor.getString(mIndexColVersion);
        	Log.i(TAG, "bindView " + str);
        	
        	TextView tv = (TextView)view;
        	tv.setText(str);
        	tv.setTag(cursor.getString(mIndexColCode));
        	
        	if (!mAppMap.containsKey(cursor.getString(mIndexColCode))) {
        		AppInfo appInfo = new AppInfo(cursor.getString(mIndexColName),
        				cursor.getString(mIndexColVersion),
        				cursor.getString(mIndexColCode),
        				cursor.getString(mIndexAuthor),
        				cursor.getString(mIndexImage),
        				cursor.getString(mIndexDesc));
        		mAppMap.put(cursor.getString(mIndexColCode), appInfo);
        	}
        }
    }
}
