package net.behoo.appmarket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.ProtocolDownloadTask;
import net.behoo.appmarket.http.UrlHelpers;
import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.InstallButtonGuard.OnInstallClickListener;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
	
	private static final int APP_PROMOTION_NUM = 6;
	
	private Button mButtonInstall = null;
	private Button mButtonAppList = null;
	private Button mButtonUpdate = null;
	private Button mButtonDownloadMgr = null;
	
	private HttpTask mHttpTask = null;
	private ArrayList<AppInfo> mAppLib = new ArrayList<AppInfo>();
	private Map<String, Integer> mCodeIndexMap = new HashMap<String, Integer>();
	private Integer mCurrentSelection = -1;
	private InstallButtonGuard mInstallButtonGuard = null;
	
	private Integer[] mImageViewIds = {
		R.id.main_appimage_1, 	R.id.main_appimage_2, 
		R.id.main_appimage_3, 	R.id.main_appimage_4, 
		R.id.main_appimage_5, 	R.id.main_appimage_6, 
	};
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_market);
        
        mButtonInstall = (Button)findViewById(R.id.main_btn_install);
        mInstallButtonGuard = new InstallButtonGuard(this, mButtonInstall, null);
        mInstallButtonGuard.setOnInstallClickListener(this);
        
        mButtonUpdate = (Button)findViewById(R.id.main_btn_download_page);
        mButtonUpdate.setOnClickListener(this);
        
        mButtonDownloadMgr = (Button)findViewById(R.id.main_btn_update_page);
        mButtonDownloadMgr.setOnClickListener(this);
        
        mButtonAppList = (Button)findViewById(R.id.main_btn_applist_page);
        mButtonAppList.setOnClickListener(this);
        
        mHttpTask = new HttpTask(mHandler);
        executeTask(mHttpTask);
		showDialog(WAITING_DIALOG);
    }
	
    public void onResume() {
    	super.onResume();
    	mInstallButtonGuard.enableGuard();
    }
    
    public void onPause() {
    	super.onPause();
    	mInstallButtonGuard.disableGuard();
    }
    
    protected void onTaskRetry() {
    	executeTask(mHttpTask);
    	showDialog(WAITING_DIALOG);
    }
    
    protected void onTaskCanceled(DialogInterface dlg) {
    	mHttpTask.setHandler(null);
    	mHttpTask.cancel();
    }
    
    public void onInstallClicked(AppInfo appInfo) {
		Intent intent = new Intent();
		intent.setClass(this, AppDownloadPage.class);
		startActivity(intent);
	};
	
	public void onClick(View v) {
		if (v.getId() == R.id.main_btn_applist_page) {
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
					String appCode = mAppLib.get(i).mAppCode;
					intent.setData(AppInfo.makeUri(appCode));
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
		if (result) {
			if (mAppLib.size() > 0) {
				mCurrentSelection = 0;
			}
			
			for (int i = 0; i < mImageViewIds.length; ++i) {
				ImageView iv = (ImageView)findViewById(mImageViewIds[i]);
				if (i < mAppLib.size()) {
					mCodeIndexMap.put(mAppLib.get(i).mAppCode, i);
					iv.setVisibility(View.VISIBLE);
					iv.setImageResource(R.drawable.appicon_default);
					iv.setFocusable(true);
					iv.setOnFocusChangeListener(this);
					iv.setTag(new Integer(i));
					iv.setPadding(8, 8, 8, 8);
					if (i == mCurrentSelection) {
						iv.requestFocus();
						
						updateInstallButtonGuard();
					}
					
					executeImageTask(mAppLib.get(i).mAppImageUrl, mAppLib.get(i).mAppCode);
				}
				else {
					iv.setVisibility(View.INVISIBLE);
				}
			}
		}
	}
	
	protected void onImageCompleted(boolean result, String url, String appcode) {
		if (result) {
			Integer i = mCodeIndexMap.get(appcode);
			ImageView iv = (ImageView)findViewById(mImageViewIds[i]);
			if (null != ImageLib.inst().getBitmap(url)) {
				iv.setImageBitmap(ImageLib.inst().getBitmap(url));
			}
			
			if (0 == i.compareTo(mCurrentSelection)) {
				updateImage(i);
			}
		}
	}
	
	private void updateInstallButtonGuard() {
		if (-1 != mCurrentSelection) {
			AppInfo appInfo = mAppLib.get(mCurrentSelection);
			mInstallButtonGuard.setAppInfo(appInfo);
		}
		else {
			mInstallButtonGuard.setAppInfo(null);
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
			String url = mAppLib.get(index).mAppImageUrl;
			ImageView iv = (ImageView)findViewById(R.id.main_app_logo);
			if (null != ImageLib.inst().getBitmap(url)) {
				iv.setImageBitmap(ImageLib.inst().getBitmap(url));
			}
			else {
				iv.setImageResource(R.drawable.appicon_default);
			}
		}
	}
	
	private class HttpTask extends ProtocolDownloadTask {
		
		private AppListParser mAppListProxy = new AppListParser();
		
		public HttpTask(Handler handler) {
			super(handler);
		}
		
		public void cancel() {
			mAppListProxy.cancel();
		}
		
		protected boolean doTask() {
			try {
				String token = TokenWrapper.getToken(AppMarket.this);
	    		String url = UrlHelpers.getPromotionUrl(token);
	    		Log.i(TAG, "doTask "+url);
	    		
	    		ArrayList<AppInfo> appLib = mAppListProxy.getPromotionList(url, APP_PROMOTION_NUM);
				mAppLib = appLib;
				return (mAppLib.size() > 0);
			} catch (Throwable tr) {
				tr.printStackTrace();
				return false;
			}
		}
	}
}