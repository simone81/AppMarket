package net.behoo.appmarket;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import behoo.sync.ISyncService;

import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.ProtocolDownloadTask;
import net.behoo.appmarket.http.UrlHelpers;
import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.InstallButtonGuard.OnInstallClickListener;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AppMarket extends AsyncTaskActivity 
					   implements OnClickListener, OnFocusChangeListener, OnInstallClickListener {
	
	private static final String TAG = "AppMarket";
	
	private Button mButtonInstall = null;
	private Button mButtonAppList = null;
	private Button mButtonUpdate = null;
	private Button mButtonDownloadMgr = null;
	
	private boolean mFirstRun = true;
	private HttpTask mHttpTask = null;
	private ArrayList<AppInfo> mAppLib = new ArrayList<AppInfo>();
	private Map<String, Integer> mCodeIndexMap = new HashMap<String, Integer>();
	private Integer mCurrentSelection = -1;
	private InstallButtonGuard mInstallButtonGuard = null;
	
	private ServiceConnection mSyncServiceConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			ServiceManager.inst().setSyncHandler(ISyncService.Stub.asInterface(service));
			
			try {
				String token = ServiceManager.inst().getSyncHandler().getToken();
				if (null != token && token.length() > 0 && mFirstRun) {
		            executeTask(mHttpTask);
		            showDialog(WAITING_DIALOG);
		    		mFirstRun = false;
				}
				else {
					Log.i(TAG, token==null ? "null token" : token);
				}
				checkUpdate();
	        } catch (RemoteException e) {
	    		Log.w(TAG, "getToken "+e.getLocalizedMessage());
	    	}
		}

		public void onServiceDisconnected(ComponentName name) {
			ServiceManager.inst().setSyncHandler(null);
		}
	};
	
	private ServiceConnection mDownloadServiceConn = new ServiceConnection() {
    	
    	public void onServiceConnected(ComponentName cname, IBinder binder){
    		ServiceManager.inst().setDownloadHandler(
    				((DownloadInstallService.LocalServiceBinder)binder).getService());
    		
    		checkUpdate();
    		updateInstallButtonGuard();
    	}
    	
    	public void onServiceDisconnected(ComponentName cname){
    		mInstallButtonGuard = null;
    		ServiceManager.inst().setDownloadHandler(null);
    	}
    };
    
    private BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (null != mInstallButtonGuard) {
				mInstallButtonGuard.updateAppState();
			}
		}
	};
    
	private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			// do something
		}
	};
	
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_market);
        
        TextView tv = (TextView)findViewById(R.id.market_subtitle);
        tv.setText(R.string.market_promotion);
        
        mButtonInstall = ( Button )findViewById(R.id.main_btn_install);
        
        mButtonUpdate = ( Button )findViewById(R.id.main_btn_download_page);
        mButtonUpdate.setOnClickListener(this);
        
        mButtonDownloadMgr = ( Button )findViewById(R.id.main_btn_update_page);
        mButtonDownloadMgr.setOnClickListener(this);
        
        mButtonAppList = ( Button )findViewById(R.id.main_btn_applist_page);
        mButtonAppList.setOnClickListener(this);
        
        startService(new Intent(this, DownloadInstallService.class));
        
        mHttpTask = new HttpTask(mHandler);
    }
    
    public void onResume() {
    	super.onResume();
    	bindService(new Intent(behoo.content.Intent.ACTION_SYNCSERVICE), mSyncServiceConn, BIND_AUTO_CREATE);
    	bindService(new Intent(this, DownloadInstallService.class), mDownloadServiceConn, Context.BIND_AUTO_CREATE);
    	registerReceiver(mDownloadReceiver, new IntentFilter(Constants.ACTION_DWONLOAD_INSTALL_STATE));
    	registerReceiver(mUpdateReceiver, new IntentFilter(Constants.ACTION_UPDATE_STATE));
    	updateInstallButtonGuard();
    }
    
    public void onPause() {
    	super.onPause();
    	unregisterReceiver(mUpdateReceiver);
    	unregisterReceiver(mDownloadReceiver);
    	unbindService(mDownloadServiceConn);
    	unbindService(mSyncServiceConn);
    }
    
    public void onInstallClicked(AppInfo appInfo) {
		// TODO Auto-generated method stub
		Intent intent = new Intent();
		intent.setClass(this, AppDownloadPage.class);
		startActivity(intent);
	};
	
	public void onClick(View v) {
		if (v.getId() == R.id.main_btn_applist_page ) {
			Intent intent = new Intent();
			intent.setClass(this, AppListPage.class);
			startActivity(intent);
		}
		else if (v.getId() == R.id.main_btn_update_page) {
			Intent intent = new Intent();
			intent.setClass(this, AppUpdatePage.class);
			startActivity(intent);
		}
		else if (v.getId() == R.id.main_btn_download_page) {
			Intent intent = new Intent();
			intent.setClass(this, AppDownloadPage.class);
			startActivity(intent);
		}
		else {
			for (int i = 0; i < mImageViewIds.length; ++i) {
				if (v.getId() == mImageViewIds[i]) {
					// go to details page
					Intent intent = new Intent();
					intent.setClass(this, AppDetailsPage.class);
					String [] value = {
						mAppLib.get(i).mAppName,
						mAppLib.get(i).mAppVersion,
						mAppLib.get(i).mAppCode,
						mAppLib.get(i).mAppAuthor,
						mAppLib.get(i).mAppImageUrl,
						mAppLib.get(i).mAppDesc,
					};
					intent.putExtra(AppDetailsPage.EXTRA_KAY, value);
					startActivity(intent);
					break;
				}
			}
		}
    }
	
	public void onFocusChange(View v, boolean hasFocus) {
		Integer index = (Integer)v.getTag();
		if (0 <= index && index < mImageViewIds.length) {
			// redraw
			if (hasFocus) {
				mCurrentSelection = index;
				updateUIState();
				// the default picture
				v.setBackgroundResource(R.drawable.focus);
				v.setOnClickListener(this);
				mButtonInstall.setNextFocusDownId(v.getId());
				mButtonAppList.setNextFocusUpId(v.getId());
				mButtonUpdate.setNextFocusUpId(v.getId());
				mButtonDownloadMgr.setNextFocusUpId(v.getId());
				
				updateInstallButtonGuard();
			}
			else {
				v.setBackgroundResource(0);
			}
		}
	}
	
	protected void onTaskCompleted(boolean result) {
		if (mAppLib.size() > 0) {
			mCurrentSelection = 0;
		}
		
		for (int i = 0; i < mImageViewIds.length; ++i) {
			ImageView iv = (ImageView)findViewById(mImageViewIds[i]);
			if (i < mAppLib.size()) {
				mCodeIndexMap.put(mAppLib.get(i).mAppCode, i);
				iv.setImageResource(R.drawable.test);
				iv.setFocusable(true);
				iv.setOnFocusChangeListener(this);
				iv.setTag(new Integer(i));
				iv.setPadding(5, 5, 5, 5);
				if (i == mCurrentSelection) {
					iv.requestFocus();
					
					updateInstallButtonGuard();
				}
				
				if (null != mAppLib.get(i).mAppImageUrl) {
					executeImageTask(mAppLib.get(i));
				}
			}
			else {
				iv.setVisibility(View.INVISIBLE);
			}
		}
	}
	
	protected void onImageCompleted(boolean result, String appcode) {
		if (result) {
			Integer i = mCodeIndexMap.get(appcode);
			Log.i(TAG, "onImageCompleted "+appcode+" "+Integer.toString(i)+" "+mCurrentSelection.toString());
			if (0 <= i && i < mAppLib.size()) {
				ImageView iv = (ImageView)findViewById(mImageViewIds[i]);
				iv.setImageDrawable(mAppLib.get(i).getDrawable());
			}
			
			if (0 == i.compareTo(mCurrentSelection)) {
				updateImage(i);
			}
		}
	}
	
	private void updateInstallButtonGuard() {
		if (null == mInstallButtonGuard) {
			if (-1 != mCurrentSelection 
					&& null != ServiceManager.inst().getDownloadHandler() 
					&& mAppLib.size() > 0) {
				mInstallButtonGuard = new InstallButtonGuard(mButtonInstall, 
						mAppLib.get(mCurrentSelection), ServiceManager.inst().getDownloadHandler());
				mInstallButtonGuard.setOnInstallClickListener(AppMarket.this);
			}
		}
		else {
			AppInfo oldAppInfo = mInstallButtonGuard.getAppInfo();
			if (-1 != mCurrentSelection) {
				AppInfo newAppInfo = mAppLib.get(mCurrentSelection);
				if (null != oldAppInfo
						&& 0 == oldAppInfo.mAppCode.compareTo(newAppInfo.mAppCode)) {
					mInstallButtonGuard.updateAppState();
				}
				else {
					mInstallButtonGuard.setAppInfo(mAppLib.get(mCurrentSelection));
				}
			}
		}
	}
	
	private void updateUIState() {
		if (mAppLib.size() > 0) {
			assert(mCurrentSelection >= 0 && mCurrentSelection < mAppLib.size());
			
			AppInfo appInfo = mAppLib.get(mCurrentSelection);
			TextView tv = (TextView)findViewById(R.id.main_app_title);
			tv.setText(appInfo.mAppName);
			
			tv = (TextView)findViewById(R.id.main_app_author);
			tv.setText(appInfo.mAppAuthor);
			
			tv = (TextView)findViewById(R.id.main_app_version);
			tv.setText(appInfo.mAppVersion);
			
			tv = (TextView)findViewById(R.id.main_app_desc);
			tv.setText(appInfo.mAppShortDesc);
			
			updateImage(mCurrentSelection);
		}
	}
	
	private void updateImage(int index) {
		if (index >= 0 && index < mImageViewIds.length) {
			if (null != mAppLib.get(index).getDrawable()) {
				ImageView iv = (ImageView)findViewById(R.id.main_app_logo);
				iv.setImageDrawable(mAppLib.get(index).getDrawable());
			}
		}
	}
	
	private void checkUpdate() {
		if (null != ServiceManager.inst().getDownloadHandler()
				&& null != ServiceManager.inst().getSyncHandler()) {
			ServiceManager.inst().getDownloadHandler().checkUpdate(
					ServiceManager.inst().getSyncHandler());
		}
	}
	
	private Integer[] mImageViewIds = {
		R.id.main_appimage_1, 	R.id.main_appimage_2, 
		R.id.main_appimage_3, 	R.id.main_appimage_4, 
		R.id.main_appimage_5, 	R.id.main_appimage_6, 
		R.id.main_appimage_7, 	R.id.main_appimage_8, 
	};
	
	private class HttpTask extends ProtocolDownloadTask {
		
		public HttpTask(Handler handler) {
			super(handler);
		}
		
		protected boolean doTask() {
			HttpUtil httpUtil = new HttpUtil();
			try {
	    		String url = UrlHelpers.getPromotionUrl(
	    				ServiceManager.inst().getSyncHandler().getToken());
	    		Log.i(TAG, "doTask "+url);
				InputStream stream = httpUtil.httpGet(url);
				ArrayList<AppInfo> appLib = AppListParser.parse(stream);
				if (null != appLib) {
					mAppLib = appLib;
					return true;
				}
				return false;
	    	} catch (Throwable tr) {
	    		Log.i(TAG, "doTask "+tr.getLocalizedMessage());
	    		return false;
	    	} finally {
	    		httpUtil.disconnect();
	    	}
		}
	}
}