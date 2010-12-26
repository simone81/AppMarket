package net.behoo.appmarket;

import net.behoo.appmarket.downloadinstall.DownloadInstallService;

import behoo.sync.ISyncService;

public class ServiceManager {
	//private static final String TAG = "ServiceManager";
	
	private ISyncService mSyncService = null;
	private DownloadInstallService mInstallService = null;
    
	private static ServiceManager mServiceManager = null;
	
	public static ServiceManager inst() {
		if (null == mServiceManager) {
			mServiceManager = new ServiceManager();
		}
		return mServiceManager;
	}
	
	public boolean isSucceed() {
		return (null != mSyncService && null != mInstallService);
	}
	
	public void setSyncHandler(ISyncService handler) {
		mSyncService = handler;
	}
	
	public ISyncService getSyncHandler() {
		return mSyncService;
	}
	
	public void setDownloadHandler(DownloadInstallService handler) {
		mInstallService = handler;
	}
	
	public DownloadInstallService getDownloadHandler() {
		return mInstallService;
	}
}
