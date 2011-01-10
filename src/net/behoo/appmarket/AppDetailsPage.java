package net.behoo.appmarket;

import net.behoo.appmarket.InstallButtonGuard.OnInstallClickListener;
import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.http.AppDetailParser;
import net.behoo.appmarket.http.ProtocolDownloadTask;
import net.behoo.appmarket.http.UrlHelpers;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

public class AppDetailsPage extends AsyncTaskActivity implements OnInstallClickListener {
	
	public static final String EXTRA_KAY = "net.behoo.appmarket.AppDetailsPage";
	
	private static final String TAG = "DetailsPage";

	private Button mInstallButton = null;
	private InstallButtonGuard mInstallButtonGuard = null;

	private AppInfo mAppInfo = new AppInfo();
	private HttpTask mHttpTask = null;
	
	private Integer[] mRemoteCntl = {
		R.string.appdetails_rc_desc1,
		R.string.appdetails_rc_desc1,
		R.string.appdetails_rc_desc2,
		R.string.appdetails_rc_desc3,
		R.string.appdetails_rc_desc4,
	};
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (null != mInstallButtonGuard) {
				mInstallButtonGuard.updateAppState();
			}
		}
	};
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_details_page); 
        
        // get the summary application information
        String [] value = getIntent().getStringArrayExtra("net.behoo.appmarket.AppDetailsPage");
        mAppInfo = new AppInfo(value[0], value[1], value[2], value[3], value[4], value[5]);
        
        mInstallButton = (Button)findViewById(R.id.detail_btn_install);
        mInstallButtonGuard = new InstallButtonGuard(mInstallButton, mAppInfo,
        		ServiceManager.inst().getDownloadHandler());
        mInstallButtonGuard.setOnInstallClickListener(this);
        
        mHttpTask = new HttpTask(mHandler);
        executeTask(mHttpTask);
        showDialog(WAITING_DIALOG);
    }
    
    public void onInstallClicked(AppInfo appInfo) {
		// TODO Auto-generated method stub
		Intent intent = new Intent();
		intent.setClass(this, AppDownloadPage.class);
		startActivity(intent);
	};
    
    public void onResume() {
    	super.onResume();
    	registerReceiver(mReceiver, new IntentFilter(Constants.ACTION_DWONLOAD_INSTALL_STATE));
    	mInstallButtonGuard.updateAppState();
    }
    
    public void onPause() {
    	super.onPause();
    	unregisterReceiver(mReceiver);
    }
	
    protected void onTaskCanceled(DialogInterface dlg) {
    	mHttpTask.cancel();
    }
    
    protected void onTaskRetry() {
    	executeTask(mHttpTask);
    	showDialog(WAITING_DIALOG);
    }
    
	protected void onTaskCompleted(boolean result) {
		if (result) {
			updateUIState();
			executeImageTask(mAppInfo.mAppImageUrl, mAppInfo.mAppCode);
			executeImageTask(mAppInfo.mAppScreenShorts, mAppInfo.mAppCode);
		}
	}
	
	protected void onImageCompleted(boolean result, String url, String appcode) {
		if (result) {
			if (null != ImageLib.inst().getDrawable(url)) {
				if (0 == url.compareTo(mAppInfo.mAppImageUrl)) {
					ImageView iv = (ImageView)findViewById(R.id.main_app_logo);
					iv.setImageDrawable(ImageLib.inst().getDrawable(url));
				}
				else if (0 == url.compareTo(mAppInfo.mAppScreenShorts)) {
					ImageView iv = (ImageView)findViewById(R.id.detail_screenshort_1);
					iv.setImageDrawable(ImageLib.inst().getDrawable(url));
				}
			}
		}
	}
	
	private void updateUIState() {
		TextView tv = (TextView)findViewById(R.id.main_app_title);
		tv.setText(mAppInfo.mAppName);
		
		tv = (TextView)findViewById(R.id.main_app_author);
		tv.setText(mAppInfo.mAppAuthor);
		
		tv = (TextView)findViewById(R.id.main_app_version);
		tv.setText(mAppInfo.mAppVersion);
		
		tv = (TextView)findViewById(R.id.main_app_size);
		Log.i(TAG, "updateUIState size: "+mAppInfo.mAppSize);
		if (mAppInfo.mAppSize.length() > 0 && null != mAppInfo.mAppSize) {
			tv.setVisibility(View.VISIBLE);
			int size = Integer.valueOf(mAppInfo.mAppSize).intValue();
			tv.setText(Formatter.formatFileSize(this, size));
		}
		else {
			tv.setVisibility(View.GONE);
		}
		
		ImageView iv = null;
		if (null != ImageLib.inst().getDrawable(mAppInfo.mAppImageUrl)) {
			iv = (ImageView)findViewById(R.id.main_app_logo);
			iv.setImageDrawable(ImageLib.inst().getDrawable(mAppInfo.mAppImageUrl));
		}
		
		iv = (ImageView)findViewById(R.id.detail_screenshort_1);
		iv.setImageResource(R.drawable.test);
		
		tv = (TextView)findViewById(R.id.detail_review_desc);
		tv.setText(mAppInfo.mAppReview);
		
		tv = (TextView)findViewById(R.id.detail_rc_desc);
		int score = 0;
		try {
		    score = Integer.parseInt(mAppInfo.mAppRemoteCntlScore);
		    tv.setText(mRemoteCntl[score%5]);
		    
		    RatingBar rb = (RatingBar)findViewById(R.id.detail_ratingbar);
		    rb.setRating(score%5);
		} catch(NumberFormatException nfe) {
		   Log.w(TAG, "Could not parse " + nfe);
		} 	
	}
	
	private class HttpTask extends ProtocolDownloadTask {
		
		private AppDetailParser mDataProxy = new AppDetailParser();
		
		public HttpTask(Handler handler) {
			super(handler);
		}
		
		public void cancel() {
			mDataProxy.cancel();
		}
		
		protected boolean doTask() {
			try {
				String url = UrlHelpers.getAppDetailUrl(
						ServiceManager.inst().getSyncHandler().getToken(), mAppInfo.mAppCode);
				Log.i(TAG, "doTask "+url);
				
				AppInfo appInfo = mDataProxy.getAppInfo(url);
				if (null != appInfo) {
					mAppInfo.mAppChangelog = appInfo.mAppChangelog;
					mAppInfo.mAppDesc = appInfo.mAppDesc;
					mAppInfo.mAppRemoteCntlScore = appInfo.mAppRemoteCntlScore;
					mAppInfo.mAppReview = appInfo.mAppReview;
					mAppInfo.mAppSize = appInfo.mAppSize;
					mAppInfo.mAppScreenShorts = appInfo.mAppScreenShorts;
					return true;
				}
	    	} catch (Throwable tr) {
	    		Log.i(TAG, "doTask "+tr.getLocalizedMessage());
	    	} 
	    	return false;
		}
    }
}
