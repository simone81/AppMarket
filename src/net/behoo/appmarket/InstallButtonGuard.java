package net.behoo.appmarket;

import behoo.providers.InstalledAppDb;
import behoo.providers.InstalledAppDb.PackageState;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import net.behoo.appmarket.http.UrlHelpers;
import net.behoo.appmarket.database.PkgsProviderWrapper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class InstallButtonGuard implements OnClickListener {
	private static final String TAG = "InstallButtonGuard";
	
	private Context mContext = null;
	private Button mButton = null;
	private AppInfo mAppInfo = null;
    private PackageState mAppState = PackageState.unknown;
    private OnInstallClickListener mListener = null;
    
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
		updateAppState();
	}
	
	public void disableGuard() {
		mContext.unregisterReceiver(mDownloadReceiver);
	}
	
	public void enableGuard() {	
		IntentFilter filter = new IntentFilter(Constants.ACTION_PKG_STATE_CHANGED);
		mContext.registerReceiver(mDownloadReceiver, filter);
		updateAppState();
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
			try {
				String pkgName = PkgsProviderWrapper.getAppPkgName(mContext, mAppInfo.mAppCode);
				PackageManager pm = mContext.getPackageManager();
				Intent intent = pm.getLaunchIntentForPackage(pkgName);
				mContext.startActivity(intent);
			} catch (Throwable tr) {
				tr.printStackTrace();
			}
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
		if (null != mAppInfo) {
			mAppState = PkgsProviderWrapper.getAppState(mContext, mAppInfo.mAppCode);
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
