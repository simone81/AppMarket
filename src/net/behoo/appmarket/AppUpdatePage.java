package net.behoo.appmarket;

import java.util.ArrayList;

import net.behoo.appmarket.InstallButtonGuard.OnInstallClickListener;
import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.downloadinstall.Constants;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class AppUpdatePage extends AsyncTaskActivity 
						   implements OnItemSelectedListener, 
						   			  OnInstallClickListener {
	//private static final String TAG = "AppUpdatePage";
	
	private ArrayList<AppInfo> mAppLib = new ArrayList<AppInfo>();
	
	private ImageView mAppImage = null;
	private ListView mListView = null;
	private UpdateListAdapter mListAdapter = null;
    private InstallButtonGuard mButtonGuard = null;
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			updateListView();
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
		mButtonGuard = new InstallButtonGuard(button, 
				null, ServiceManager.inst().getDownloadHandler());
		mButtonGuard.setOnInstallClickListener(this);
		
		updateListView();
	}
	
	public void onResume() {
		super.onResume();
		this.registerReceiver(mReceiver, new IntentFilter(Constants.ACTION_UPDATE_STATE));
	}
	
	public void onPause() {
		super.onStop();
		this.unregisterReceiver(mReceiver);
	}
	
	public void onInstallClicked(AppInfo appInfo) {
		// TODO Auto-generated method stub
		Intent i = new Intent();
		i.setClass(this, AppDownloadPage.class);
		startActivity(i);
	}
	
	public void onItemSelected(AdapterView<?> arg0, View arg1, 
			int position, long id) {
		mButtonGuard.setAppInfo(mAppLib.get(position));
		updateUIState();
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}
	
	protected void onImageCompleted(boolean result, String url, String appcode) {
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
		ArrayList<AppInfo> appLib = ServiceManager.inst().getDownloadHandler().getUpdateList();
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
		if (null == ImageLib.inst().getDrawable(appInfo.mAppImageUrl)) {
			if (false == ImageLib.inst().isImageDownloading(appInfo.mAppImageUrl)) {
				executeImageTask(appInfo.mAppImageUrl, appInfo.mAppCode);
			}
			else {
				mAppImage.setImageResource(R.drawable.test);
			}
		}
		else {
			mAppImage.setImageDrawable(ImageLib.inst().getDrawable(appInfo.mAppImageUrl));
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
        	View view = null;
        	if (convertView == null) {
        		view = mInflater.inflate(R.layout.applist_item_layout, parent, false);
        		view.setFocusable(true);
        	}
        	else {
        		view = convertView;
        	}

        	// update state
            AppInfo appInfo = mAppLib.get(position);
            TextView titleView = (TextView)view.findViewById(R.id.applist_item_title);
            titleView.setText(appInfo.mAppName);
            TextView subTitleView = (TextView)view.findViewById(R.id.applist_item_subtitle);
            subTitleView.setText(appInfo.mAppAuthor);
            return view;
        }
	}
}
