package net.behoo.appmarket;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import net.behoo.appmarket.downloadinstall.Constants.PackageState;
import net.behoo.appmarket.http.UrlHelpers;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class InstallButtonGuard implements OnClickListener {
	private static final String TAG = "InstallButtonGuard";
	
	private Button mButton = null;
	private AppInfo mAppInfo = null;
	private DownloadInstallService mService = null;
    private PackageState mAppState = Constants.PackageState.unknown;
    private OnInstallListener mListener = null;
    
	public InstallButtonGuard(Button button, AppInfo appInfo, DownloadInstallService service) {
		if (null == button || null == appInfo || null == service)
			throw new NullPointerException();
		
		mButton = button;
		mAppInfo = appInfo;
		mService = service;
		
		mButton.setOnClickListener(this);
		
		// set the button state according to the application state
		updateAppState();
	}
	
	public void setOnInstallListener(OnInstallListener listener ) {
		mListener = listener;
	}
	
	public void setAppInfo(AppInfo appInfo) {
		if (mAppInfo.mAppCode.compareTo(appInfo.mAppCode) != 0) {
			mAppInfo = appInfo;
			updateButtonState();
		}
	}
	
	public PackageState getPackageState() {
		return mAppState;
	}
	
	public void onClick(View v) {
		switch (mAppState) {
		case unknown:
		case uninstalled:	
		case need_update:	
		case install_failed:	
		case download_failed:	
			mService.downloadAndInstall(UrlHelpers.getDownloadUrl("token", mAppInfo.mAppChangelog),
					AppInfo.MIMETYPE, mAppInfo);
			if (null != mListener) {
				mListener.onInstalled(mAppInfo);
			}
			break;
		case downloading:
		case download_succeeded:
		case installing:
		case install_succeeded:	
		default:
			Log.e(TAG, "the button under these states should be clicked");
			break;
		}
	}
	
	public void updateAppState() {
		mAppState = mService.getAppState(mAppInfo.mAppCode);
		updateButtonState();
	}
	
	private void updateButtonState() {
		switch(mAppState) {
		case unknown:
		case uninstalled:	
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
			mButton.setEnabled(false);
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
	
	interface OnInstallListener {
		abstract void onInstalled(AppInfo appInfo) ;
	}
}
