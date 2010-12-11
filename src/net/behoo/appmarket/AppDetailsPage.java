package net.behoo.appmarket;

import java.io.InputStream;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import net.behoo.appmarket.http.AppDetailParser;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.UrlHelpers;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class AppDetailsPage extends AsyncTaskActivity {
	
	public static final String APP_CODE = "appcode";
	
	private static final String TAG = "DetailsPage";
	
	private boolean mServiceBound = false;
	private DownloadInstallService mInstallService = null;
	
	private String mAppCode = "";
	private AppInfo mAppInfo = new AppInfo();
	
	private Integer[] mRemoteCntl = {
		R.string.appdetails_rc_desc1,
		R.string.appdetails_rc_desc2,
		R.string.appdetails_rc_desc3,
		R.string.appdetails_rc_desc4
	};
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details_page); 
        
        Bundle bundle = getIntent().getExtras();
        mAppCode = bundle.getString(APP_CODE);
        
        Button button = (Button)findViewById(R.id.detail_btn_install);
        button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent();
				intent.setClass(AppDetailsPage.this, AppDownloadPage.class);
				startActivity(intent);
			}
		});
        
        startTaskAndShowDialog();
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
    
    protected boolean onRunTask() {
    	try {
	    	HttpUtil httpUtil = new HttpUtil();
			InputStream stream = httpUtil.httpGet(UrlHelpers.getAppDetailUrl("", ""));
			mAppInfo = AppDetailParser.parse(stream);
			return true;
    	} catch (Throwable tr) {
    		return false;
    	}
    }
	
	protected void onTaskCompleted(int result) {
		updateUIState();
	}
	
	private void updateUIState() {
		TextView tv = (TextView)findViewById(R.id.main_app_title);
		tv.setText(mAppInfo.mAppName);
		
		tv = (TextView)findViewById(R.id.main_app_author);
		tv.setText(mAppInfo.mAppAuthor);
		
		tv = (TextView)findViewById(R.id.main_app_version);
		tv.setText(mAppInfo.mAppVersion);
		
		ImageView iv = (ImageView)findViewById(R.id.detail_screenshort_1);
		iv.setImageResource(R.drawable.test);
		
		iv = (ImageView)findViewById(R.id.detail_screenshort_2);
		iv.setImageResource(R.drawable.test);
		
		tv = (TextView)findViewById(R.id.detail_review_desc);
		tv.setText(mAppInfo.mAppReview);
		
		tv = (TextView)findViewById(R.id.detail_rc_desc);
		Integer score = Integer.parseInt(mAppInfo.mAppRemoteCntlScore);
		tv.setText(mRemoteCntl[score%4]);
	}
	
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
}
