package net.behoo.appmarket;

import java.io.InputStream;
import java.util.ArrayList;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.UrlHelpers;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class AppListPage extends AsyncTaskActivity 
						 implements OnClickListener, OnItemSelectedListener {
	private static final String TAG = "AppListPage";
	
	private ArrayList<AppInfo> mAppList = new ArrayList<AppInfo>();
	private ListView mListView = null;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_list_page);
		
		mListView = (ListView)findViewById(R.id.app_list);
		mListView.setAdapter(new AppListAdapter(this));
		mListView.setOnItemSelectedListener(this);
		
		Button button = ( Button )findViewById( R.id.applist_btn_detail );
		button.setOnClickListener(this);
		
		startTaskAndShowDialog();
	}
	
	public void onClick(View v) {
		//int pos = mListView.getSelectedItemPosition();
		//if (ListView.INVALID_POSITION != pos) {
			Intent intent = new Intent();
			intent.setClass(AppListPage.this, AppDetailsPage.class);
			intent.putExtra(AppDetailsPage.APP_CODE, "");
			//intent.putExtra(DetailsPage.APP_CODE, mAppList.get(pos).mAppCode);
			startActivity( intent );
		//}
	}
	
	public void onItemSelected(AdapterView parent, View view, int position, long id) {
		updateUIState();
	}
	
	public void onNothingSelected(AdapterView view) {
		
	}
	 
	protected boolean onRunTask() {
    	try {
    		HttpUtil httpUtil = new HttpUtil();
			InputStream stream = httpUtil.httpGet(UrlHelpers.getAppListUrl("", 0, 0));
			mAppList = AppListParser.parse(stream);
			return true;
    	} catch (Throwable tr) {
    		return false;
    	}
    }
	
	protected void onTaskCompleted(int result) {
		Log.i(TAG, String.format("onTaskComplete %d", mAppList.size()));
		mListView.invalidate();
		if (mListView.getCount() > 0)
			mListView.setSelection(0);
		updateUIState();
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
		}
	}
	
	private class AppListAdapter extends BaseAdapter {
		private Context mContext;
        private LayoutInflater mInflater;
        
        public AppListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return mAppList.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.applist_item_layout, parent, false);
            view.setFocusable(true);
            
            AppInfo appInfo = mAppList.get(position);
            TextView tv = (TextView)view.findViewById(R.id.applist_item_title);
            tv.setText(appInfo.mAppName);
            
            return view;
        }
    }   
}
