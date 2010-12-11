package net.behoo.appmarket;


import java.io.InputStream;

import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.UrlHelpers;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class AppUpdatePage extends AsyncTaskActivity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_update_page);
		
		ListView lv = (ListView)findViewById(R.id.app_update_list);
		lv.setAdapter(new UpdateListAdapter(this));
		
		startTaskAndShowDialog();
	}
	
	protected boolean onRunTask() {
		try {
			// make the request string
			String reqStr = new String();
			reqStr = "<BH_S_App_Code_List count=";
			reqStr += String.format("%d", 10);
			reqStr += ">";
			
			for (int i = 0; i < 10; ++i) {
				reqStr += "<BH_S_App_Code_Version>";
					reqStr += "<BH_D_App_Code>";
					reqStr += String.format("%d", i);
					reqStr += "</BH_D_App_Code>";
					
					reqStr += "<BH_D_App_Version>";
					reqStr += "</BH_D_App_Version>";
					
				reqStr += "</BH_S_App_Code_Version>";
			}
			
			reqStr += "</BH_S_App_Code_List>";
			
			HttpUtil httpUtil = new HttpUtil();
			String url = UrlHelpers.getUpdateUrl("", null);
			InputStream inputStream = httpUtil.httpPost("http://192.168.1.5", reqStr);
			AppListParser.parse(inputStream);
			return true;
		}
		catch (Throwable tr) {
			return false;
		}
	}
	
	protected void onTaskCompleted(int result) {
		
	}
	
	private class UpdateListAdapter extends BaseAdapter {
		private Context mContext;
        private LayoutInflater mInflater;
        
        public UpdateListAdapter(Context context) {
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
            text.setText("54321");
            return text;
        }
    }
}
