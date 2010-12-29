package net.behoo.appmarket;

import java.io.InputStream;

import net.behoo.appmarket.InstallButtonGuard.OnInstallClickListener;
import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.http.AppDetailParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.ProtocolDownloadTask;
import net.behoo.appmarket.http.UrlHelpers;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AppDetailsPage extends AsyncTaskActivity implements OnInstallClickListener {
	
	public static final String EXTRA_KAY = "net.behoo.appmarket.AppDetailsPage";
	
	private static final String TAG = "DetailsPage";

	private Button mInstallButton = null;
	private InstallButtonGuard mInstallButtonGuard = null;

	private AppInfo mAppInfo = new AppInfo();
	private HttpTask mHttpTask = null;
	
	private Integer[] mRemoteCntl = {
		R.string.appdetails_rc_desc1,
		R.string.appdetails_rc_desc2,
		R.string.appdetails_rc_desc3,
		R.string.appdetails_rc_desc4
	};
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (null != mInstallButtonGuard) {
				mInstallButtonGuard.updateAppState();
			}
		}
	};
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details_page); 
        
        // get the summary application information
        String [] value = getIntent().getStringArrayExtra("net.behoo.appmarket.AppDetailsPage");
        mAppInfo = new AppInfo(value[0], value[1], value[2], value[3], value[4], value[5]);
        
        mInstallButton = (Button)findViewById(R.id.detail_btn_install);
        mInstallButtonGuard = new InstallButtonGuard(mInstallButton, mAppInfo,
        		ServiceManager.inst().getDownloadHandler());
        mInstallButtonGuard.setOnInstallClickListener(this);
        
        mHttpTask = new HttpTask(mHandler);
        executeTask(mHttpTask);
        showDialog(WAITING_DIALOG);
    }
    
    public void onInstallClicked(AppInfo appInfo) {
		// TODO Auto-generated method stub
		Intent intent = new Intent();
		intent.setClass(this, AppDownloadPage.class);
		startActivity(intent);
	};
    
    public void onResume() {
    	super.onResume();
    	registerReceiver(mReceiver, new IntentFilter(Constants.ACTION_DWONLOAD_INSTALL_STATE));
    	mInstallButtonGuard.updateAppState();
    }
    
    public void onPause() {
    	super.onPause();
    	unregisterReceiver(mReceiver);
    }
	
	protected void onTaskCompleted(boolean result) {
		if (result) {
			updateUIState();
			executeImageTask(mAppInfo.mAppImageUrl, mAppInfo.mAppCode);
		}
	}
	
	protected void onImageCompleted(boolean result, String url, String appcode) {
		if (result) {
			if (null != ImageLib.inst().getDrawable(url)) {
				ImageView iv = (ImageView)findViewById(R.id.main_app_logo);
				iv.setImageDrawable(ImageLib.inst().getDrawable(url));
			}
		}
	}
	
	private void updateUIState() {
		TextView tv = (TextView)findViewById(R.id.main_app_title);
		tv.setText(mAppInfo.mAppName);
		
		tv = (TextView)findViewById(R.id.main_app_author);
		tv.setText(mAppInfo.mAppAuthor);
		
		tv = (TextView)findViewById(R.id.main_app_version);
		tv.setText(mAppInfo.mAppVersion);
		
		ImageView iv = null;
		if (null != ImageLib.inst().getDrawable(mAppInfo.mAppImageUrl)) {
			iv = (ImageView)findViewById(R.id.main_app_logo);
			iv.setImageDrawable(ImageLib.inst().getDrawable(mAppInfo.mAppImageUrl));
		}
		
		iv = (ImageView)findViewById(R.id.detail_screenshort_1);
		iv.setImageResource(R.drawable.test);
		
		iv = (ImageView)findViewById(R.id.detail_screenshort_2);
		iv.setImageResource(R.drawable.test);
		
		tv = (TextView)findViewById(R.id.detail_review_desc);
		tv.setText(mAppInfo.mAppReview);
		
		tv = (TextView)findViewById(R.id.detail_rc_desc);
		int score = 0;
		try {
		    score = Integer.parseInt(mAppInfo.mAppRemoteCntlScore);
		    tv.setText(mRemoteCntl[score%4]);
		} catch(NumberFormatException nfe) {
		   Log.w(TAG, "Could not parse " + nfe);
		} 	
	}
	
	private class HttpTask extends ProtocolDownloadTask {
		
		public HttpTask(Handler handler) {
			super(handler);
		}
		
		protected boolean doTask() {
			HttpUtil httpUtil = new HttpUtil();
			try {
				String url = UrlHelpers.getAppDetailUrl(
						ServiceManager.inst().getSyncHandler().getToken(), mAppInfo.mAppCode);
				Log.i(TAG, "doTask "+url);
				
				InputStream stream = httpUtil.httpGet(url);
				AppInfo appInfo = AppDetailParser.parse(stream);
				mAppInfo.mAppChangelog = appInfo.mAppChangelog;
				mAppInfo.mAppDesc = appInfo.mAppDesc;
				mAppInfo.mAppRemoteCntlScore = appInfo.mAppRemoteCntlScore;
				mAppInfo.mAppReview = appInfo.mAppReview;
				mAppInfo.mAppSize = appInfo.mAppSize;
				mAppInfo.mAppScreenShorts = appInfo.mAppScreenShorts;
				return true;
	    	} catch (Throwable tr) {
	    		return false;
	    	} finally {
	    		httpUtil.disconnect();
	    	}
		}
    }
}
