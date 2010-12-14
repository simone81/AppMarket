package net.behoo.appmarket;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.http.DownloadConstants;
import net.behoo.appmarket.http.ImageDownloadTask;
import net.behoo.appmarket.http.PausableThreadPoolExecutor;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

abstract public class AsyncTaskActivity extends Activity {
	
	public static final int WAITING_DIALOG = 0;
	
	private PausableThreadPoolExecutor mThreadPool = new PausableThreadPoolExecutor(5);
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
        	switch (msg.what) {
        	case DownloadConstants.MSG_PROTOCOL_SUCCEED:
        		AsyncTaskActivity.this.dismissDialog(AsyncTaskActivity.WAITING_DIALOG);
        		AsyncTaskActivity.this.onTaskCompleted(true);
        		break;
        	case DownloadConstants.MSG_PROTOCOL_FAILURE:
        		AsyncTaskActivity.this.dismissDialog(AsyncTaskActivity.WAITING_DIALOG);
        		AsyncTaskActivity.this.onTaskCompleted(false);
        		break;
        	case DownloadConstants.MSG_IMG_SUCCEED: {
        		Bundle data = msg.getData();
        		AsyncTaskActivity.this.onImageCompleted(true, data.getString(DownloadConstants.MSG_DATA_APPCODE));
        		break;
        	}
        	case DownloadConstants.MSG_IMG_FAILURE: {
        		Bundle data = msg.getData();
        		AsyncTaskActivity.this.onImageCompleted(false, data.getString(DownloadConstants.MSG_DATA_APPCODE));
        		break;
        	}
            default:
                break;
        	}
        	super.handleMessage(msg);
        }
	};
	
	public void onResume() {
    	super.onResume();
    	mThreadPool.resume();
    }
    
    public void onPause() {
    	super.onPause();
    	mThreadPool.pause();
    }
    
	public void onDestroy() {
    	super.onDestroy();

    	mThreadPool.resume();
    	mThreadPool.shutdown();
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
	
	protected void executeTask(Runnable task) {
		mThreadPool.execute(task);
	}
	
	protected void executeImageTask(AppInfo appInfo) {
		ImageDownloadTask task = new ImageDownloadTask(appInfo, mHandler);
		mThreadPool.execute(task);
	}

	protected void onTaskCompleted(boolean result) {}
	protected void onImageCompleted(boolean result, String appcode) {}
}
