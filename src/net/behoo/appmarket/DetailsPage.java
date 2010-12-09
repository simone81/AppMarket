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

public class DetailsPage extends AsyncTaskActivity {
	private static final String TAG = "DetailsPage";
	
	private boolean mServiceBound = false;
	private DownloadInstallService mInstallService = null;
	
	private AppInfo mAppInfo = new AppInfo();
    /** Called when the activity is first created. */
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details_page); 
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
			InputStream stream = httpUtil.httpGet(UrlHelpers.getPromotionUrl(""));
			AppDetailParser.parse(stream, mAppInfo);
			return true;
    	} catch (Throwable tr) {
    		return false;
    	}
    }
	
	protected void onTaskCompleted(int result) {
		
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
