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
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

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
        	case DOWNLOAD_SUCCEED:
        		break;
        	case DOWNLOAD_FAILURE:
        		break;
            default:
                break;
        	}
        	super.handleMessage(msg);
        }
	};
	
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
        setContentView(R.layout.app_market);
        
        Button button = ( Button )findViewById( R.id.main_btn_install );
        button.setOnClickListener(this);
        
        button = ( Button )findViewById( R.id.main_btn_download_page );
        button.setOnClickListener(this);
        
        button = ( Button )findViewById( R.id.main_btn_update_page );
        button.setOnClickListener(this);
        
        button = ( Button )findViewById( R.id.main_btn_applist_page );
        button.setOnClickListener(this);
       
        //mDownloadThread.start();
        startService(new Intent(this, DownloadInstallService.class));
        
        GridView gv = ( GridView )findViewById( R.id.main_app_grid );
        gv.setAdapter(new AppAdapter(this));
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
			intent.setClass(this, DownloadPage.class);
		}
		startActivity( intent );
    }
	
	public class AppAdapter extends BaseAdapter {
    	private Context _mContext;
    	public AppAdapter(Context c) {
            _mContext = c;
        }

        public int getCount() {
        	return 8;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView( _mContext );
                imageView.setLayoutParams(new GridView.LayoutParams(60, 60));
                imageView.setAdjustViewBounds(false);
                imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            imageView.setImageResource(R.drawable.test);
            return imageView;
        }
    }
}