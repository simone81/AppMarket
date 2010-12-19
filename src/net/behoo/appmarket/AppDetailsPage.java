package net.behoo.appmarket;

import java.io.InputStream;

import net.behoo.appmarket.InstallButtonGuard.OnInstallClickListener;
import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import net.behoo.appmarket.http.AppDetailParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.ProtocolDownloadTask;
import net.behoo.appmarket.http.UrlHelpers;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AppDetailsPage extends AsyncTaskActivity implements OnInstallClickListener {
	
	public static final String APP_CODE = "appcode";
	public static final String APP_NAME = "appcode";
	public static final String APP_VERSION = "appcode";
	public static final String APP_AUTHOR = "appcode";
	public static final String APP_DESC = "appcode";
	public static final String APP_IMAGE_URL = "appcode";
	
	private static final String TAG = "DetailsPage";
	
	private DownloadInstallService mInstallService = null;
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
    
	private ServiceConnection mServiceConn = new ServiceConnection() {
    	
    	public void onServiceConnected(ComponentName cname, IBinder binder){
    		mInstallService = ((DownloadInstallService.LocalServiceBinder)binder).getService();
    		mInstallButtonGuard = new InstallButtonGuard(mInstallButton,
    				mAppInfo, mInstallService);
    		mInstallButtonGuard.setOnInstallClickListener(AppDetailsPage.this);
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
        setContentView(R.layout.details_page); 
        
        // get the summary application information
        String [] value = getIntent().getStringArrayExtra("net.behoo.appmarket.AppDetailsPage");
        mAppInfo = new AppInfo(value[0], value[1], value[2], value[3], value[4], value[5]);
        
        // tbd should disable first, because we can't get the app state now
        mInstallButton = (Button)findViewById(R.id.detail_btn_install);
        
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
    	
    	bindService( new Intent( this, DownloadInstallService.class ), mServiceConn, Context.BIND_AUTO_CREATE );
    	registerReceiver( mReceiver, new IntentFilter( Constants.ACTION_DWONLOAD_INSTALL_STATE ) );
    }
    
    public void onPause() {
    	super.onPause();
    	
    	unbindService( mServiceConn );
    	unregisterReceiver( mReceiver );
    }
	
	protected void onTaskCompleted(boolean result) {
		if (result) {
			updateUIState();
			
			executeImageTask(mAppInfo);
		}
	}
	
	protected void onImageCompleted(boolean result, String appcode) {
		if (result) {
			ImageView iv = (ImageView)findViewById(R.id.main_app_logo);
			iv.setImageDrawable(mAppInfo.getDrawable());
		}
	}
	
	private void updateUIState() {
		TextView tv = (TextView)findViewById(R.id.main_app_title);
		tv.setText(mAppInfo.mAppName);
		
		tv = (TextView)findViewById(R.id.main_app_author);
		tv.setText(mAppInfo.mAppAuthor);
		
		tv = (TextView)findViewById(R.id.main_app_version);
		tv.setText(mAppInfo.mAppVersion);
		
		ImageView iv = (ImageView)findViewById(R.id.main_app_logo);
		if (null != mAppInfo.getDrawable()) {
			iv.setImageDrawable(mAppInfo.getDrawable());
		}
		
		iv = (ImageView)findViewById(R.id.detail_screenshort_1);
		iv.setImageResource(R.drawable.test);
		
		iv = (ImageView)findViewById(R.id.detail_screenshort_2);
		iv.setImageResource(R.drawable.test);
		
		tv = (TextView)findViewById(R.id.detail_review_desc);
		tv.setText(mAppInfo.mAppReview);
		
		tv = (TextView)findViewById(R.id.detail_rc_desc);
		Integer score = Integer.parseInt(mAppInfo.mAppRemoteCntlScore);
		tv.setText(mRemoteCntl[score%4]);
	}
	
	private class HttpTask extends ProtocolDownloadTask {
		
		public HttpTask(Handler handler) {
			super(handler);
		}
		
		protected boolean doTask() {
			try {
				HttpUtil httpUtil = new HttpUtil();
				InputStream stream = httpUtil.httpGet(
						UrlHelpers.getAppDetailUrl("token", mAppInfo.mAppCode));
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
	    	}
		}
    }
}
