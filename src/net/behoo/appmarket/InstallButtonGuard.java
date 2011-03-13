package net.behoo.appmarket;

import behoo.providers.InstalledAppDb;
import behoo.providers.InstalledAppDb.PackageState;
import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import net.behoo.appmarket.http.UrlHelpers;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class InstallButtonGuard implements OnClickListener {
	private static final String TAG = "InstallButtonGuard";
	
	private Context mContext = null;
	private Button mButton = null;
	private AppInfo mAppInfo = null;
	private DownloadInstallService mInstallService = null;
    private PackageState mAppState = PackageState.unknown;
    private OnInstallClickListener mListener = null;
    
    private ServiceConnection mServiceConn = new ServiceConnection() {
    	public void onServiceConnected(ComponentName cname, IBinder binder){
    		mInstallService =
    				((DownloadInstallService.LocalServiceBinder)binder).getService();
    		updateAppState();
    	}
    	
    	public void onServiceDisconnected(ComponentName cname){
    		mInstallService = null;
    	}
    };
    
    private BroadcastReceiver mDownloadReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			try {
				String appCode = intent.getStringExtra(Constants.PACKAGE_CODE);
				String state = intent.getStringExtra(Constants.PACKAGE_STATE);
				if (null != mAppInfo && appCode.equals(mAppInfo.mAppCode)) {
					mAppState = PackageState.valueOf(state);
					mButton.setVisibility(View.VISIBLE);
					updateButtonState();
				}
			} catch (Throwable tr) {
				tr.printStackTrace();
			}
		}
	};
	
	public InstallButtonGuard(Context context, Button button, AppInfo appInfo) {
		if (null == context
			|| null == button) {
			throw new NullPointerException();
		}
		
		mContext = context;
		mButton = button;
		mAppInfo = appInfo;
		mButton.setOnClickListener(this);
	}
	
	public void disableGuard() {
		mContext.unregisterReceiver(mDownloadReceiver);
		mContext.unbindService(mServiceConn);
	}
	
	public void enableGuard() {
		Intent intent = new Intent(mContext, DownloadInstallService.class);
		mContext.bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
		
		IntentFilter filter = new IntentFilter(Constants.ACTION_PKG_STATE_CHANGED);
		mContext.registerReceiver(mDownloadReceiver, filter);
	}
	
	public void setOnInstallClickListener(OnInstallClickListener listener ) {
		mListener = listener;
	}
	
	public void setAppInfo(AppInfo appInfo) {
		if (null == mAppInfo
			|| !mAppInfo.equalsAppCode(appInfo)) {
			mAppInfo = appInfo;
		}
		updateAppState();
	}
	
	public AppInfo getAppInfo() {
		return mAppInfo;
	}
	
	public InstalledAppDb.PackageState getPackageState() {
		return mAppState;
	}
	
	public void onClick(View v) {
		if (null == mInstallService) {
			return ;
		}
		
		switch (mAppState) {
		case unknown:	
		case need_update:	
		case install_failed:	
		case download_failed:
			try {
				String url = UrlHelpers.getDownloadUrl(
						TokenWrapper.getToken(mContext), mAppInfo.mAppCode);
				
				Intent i = new Intent();
				i.setClass(mContext, DownloadInstallService.class);
				i.setAction(DownloadInstallService.ACTION_INSTALL_APP);
				i.putExtra(DownloadInstallService.EXTRA_HTTP_URL, url);
				i.putExtra(DownloadInstallService.EXTRA_MIME, AppInfo.MIMETYPE);
				i.putExtra(DownloadInstallService.EXTRA_APP_CODE, mAppInfo.mAppCode);
				i.putExtra(DownloadInstallService.EXTRA_APP_VERSION, mAppInfo.mAppVersion);
				i.putExtra(DownloadInstallService.EXTRA_APP_NAME, mAppInfo.mAppName);
				i.putExtra(DownloadInstallService.EXTRA_APP_IMAGE_URL, mAppInfo.mAppImageUrl);
				i.putExtra(DownloadInstallService.EXTRA_APP_AUTHOR, mAppInfo.mAppAuthor);
				i.putExtra(DownloadInstallService.EXTRA_APP_SHORTDESC, mAppInfo.mAppShortDesc);
				mContext.startService(i);
				mListener.onInstallClicked(mAppInfo);
			} catch (Throwable tr) {
				tr.printStackTrace();
			}
			break;
		case install_succeeded:	
			// get the package name
			// get the launch intent
			// start the activity
			break;
		case downloading:
		case download_succeeded:
		case installing:
		default:
			Log.e(TAG, "the button under these states should not be clicked");
			break;
		}
	}
	
	public void updateAppState() {
		if (null == mInstallService) {
			return ;
		}
		
		if (null != mAppInfo) {
			mAppState = mInstallService.getAppState(mAppInfo.mAppCode);
			mButton.setVisibility(View.VISIBLE);
			updateButtonState();
		}
		else {
			mButton.setVisibility(View.INVISIBLE);
		}
	}
	
	private void updateButtonState() {
		switch(mAppState) {
		case unknown:
			mButton.setText(R.string.downloadpage_to_install);
			mButton.setEnabled(true);
			break;
		case downloading:
			mButton.setText(R.string.downloadpage_downloading);
			mButton.setEnabled(false);
			break;
		case download_succeeded:
			mButton.setText(R.string.downloadpage_download_success);
			mButton.setEnabled(false);
			break;
		case installing:
			mButton.setText(R.string.downloadpage_installing);
			mButton.setEnabled(false);
			break;
		case install_succeeded:
			mButton.setText(R.string.downloadpage_install_success);
			mButton.setEnabled(true);
			break;
		case need_update:
			mButton.setText(R.string.downloadpage_update);
			mButton.setEnabled(true);
			break;
		case install_failed:
		case download_failed:	
			mButton.setText(R.string.downloadpage_retry);
			mButton.setEnabled(true);
			break;		
		default:
			Log.w(TAG, "updateButtonState unspecified app state");
			break;
		}
	}
	
	interface OnInstallClickListener {
		abstract void onInstallClicked(AppInfo appInfo) ;
	}
}
