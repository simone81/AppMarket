package net.behoo.appmarket;

import net.behoo.appmarket.data.AppInfo;
import net.behoo.appmarket.downloadinstall.Constants;
import net.behoo.appmarket.downloadinstall.DownloadInstallService;
import net.behoo.appmarket.downloadinstall.Constants.PackageState;
import net.behoo.appmarket.http.UrlHelpers;
import android.os.RemoteException;
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
    private OnInstallClickListener mListener = null;
    
	public InstallButtonGuard(Button button, AppInfo appInfo, DownloadInstallService service) {
		if (null == button || null == service)
			throw new NullPointerException();
		
		mButton = button;
		mAppInfo = appInfo;
		mService = service;
		
		mButton.setOnClickListener(this);
		
		// set the button state according to the application state
		updateAppState();
	}
	
	public void setOnInstallClickListener(OnInstallClickListener listener ) {
		mListener = listener;
	}
	
	public void setAppInfo(AppInfo appInfo) {
		if (null == mAppInfo
				|| mAppInfo.mAppCode.compareTo(appInfo.mAppCode) != 0) {
			mAppInfo = appInfo;
			updateAppState();
		}
	}
	
	public AppInfo getAppInfo() {
		return mAppInfo;
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
			try {
				String url = UrlHelpers.getDownloadUrl(
						ServiceManager.inst().getSyncHandler().getToken(), 
						mAppInfo.mAppCode);
				mService.downloadAndInstall(url, AppInfo.MIMETYPE, mAppInfo);
				if (null != mListener) {
					mListener.onInstallClicked(mAppInfo);
				}
			} catch (RemoteException e) {
				Log.i(TAG, "onClick install "+e.getLocalizedMessage());
			}
			break;
		case install_succeeded:	
			// tbd uninstall
			mService.uninstall(mAppInfo.mAppCode);
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
			mAppState = mService.getAppState(mAppInfo.mAppCode);
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
			// tbd
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
