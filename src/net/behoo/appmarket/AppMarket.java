package net.behoo.appmarket;

import java.io.InputStream;
import java.util.ArrayList;

import junit.framework.Assert;

import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.DownloadConstants;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.ImageDownloadTask;
import net.behoo.appmarket.http.PausableThreadPoolExecutor;
import net.behoo.appmarket.http.UrlHelpers;
import net.behoo.appmarket.data.AppInfo;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AppMarket extends AsyncTaskActivity 
					   implements OnClickListener, OnFocusChangeListener {
	
	private static final String TAG = "AppMarket";
	
	private Button mButtonInstall = null;
	private Button mButtonAppList = null;
	private Button mButtonUpdate = null;
	private Button mButtonDownloadMgr = null;
	
	private ArrayList<AppInfo> mAppLib = new ArrayList<AppInfo>();
	private Integer mCurrentSelection = -1;
	
	// apk download and install service
	private boolean mServiceBound = false;
	private DownloadInstallService mInstallService = null;
	private HttpTask mHttpTask = new HttpTask();
	
	private ServiceConnection mServiceConn = new ServiceConnection() {
    	
    	public void onServiceConnected(ComponentName cname, IBinder binder){
    		mInstallService = ((DownloadInstallService.LocalServiceBinder)binder).getService();
    		mServiceBound = true;
    		Log.i(TAG, "onServiceConnected cname: " + cname.toShortString());
    	}
    	
    	public void onServiceDisconnected(ComponentName cname){
    		mInstallService = null;
    		mServiceBound = false;
    		Log.i(TAG, "onServiceDisconnected");
    	}
    };
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceive");
		}
	};
    
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_market);
        
        TextView tv = (TextView)findViewById(R.id.market_subtitle);
        tv.setText(R.string.market_promotion);
        
        mButtonInstall = ( Button )findViewById(R.id.main_btn_install);
        mButtonInstall.setOnClickListener(this);
        
        mButtonUpdate = ( Button )findViewById(R.id.main_btn_download_page);
        mButtonUpdate.setOnClickListener(this);
        
        mButtonDownloadMgr = ( Button )findViewById(R.id.main_btn_update_page);
        mButtonDownloadMgr.setOnClickListener(this);
        
        mButtonAppList = ( Button )findViewById(R.id.main_btn_applist_page);
        mButtonAppList.setOnClickListener(this);
        
        startService(new Intent(this, DownloadInstallService.class));
        executeTask(mHttpTask);
        showDialog(WAITING_DIALOG);
    }
    
    public void onResume() {
    	super.onResume();
    	bindService( new Intent( this, DownloadInstallService.class ), mServiceConn, Context.BIND_AUTO_CREATE );
    	registerReceiver( mReceiver, new IntentFilter( Constants.ACTION_STATE ) );
    }
    
    public void onPause() {
    	super.onPause();
    	unbindService( mServiceConn );
    	unregisterReceiver( mReceiver );
    }
    
	public void onClick(View v) {
		Intent intent = new Intent();
		if (v.getId() == R.id.main_btn_install) {
			if (mServiceBound) {
				mInstallService.install("");
			}
		}
		else if (v.getId() == R.id.main_btn_applist_page ) {
			intent.setClass(this, AppListPage.class);
		}
		else if (v.getId() == R.id.main_btn_update_page) {
			intent.setClass(this, AppUpdatePage.class);
		}
		else if (v.getId() == R.id.main_btn_download_page) {
			intent.setClass(this, AppDownloadPage.class);
		}
		startActivity( intent );
    }
	
	public void onFocusChange(View v, boolean hasFocus) {
		Integer index = -1;
		int min = mImageViewIds[0];
		int max = mImageViewIds[mImageViewIds.length-1];
		if (v.getId() <= max && v.getId() >= min) {
			index = (Integer)v.getTag();
			mCurrentSelection = index;
			// redraw
			if (hasFocus) {
				updateUIState();
				v.setBackgroundResource(R.drawable.focus);
				mButtonInstall.setNextFocusDownId(v.getId());
				mButtonAppList.setNextFocusUpId(v.getId());
				mButtonUpdate.setNextFocusUpId(v.getId());
				mButtonDownloadMgr.setNextFocusUpId(v.getId());
			}
			else {
				v.setBackgroundResource(0);
			}
		}
	}
	
	protected void onTaskCompleted(int result) {
		if (mAppLib.size() > 0) {
			mCurrentSelection = 0;
		}
		
		for (int i = 0; i < mImageViewIds.length; ++i) {
			ImageView iv = (ImageView)findViewById(mImageViewIds[i]);
			if (i < mAppLib.size()) {
				iv.setImageResource(R.drawable.test);
				iv.setFocusable(true);
				iv.setOnFocusChangeListener(this);
				iv.setTag(new Integer(i));
				iv.setPadding(5, 5, 5, 5);
				if (i == mCurrentSelection) {
					iv.requestFocus();
				}
				
				if (null != mAppLib.get(i).mAppImageUrl) {
					executeImageTask(mAppLib.get(i));
				}
			}
			else {
				iv.setVisibility(View.INVISIBLE);
			}
		}
		
		updateUIState();
	}
	
	protected void onImageCompleted(boolean result, String appcode) {
		if (result) {
			for (int i = 0; i < mAppLib.size(); ++i) {
				if (0 == appcode.compareTo(mAppLib.get(i).mAppCode)) {
					updateImage(i);
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
			
			if (appInfo.getDrawable() != null) {
				ImageView iv = (ImageView)findViewById(mImageViewIds[mCurrentSelection]);
				Assert.assertTrue(true);
				iv.setImageDrawable(appInfo.getDrawable());
			}
		}
	}
	
	private void updateImage(int index) {
		if (index >= 0 && index < mImageViewIds.length) {
			ImageView iv = (ImageView)findViewById(mImageViewIds[index]);
			if (null != mAppLib.get(index).getDrawable()) {
				iv.setImageDrawable(mAppLib.get(index).getDrawable());
			}
			else {
				iv.setImageResource(R.drawable.test);
			}
		}
	}
	
	private Integer[] mImageViewIds = {
		R.id.main_appimage_1, 	R.id.main_appimage_2, 
		R.id.main_appimage_3, 	R.id.main_appimage_4, 
		R.id.main_appimage_5, 	R.id.main_appimage_6, 
		R.id.main_appimage_7, 	R.id.main_appimage_8, 
	};
	
	private class HttpTask implements Runnable {
		public boolean mResult = false;
		public void run() {
	    	try {
	    		HttpUtil httpUtil = new HttpUtil();
				InputStream stream = httpUtil.httpGet(UrlHelpers.getPromotionUrl(""));
				mAppLib = AppListParser.parse(stream);
				mResult = true;
	    	} catch (Throwable tr) {
	    		mResult = false;
	    	}
		}
    };
}