package net.behoo.appmarket;

import java.io.InputStream;

import net.behoo.appmarket.http.AppListParser;
import net.behoo.appmarket.http.HttpUtil;
import net.behoo.appmarket.http.UrlHelpers;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

abstract public class AsyncTaskActivity extends Activity {
	
	private static final int WAITING_DIALOG = 0;
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
        	switch (msg.what) {
        	case HttpUtil.DOWNLOAD_SUCCEED:
        		AsyncTaskActivity.this.dismissDialog(AsyncTaskActivity.WAITING_DIALOG);
        		AsyncTaskActivity.this.onTaskCompleted(HttpUtil.DOWNLOAD_SUCCEED);
        		break;
        	case HttpUtil.DOWNLOAD_FAILURE:
        		AsyncTaskActivity.this.dismissDialog(AsyncTaskActivity.WAITING_DIALOG);
        		AsyncTaskActivity.this.onTaskCompleted(HttpUtil.DOWNLOAD_FAILURE);
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
			if (!AsyncTaskActivity.this.onRunTask()) {
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
        
        mDownloadThread.start();
        showDialog(WAITING_DIALOG);
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
	
	abstract protected boolean onRunTask() ;
	
	abstract protected void onTaskCompleted(int result) ;
}
