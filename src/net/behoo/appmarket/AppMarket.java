package net.behoo.appmarket;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.UrlHelpers;
import net.behoo.appmarket.data.AppInfo;

import android.app.Activity;
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
import android.widget.Button;
import android.widget.TextView;

public class AppMarket extends Activity implements OnClickListener {
	
	private static final String TAG = "AppMarket";
	
	private static final int DOWNLOAD_SUCCEED = 0;
	private static final int DOWNLOAD_FAILURE = 1;
	
	private Map<String, AppInfo> mAppLib = new HashMap<String, AppInfo>();
	
	private boolean mServiceBound = false;
	private DownloadInstallService mInstallService = null;
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
        	switch (msg.what) {
        	case DOWNLOAD_SUCCEED:// update all the ui
        		break;
        	case DOWNLOAD_FAILURE:// popup a dialog ? tbd
        		break;
            default:
                break;
        	}
        	super.handleMessage(msg);
        }
	};
	
	private boolean mThreadExit = false;
	private Thread mDownloadThread = new Thread(new Runnable() {
		public void run() {
			int msg = DOWNLOAD_SUCCEED;
			try {
				HttpUtil httpUtil = new HttpUtil();
				InputStream stream = httpUtil.httpGet(UrlHelpers.getPromotionUrl(""));
				AppListParser.parse(stream, mAppLib);
			}
			catch ( Throwable tr ) {
				msg = DOWNLOAD_FAILURE;
			}
			
			synchronized (this) { 
				if(!mThreadExit) {
					mHandler.sendEmptyMessageDelayed(msg, 0);
				}
			}
		}
	});
	
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // download and install
        Button button = ( Button )findViewById( R.id.download_and_install );
        button.setOnClickListener( this );  
        
        button = ( Button )findViewById( R.id.install_from_sdcard );
        button.setOnClickListener( this );
        
        TextView tv = ( TextView )findViewById( R.id.main_input );
        tv.setText( "http://192.168.1.50/ScreenTests.apk" );
        
        tv = ( TextView )findViewById( R.id.main_input_sdcard );
        tv.setText( "file:///nfs/ScreenTests.apk" );
        
        mDownloadThread.start();
        startService( new Intent(this, DownloadInstallService.class) );
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
    
    
    public void onDestroy() {
    	super.onDestroy();
    	
    	synchronized (this) { 
    		mThreadExit = true;
    	}
    	//stopService( new Intent(this, DownloadInstallService.class) );
    }
    
    
	public void onClick(View v) {
    	//this.startActivity( new Intent( this, DetailsPage.class ) );
    	if( v.getId() == R.id.download_and_install ) {
    		// download and install
    		if( mServiceBound ) {
        		TextView ev = ( TextView )findViewById( R.id.main_input );
        		updateStatusUI( "begin downloading!" );
        		mInstallService.downloadAndInstall(ev.getText().toString(), 
        				"application/vnd.android.package-archive", null, -1);
        	}
        	else {
        		updateStatusUI( "service is not bound!" );
        	}
    	}
    	else {
    		// install from sdcard
    		if( mServiceBound ) {
        		TextView ev = ( TextView )findViewById( R.id.main_input_sdcard );
        		updateStatusUI( "begin installing!" );
        		mInstallService.install( ev.getText().toString() );
        	}
        	else {
        		updateStatusUI( "service is not bound!" );
        	}
    	}
    }
    
    private void updateStatusUI( String status ) {
    	TextView tv = ( TextView )findViewById( R.id.main_state );
    	tv.setText( status );
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
			Bundle bundle = intent.getExtras();
			String url = bundle.getCharSequence(Constants.PACKAGE_URI).toString();
			String state = bundle.getCharSequence(Constants.PACKAGE_STATE).toString();
			AppMarket.this.updateStatusUI( url + " " + state );
		}
	};
}