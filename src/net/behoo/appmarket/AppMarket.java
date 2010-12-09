package net.behoo.appmarket;

import java.io.InputStream;
import java.util.ArrayList;

import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.UrlHelpers;
import net.behoo.appmarket.data.AppInfo;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
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
import android.widget.ListView;
import android.widget.TextView;

public class AppMarket extends Activity implements OnClickListener {
	
	private static final String TAG = "AppMarket";
	
	private static final int WAITING_DIALOG = 0;
	
	private ArrayList<AppInfo> mAppLib = new ArrayList<AppInfo>();
	
	private boolean mServiceBound = false;
	private DownloadInstallService mInstallService = null;
	
	private GridView mGridView = null;
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
        	switch (msg.what) {
        	case HttpUtil.DOWNLOAD_SUCCEED:
        		AppMarket.this.mGridView.invalidate();
        		AppMarket.this.updateUIState();
        		AppMarket.this.dismissDialog(AppMarket.WAITING_DIALOG);
        		break;
        	case HttpUtil.DOWNLOAD_FAILURE:
        		AppMarket.this.dismissDialog(AppMarket.WAITING_DIALOG);
        		// retry ?
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
			int msg = HttpUtil.DOWNLOAD_SUCCEED;
			try {
				HttpUtil httpUtil = new HttpUtil();
				InputStream stream = httpUtil.httpGet(UrlHelpers.getPromotionUrl(""));
				AppListParser.parse(stream, mAppLib);
			}
			catch ( Throwable tr ) {
				msg = HttpUtil.DOWNLOAD_FAILURE;
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
        
        TextView tv = (TextView)findViewById(R.id.market_subtitle);
        tv.setText(R.string.market_promotion);
        
        Button button = ( Button )findViewById( R.id.main_btn_install );
        button.setOnClickListener(this);
        
        button = ( Button )findViewById( R.id.main_btn_download_page );
        button.setOnClickListener(this);
        
        button = ( Button )findViewById( R.id.main_btn_update_page );
        button.setOnClickListener(this);
        
        button = ( Button )findViewById( R.id.main_btn_applist_page );
        button.setOnClickListener(this);
        
        mGridView = ( GridView )findViewById( R.id.main_app_grid );
        mGridView.setAdapter(new AppAdapter(this));
        
        mDownloadThread.start();
        startService(new Intent(this, DownloadInstallService.class));
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
	
	public Dialog onCreateDialog(int id) {
		switch (id) {
        case WAITING_DIALOG:
        	return ProgressDialog.show((Context)this, 
        			this.getString(R.string.main_dlginit_title), 
        			this.getString(R.string.main_dlginit_content),
        			true );
       }
       return null;
	}
	
	private void updateUIState() {
		if (mGridView.getCount() > 0 ) {
			int pos = mGridView.getSelectedItemPosition();
			if (ListView.INVALID_POSITION == pos) {
				mGridView.setSelection(0);
				pos = 0;
			}
			
			AppInfo appInfo = mAppLib.get(pos);
			TextView tv = (TextView)findViewById(R.id.main_app_title);
			tv.setText(appInfo.mAppName);
			
			tv = (TextView)findViewById(R.id.main_app_author);
			tv.setText(appInfo.mAppAuthor);
			
			tv = (TextView)findViewById(R.id.main_app_version);
			tv.setText(appInfo.mAppVersion);
			
			tv = (TextView)findViewById(R.id.main_app_desc);
			tv.setText(appInfo.mAppShortDesc);
		}
		else {
			
		}
	}
	
	public class AppAdapter extends BaseAdapter {
    	private Context _mContext;
    	public AppAdapter(Context c) {
            _mContext = c;
        }

        public int getCount() {
        	return mAppLib.size();
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
            
            AppInfo appInfo = AppMarket.this.mAppLib.get(position);
            if (null != appInfo) {
            	imageView.setImageURI(Uri.parse(appInfo.mAppImageUrl));
            }
            else {
            	imageView.setImageResource(R.drawable.test);
            }
            return imageView;
        }
    }
}