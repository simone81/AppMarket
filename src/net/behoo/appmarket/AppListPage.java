package net.behoo.appmarket;

import java.io.InputStream;
import java.util.ArrayList;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.UrlHelpers;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class AppListPage extends Activity implements OnClickListener {
	
	private static final int WAITING_DIALOG = 0;
	
	private ArrayList<AppInfo> mAppList = new ArrayList<AppInfo>();
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
        	switch (msg.what) {
        	case HttpUtil.DOWNLOAD_SUCCEED:
        		AppListPage.this.dismissDialog(AppListPage.WAITING_DIALOG);
        		break;
        	case HttpUtil.DOWNLOAD_FAILURE:
        		AppListPage.this.dismissDialog(AppListPage.WAITING_DIALOG);
        		// retry ?
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
			int msg = HttpUtil.DOWNLOAD_SUCCEED;
			try {
				HttpUtil httpUtil = new HttpUtil();
				InputStream stream = httpUtil.httpGet(UrlHelpers.getPromotionUrl(""));
				AppListParser.parse(stream, mAppList);
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
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_list_page);
		
		ListView lv = (ListView)findViewById(R.id.app_list);
		lv.setAdapter(new AppListAdapter(this));
		
		Button button = ( Button )findViewById( R.id.applist_btn_detail );
		button.setOnClickListener(this);
		
		mDownloadThread.start();
		showDialog(WAITING_DIALOG);
	}
	
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.setClass(AppListPage.this, DetailsPage.class);
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
	
	private class AppListAdapter extends BaseAdapter {
		private Context mContext;
        private LayoutInflater mInflater;
        
        public AppListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount() {
            return 10;
        }

        public Object getItem(int position) {
            return position;
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
            text.setText("込込込込込込込込");
            return text;
        }
    }
}
