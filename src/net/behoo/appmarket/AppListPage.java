package net.behoo.appmarket;

import java.util.ArrayList;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.ProtocolDownloadTask;
import net.behoo.appmarket.http.UrlHelpers;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.view.View.OnKeyListener;

public class AppListPage extends AsyncTaskActivity 
						 implements OnClickListener, 
						 OnItemSelectedListener, OnKeyListener,
						 OnItemClickListener {
	
	private static final String TAG = "AppListPage";
	private static final int PAGE_SIZE = 20;
	
	private ArrayList<AppInfo> mAppList = new ArrayList<AppInfo>();
	private HttpTask mHttpTask = null;
	private ListView mListView = null;
	private AppListAdapter mListAdapter = null;
	private ImageView mAppImage = null;
	private boolean mContinueDownload = true;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_list_page);
		
		mListAdapter = new AppListAdapter(this);
		mListView = (ListView)findViewById(R.id.app_list);
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemSelectedListener(this);
		mListView.setOnItemClickListener(this);
		mListView.setOnKeyListener(this);
		
		Button button = (Button)findViewById(R.id.applist_btn_detail);
		button.setOnClickListener(this);
		
		mAppImage = (ImageView)findViewById(R.id.main_app_logo);
			
		mHttpTask = new HttpTask(mHandler);
		executeTask(mHttpTask);
		showDialog(WAITING_DIALOG);
	}
	
	public void onClick(View v) {
		if (v.getId() == R.id.applist_btn_detail) {
			int pos = mListView.getSelectedItemPosition();
			if (ListView.INVALID_POSITION != pos) {
				startDetailsActivity(pos);
			}
		}
	}

	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_DPAD_DOWN == keyCode 
				&& mListView.getSelectedItemPosition() == mListView.getCount()-1) {
			if (mContinueDownload && event.getAction() == KeyEvent.ACTION_DOWN) {
				executeTask(mHttpTask);
				showDialog(WAITING_DIALOG);
			}
			return true;
		}
		return false;
	} 
	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		startDetailsActivity(position);
	}
	
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		updateUIState();
	}
	
	public void onNothingSelected(AdapterView<?> view) {
	}
	
	protected void onTaskRetry() {
    	executeTask(mHttpTask);
    	showDialog(WAITING_DIALOG);
    }
	
	protected void onTaskCanceled(DialogInterface dlg) {
		mHttpTask.setHandler(null);
    	mHttpTask.cancel();
    }
	
	protected void onTaskCompleted(boolean result) {
		if (result) {
			mListAdapter.notifyDataSetChanged();
			mListView.requestFocus();
			updateUIState();
		}
	}
	
	protected void onImageCompleted(boolean result, String url, String appcode) {
		if (result) {
			int pos = mListView.getSelectedItemPosition();
			if (ListView.INVALID_POSITION != pos) {
				String code = (String)mListView.getItemAtPosition(pos);
				if (0 == code.compareTo(appcode)) {
					updateImage(mAppList.get(pos));
				}
			}
		}
	}
	
	private void startDetailsActivity(int pos) {
		Intent intent = new Intent();
		intent.setClass(this, AppDetailsPage.class);
		String appCode = mAppList.get(pos).mAppCode;
		intent.setData(AppInfo.makeUri(appCode));
		startActivity( intent );
	}
	
	private void updateUIState() {
		int pos = mListView.getSelectedItemPosition();
		if (ListView.INVALID_POSITION != pos && mListView.getCount() > 0) {
			AppInfo appInfo = mAppList.get(pos);
			
			TextView tv = (TextView)findViewById(R.id.main_app_title);
			tv.setText(appInfo.mAppName);
			
			tv = (TextView)findViewById(R.id.main_app_author);
			tv.setText(appInfo.mAppAuthor);
			
			tv = (TextView)findViewById(R.id.main_app_version);
			tv.setText(appInfo.mAppVersion);
			
			tv = (TextView)findViewById(R.id.app_list_desc);
			tv.setText(appInfo.mAppShortDesc);
			
			updateImage(appInfo);
		}
	}
	
	private void updateImage(AppInfo appInfo) {
		if (null == ImageLib.inst().getBitmap(appInfo.mAppImageUrl)) {
			mAppImage.setImageResource(R.drawable.appicon_default);
			if (false == ImageLib.inst().isImageDownloading(appInfo.mAppImageUrl)) {
				executeImageTask(appInfo.mAppImageUrl, appInfo.mAppCode);
			}
		}
		else {
			mAppImage.setImageBitmap(ImageLib.inst().getBitmap(appInfo.mAppImageUrl));
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
		
		public boolean doTask() {
	    	try {
	    		String url = UrlHelpers.getAppListUrl(
	    			TokenWrapper.getToken(AppListPage.this), 
	    			mAppList.size() + 1, PAGE_SIZE);
	    		Log.i(TAG, "doTask "+url);
	    		
	    		ArrayList<AppInfo> appLib = mAppListProxy.getAppList(url, PAGE_SIZE);
				mAppList.addAll(appLib);
				mContinueDownload = (mAppListProxy.getAppListTotal() > mAppList.size());
				return (appLib.size() > 0);
	    	} catch (Throwable tr) {
	    		Log.w(TAG, "doTask "+tr.getLocalizedMessage());
	    	} 
	    	return false;
		}
    };
    
	private class AppListAdapter extends BaseAdapter {
		//private Context mContext = null;
        private LayoutInflater mInflater = null;
        
        public AppListAdapter(Context context) {
            //mContext = context;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return mAppList.size();
        }

        public Object getItem(int position) {
            return mAppList.get(position).mAppCode;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	View view = null;
        	if (convertView == null) {
        		view = mInflater.inflate(R.layout.applist_item_layout, parent, false);
        	}
        	else {
        		view = convertView;
        	}
        	
        	// update state
            AppInfo appInfo = mAppList.get(position);
            TextView titleView = (TextView)view.findViewById(R.id.applist_item_title);
            titleView.setText(appInfo.mAppName);
            
            return view;
        }
    }
}
