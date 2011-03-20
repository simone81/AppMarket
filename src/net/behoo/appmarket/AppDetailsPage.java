package net.behoo.appmarket;

import net.behoo.appmarket.InstallButtonGuard.OnInstallClickListener;
import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.http.AppDetailParser;
import net.behoo.appmarket.http.ProtocolDownloadTask;
import net.behoo.appmarket.http.UrlHelpers;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
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
	private static final String TAG = "DetailsPage";

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
    
	private static final int APP_CODE = 0;
	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	static {
        sURIMatcher.addURI(AppInfo.AppAuthority, AppInfo.AppPath+"/#", APP_CODE);
    }
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_details_page); 
        
        Button button = (Button)findViewById(R.id.detail_btn_install);
        mInstallButtonGuard = new InstallButtonGuard(this, button, null);
        mInstallButtonGuard.setOnInstallClickListener(this);
        
        Intent intent = getIntent();
        Uri data = intent.getData();
        int matchId = sURIMatcher.match(data);
        if (APP_CODE == matchId) {
	        String appCode = data.getPathSegments().get(1);
	        mHttpTask = new HttpTask(mHandler, appCode);
	        executeTask(mHttpTask);
	        showDialog(WAITING_DIALOG);
        }
        else {
        	// error dialog
        	Log.w(TAG, "unresolved data received.");
        }
    }
	
    public void onInstallClicked(AppInfo appInfo) {
		// TODO Auto-generated method stub
		Intent intent = new Intent();
		intent.setClass(this, AppDownloadPage.class);
		startActivity(intent);
	};
    
	public void onResume() {
		super.onResume();
		mInstallButtonGuard.enableGuard();
	}
	
    public void onPause() {
    	super.onPause();
    	mInstallButtonGuard.disableGuard();
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
	        mInstallButtonGuard.setAppInfo(mAppInfo);
			updateUIState();
			executeImageTask(mAppInfo.mAppImageUrl, mAppInfo.mAppCode);
			executeImageTask(mAppInfo.mAppScreenShorts1, mAppInfo.mAppCode);
		}
	}
	
	protected void onImageCompleted(boolean result, String url, String appcode) {
		if (result) {
			if (null != ImageLib.inst().getBitmap(url)) {
				if (0 == url.compareTo(mAppInfo.mAppImageUrl)) {
					ImageView iv = (ImageView)findViewById(R.id.main_app_logo);
					iv.setImageBitmap(ImageLib.inst().getBitmap(url));
				}
				else if (0 == url.compareTo(mAppInfo.mAppScreenShorts1)) {
					ImageView iv = (ImageView)findViewById(R.id.detail_screenshort_1);
					iv.setImageBitmap(ImageLib.inst().getBitmap(url));
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
		if (null != ImageLib.inst().getBitmap(mAppInfo.mAppImageUrl)) {
			iv = (ImageView)findViewById(R.id.main_app_logo);
			iv.setImageBitmap(ImageLib.inst().getBitmap(mAppInfo.mAppImageUrl));
		}
		
		iv = (ImageView)findViewById(R.id.detail_screenshort_1);
		iv.setImageResource(R.drawable.appicon_default);
		
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
		   nfe.printStackTrace();
		} 	
	}
	
	private class HttpTask extends ProtocolDownloadTask {
		private AppDetailParser mDataProxy = new AppDetailParser();
		private String mAppCode = null;
		public HttpTask(Handler handler, String appCode) {
			super(handler);
			mAppCode = appCode;
		}
		
		public void cancel() {
			mDataProxy.cancel();
		}
		
		protected boolean doTask() {
			try {
				String url = UrlHelpers.getAppDetailUrl(
						TokenWrapper.getToken(AppDetailsPage.this), mAppCode);
				
				AppInfo appInfo = mDataProxy.getAppInfo(url);
				if (null != appInfo) {
					mAppInfo = appInfo;
					return true;
				}
	    	} catch (Throwable tr) {
	    		tr.printStackTrace();
	    	} 
	    	return false;
		}
    }
}
