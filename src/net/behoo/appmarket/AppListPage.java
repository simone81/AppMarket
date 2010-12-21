package net.behoo.appmarket;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.ProtocolDownloadTask;
import net.behoo.appmarket.http.UrlHelpers;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

public class AppListPage extends AsyncTaskActivity 
						 implements OnClickListener, 
						 OnItemSelectedListener {
	private static final String TAG = "AppListPage";
	
	private ArrayList<AppInfo> mAppList = new ArrayList<AppInfo>();
	private HttpTask mHttpTask = null;
	
	private boolean mFirstRun = true;
	private ListView mListView = null;
	private AppListAdapter mListAdapter = null;
	private Set<String> mImageDownloadFlags = new HashSet<String>();
	private ImageView mAppImage = null;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_list_page);
		
		mListAdapter = new AppListAdapter(this);
		mListView = (ListView)findViewById(R.id.app_list);
		mListView.setAdapter(mListAdapter);
		mListView.setOnItemSelectedListener(this);
		
		Button button = ( Button )findViewById( R.id.applist_btn_detail );
		button.setOnClickListener(this);
		
		mAppImage = (ImageView)findViewById(R.id.main_app_logo);
		
		mHttpTask = new HttpTask(mHandler);
	}
	
	public void onResume() {
		super.onResume();
		if (mFirstRun) {
			executeTask(mHttpTask);
			showDialog(WAITING_DIALOG);
		}
	}
	
	public void onClick(View v) {
		int pos = mListView.getSelectedItemPosition();
		Log.i(TAG, "onClick pos "+Integer.toString(pos));
		if (ListView.INVALID_POSITION != pos) {
			Intent intent = new Intent();
			intent.setClass(AppListPage.this, AppDetailsPage.class);
			String [] value = {
				mAppList.get(pos).mAppName,
				mAppList.get(pos).mAppVersion,
				mAppList.get(pos).mAppCode,
				mAppList.get(pos).mAppAuthor,
				mAppList.get(pos).mAppImageUrl,
				mAppList.get(pos).mAppDesc,
			};
			intent.putExtra("net.behoo.appmarket.AppDetailsPage", value);
			
			startActivity( intent );
		}
	}
	
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		updateUIState();
	}
	
	public void onNothingSelected(AdapterView<?> view) {
	}
	
	protected void onTaskCompleted(boolean result) {
		Log.i(TAG, String.format("onTaskComplete ret: %d, count: %d", result?1:0, mAppList.size()));
		mListAdapter.notifyDataSetChanged();
		if (mListView.getCount() > 0) {
			mListView.setSelection(0);
		}
		mListView.requestFocus();
		updateUIState();
	}
	
	protected void onImageCompleted(boolean result, String appcode) {
		mImageDownloadFlags.add(appcode);
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
	
	private void updateUIState() {
		assert(mListView.getCount() == mAppList.size());
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
		if (null == appInfo.getDrawable()) {
			mAppImage.setImageResource(R.drawable.test);
			if (false == mImageDownloadFlags.contains(appInfo.mAppCode)) {
				executeImageTask(appInfo);
			}
		}
		else {
			mAppImage.setImageDrawable(appInfo.getDrawable());
		}
	}
	
	private class HttpTask extends ProtocolDownloadTask {
		
		public HttpTask(Handler handler) {
			super(handler);
		}
		
		public boolean doTask() {
	    	try {
	    		HttpUtil httpUtil = new HttpUtil();
	    		String url = UrlHelpers.getAppListUrl("", 0, 20);
				InputStream stream = httpUtil.httpGet(url);
				ArrayList<AppInfo> appLib = AppListParser.parse(stream);
				if (null != appLib) {
					mAppList.addAll(appLib);
					return true;
				}
				return false;
	    	} catch (Throwable tr) {
	    		return false;
	    	}
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
        		Log.i(TAG, "getView create "+Integer.toString(position));
        		view = mInflater.inflate(R.layout.applist_item_layout, parent, false);
        		view.setFocusable(true);
        	}
        	else {
        		Log.i(TAG, "getView convertView "+Integer.toString(position));
        		view = convertView;
        	}
        	
        	// update state
            AppInfo appInfo = mAppList.get(position);
            TextView tv = (TextView)view.findViewById(R.id.applist_item_title);
            tv.setText(appInfo.mAppName);
            
            return view;
        }
    }  
}
