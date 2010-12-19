package net.behoo.appmarket;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import net.behoo.appmarket.http.UrlHelpers;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class AppUpdatePage extends AsyncTaskActivity 
						   implements OnItemSelectedListener, OnClickListener {
	private static final String TAG = "AppUpdatePage";
	
	private DownloadInstallService mInstallService = null;
	
	private ArrayList<AppInfo> mAppLib = new ArrayList<AppInfo>();
	private Set<String> mImageDownloadFlags = new HashSet<String>();
	private ImageView mAppImage = null;
	private ListView mListView = null;
	private UpdateListAdapter mListAdapter = null;
	
	private ServiceConnection mServiceConn = new ServiceConnection() {
		
    	public void onServiceConnected(ComponentName cname, IBinder binder){
    		mInstallService = ((DownloadInstallService.LocalServiceBinder)binder).getService();
    		
    		updateListView();
    		mInstallService.checkUpdate();
    	}
    	
    	public void onServiceDisconnected(ComponentName cname){
    		mInstallService = null;
    	}
    };
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (null != mInstallService) {
				updateListView();
			}
		}
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_update_page);
		
		mListView = (ListView)findViewById(R.id.app_update_list);
		mListAdapter = new UpdateListAdapter(this);
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemSelectedListener(this);
		
		mAppImage = (ImageView)findViewById(R.id.main_app_logo);
		
		Button button = (Button)findViewById(R.id.appupdate_btn_update);
		button.setOnClickListener(this);
	}
	
	public void onResume() {
		super.onResume();
		this.bindService(new Intent(this, DownloadInstallService.class), mServiceConn, Context.BIND_AUTO_CREATE);
		this.registerReceiver(mReceiver, new IntentFilter(Constants.ACTION_UPDATE_STATE));
	}
	
	public void onPause() {
		super.onStop();
		this.unbindService(mServiceConn);
		this.unregisterReceiver(mReceiver);
	}
	
	public void onClick(View v) {
		// TODO Auto-generated method stub
		int pos = mListView.getSelectedItemPosition();
		Log.i(TAG, "onClick "+Integer.toString(pos));
		if (ListView.INVALID_POSITION != pos) {
			mInstallService.downloadAndInstall(
					UrlHelpers.getDownloadUrl("token", mAppLib.get(pos).mAppCode), 
					AppInfo.MIMETYPE, mAppLib.get(pos));
			
			Intent i = new Intent();
			i.setClass(this, AppDownloadPage.class);
			startActivity(i);
		}
	}
	
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		updateUIState();
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}
	
	protected void onImageCompleted(boolean result, String appcode) {
		mImageDownloadFlags.add(appcode);
		if (result) {
			int pos = mListView.getSelectedItemPosition();
			if (ListView.INVALID_POSITION != pos) {
				String code = (String)mListView.getItemAtPosition(pos);
				if (0 == code.compareTo(appcode)) {
					updateImage(mAppLib.get(pos));
				}
			}
		}
	}
	
	public void updateListView() {
		ArrayList<AppInfo> appLib = mInstallService.getUpdateList();
		if (null != appLib) {
			mAppLib = appLib;
			mListAdapter.notifyDataSetChanged();
			if (ListView.INVALID_POSITION == mListView.getSelectedItemPosition() 
				&& mListView.getCount() > 0) {
				mListView.setSelection(0);
			}
			updateUIState();
		}
	}
	
	public void updateUIState() {
		assert(mListView.getCount() == mAppLib.size());
		int pos = mListView.getSelectedItemPosition();
		if (ListView.INVALID_POSITION != pos && mListView.getCount() > 0) {
			AppInfo appInfo = mAppLib.get(pos);
			
			TextView tv = (TextView)findViewById(R.id.main_app_title);
			tv.setText(appInfo.mAppName);
			
			tv = (TextView)findViewById(R.id.main_app_author);
			tv.setText(appInfo.mAppAuthor);
			
			tv = (TextView)findViewById(R.id.main_app_version);
			tv.setText(appInfo.mAppVersion);
			
			tv = (TextView)findViewById(R.id.app_update_desc);
			tv.setText(appInfo.mAppShortDesc);
			
			updateImage(appInfo);
		}
	}
	
	private void updateImage(AppInfo appInfo) {
		if (null == appInfo.getDrawable()) {
			if (false == mImageDownloadFlags.contains(appInfo.mAppCode)) {
				executeImageTask(appInfo);
			}
			else {
				mAppImage.setImageResource(R.drawable.test);
			}
		}
		else {
			mAppImage.setImageDrawable(appInfo.getDrawable());
		}
	}
	
	private class UpdateListAdapter extends BaseAdapter {
		//private Context mContext = null;
		private LayoutInflater mInflater = null;
        
        public UpdateListAdapter(Context context) {
            //mContext = context;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return mAppLib.size();
        }

        public Object getItem(int position) {
            return mAppLib.get(position).mAppCode;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text;
            
            if (convertView == null) {
                text = (TextView)mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            } else {
                text = (TextView)convertView;
            }
            text.setText(mAppLib.get(position).mAppName);
            return text;
        }
    }
}
