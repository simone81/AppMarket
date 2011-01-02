package net.behoo.appmarket;

import net.behoo.appmarket.http.DownloadConstants;
import net.behoo.appmarket.http.ImageDownloadTask;
import net.behoo.appmarket.http.PausableThreadPoolExecutor;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface.OnCancelListener;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

abstract public class AsyncTaskActivity extends Activity
										implements OnCancelListener {
	public static final int WAITING_DIALOG = 0;
	public static final int TASK_ERROR_DIALOG = 1;
	
	private static final String TAG = "AsyncTaskActivity";
	
	private PausableThreadPoolExecutor mThreadPool = new PausableThreadPoolExecutor(5);
	
	protected Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
        	switch (msg.what) {
        	case DownloadConstants.MSG_PROTOCOL_SUCCEED:
        		AsyncTaskActivity.this.dismissDialog(AsyncTaskActivity.WAITING_DIALOG);
        		AsyncTaskActivity.this.onTaskCompleted(true);
        		break;
        	case DownloadConstants.MSG_PROTOCOL_FAILURE:
        		Log.w(TAG, "handleMessage "+"MSG_PROTOCOL_FAILURE");
        		AsyncTaskActivity.this.dismissDialog(AsyncTaskActivity.WAITING_DIALOG);
        		AsyncTaskActivity.this.onTaskCompleted(false);
        		showDialog(TASK_ERROR_DIALOG);
        		break;
        	case DownloadConstants.MSG_IMG_SUCCEED: {
        		Bundle data = msg.getData();
        		AsyncTaskActivity.this.onImageCompleted(true, 
        				data.getString(DownloadConstants.MSG_DATA_URL),
        				data.getString(DownloadConstants.MSG_DATA_APPCODE)
        				);
        		break;
        	}
        	case DownloadConstants.MSG_IMG_FAILURE: {
        		Bundle data = msg.getData();
        		AsyncTaskActivity.this.onImageCompleted(false, 
        				data.getString(DownloadConstants.MSG_DATA_URL),
        				data.getString(DownloadConstants.MSG_DATA_APPCODE)
        				);
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
        	Dialog dlg = ProgressDialog.show((Context)this, 
        			this.getString(R.string.main_dlginit_title), 
        			this.getString(R.string.main_dlginit_content),
        			true,
        			true);
        	dlg.setOnCancelListener(this);
        	return dlg;
        case TASK_ERROR_DIALOG:
        	return new AlertDialog.Builder(this)
            	.setTitle(R.string.market_title)
            	.setPositiveButton(R.string.dlg_httptask_retry, new DialogInterface.OnClickListener() {
            		public void onClick(DialogInterface dialog, int which) {
            			Log.i(TAG, "retry http task");
            			onTaskRetry();
            		}})
            	.setNegativeButton(R.string.dlg_httptask_exit, new DialogInterface.OnClickListener() {
            		public void onClick(DialogInterface dialog, int which) {
            			Log.i(TAG, "exit this activity");
            			finish();
            		}})
            	.setMessage(R.string.dlg_httptask_error_hint)
            	.setOnCancelListener(this)
            	.create();
        default:
        	break;
       }
       return null;
	}
	
	public void onCancel(DialogInterface dialog) {
		onTaskCanceled(dialog);
		finish();
	}
	
	protected void executeTask(Runnable task) {
		mThreadPool.execute(task);
	}
	
	protected void executeImageTask(String url, String appCode) {
		ImageDownloadTask task = new ImageDownloadTask(url, appCode, mHandler);
		ImageLib.inst().setDownloadFlag(url);
		mThreadPool.execute(task);
	}
	protected void onTaskRetry() {}
	protected void onTaskCanceled(DialogInterface dialog) {}
	protected void onTaskCompleted(boolean result) {}
	protected void onImageCompleted(boolean result, String url, String appcode) {}
}
