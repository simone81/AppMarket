package net.behoo.AppMarket;

import InstallAppProgress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import PackageInstallerActivity.ClearCacheReceiver;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser.Package;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

public class PackageInstaller {
	
	private static final String TAG = "PackageInstaller";
	private static final String TMP_INSTALL_FILE_NAME="tmpInstallCopy.apk";
	
	private Context mContext = null;
	private PackageManager mPkgMgr = null;
	private PackageParser.Package mPkgInfo = null;
	// ApplicationInfo object primarily used for already existing applications
    private ApplicationInfo mAppInfo = null;
    private boolean mInstallingFinished = false;
    private boolean mFreeingStorageFinished = false;
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case INSTALL_COMPLETE:
                	mInstallingFinished = true;
                    if(msg.arg1 == SUCCEEDED) {
                    	
                    }
                    else{
                    	
                    }
                    break;
                default:
                    break;
            }
        }
    };
    
	public PackageInstaller(Context c, PackageManager pm) {
		mContext = c;
		mPkgMgr = pm;
	}
	
	public void installPackage( Uri pkgURI ) {
		mPkgInfo = PackageUtil.getPackageInfo( pkgURI );
		
		if(mPkgInfo == null) {
			IllegalArgumentException e = new IllegalArgumentException();
            throw e;
        }
		
		//check setting. how to decide the source of the app. from behoo or ? 
		// check permissions tbd
//        if(!isInstallingUnknownAppsAllowed()) {
//            //ask user to enable setting first
//            // 
//            return;
//        }
		
		//compute the size of the application. just an estimate
        long size;
        String apkPath = pkgURI.getPath();
        File apkFile = new File( apkPath );
        size = 4*apkFile.length();
        checkOutOfSpace(size);
		
		makeTempCopyAndInstall( pkgURI.getPath() );
	}
	
	private void checkOutOfSpace(long size) {
        Log.i(TAG, "Checking for "+size+" number of bytes");  
        
        FreeStorageObserver observer = new FreeStorageObserver();
        mPkgMgr.freeStorageAndNotify( size, observer );
        // need better way to wait on finishing
        while( !mFreeingStorageFinished )
        	Thread.sleep( 1000 );
    }
	
	private void makeTempCopyAndInstall(String filePath) {
        // Check if package is already installed. display confirmation dialog if replacing pkg
        try {
            mAppInfo = mPkgMgr.getApplicationInfo( mPkgInfo.packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES );
        } catch (NameNotFoundException e) {
            mAppInfo = null;
        }
        
        if (mAppInfo == null) {
        	Log.i(TAG, "start install:" + mPkgInfo.applicationInfo.packageName);
        	// 
        	File tmpPackageFile  = mContext.getFileStreamPath( TMP_INSTALL_FILE_NAME );
        	if (tmpPackageFile == null) {
                Log.w(TAG, "Failed to create temp file");
                throw new Exception();/// tbd self-defined exception
            }
        	if (tmpPackageFile.exists()) {
                tmpPackageFile.delete();
            }
        	// Open file to make it world readable
            FileOutputStream fos;
            try {
                fos = mContext.openFileOutput( TMP_INSTALL_FILE_NAME, Context.MODE_WORLD_READABLE );
            } catch (FileNotFoundException e1) {
                Log.e(TAG, "Error opening file " + TMP_INSTALL_FILE_NAME);
                return;
            }
            
            try {
                fos.close();
            } catch (IOException e) {
                Log.e(TAG, "Error opening file " + TMP_INSTALL_FILE_NAME);
                return;
            }

            File srcPackageFile = new File( filePath );
            if (!FileUtils.copyFile(srcPackageFile, tmpPackageFile)) {
                Log.w(TAG, "Failed to make copy of file: " + srcPackageFile);
                return;
            }
            
            Uri pkgURI = Uri.parse("file://" + tmpPackageFile.getPath());
            PackageInstallObserver observer = new PackageInstallObserver();
            mPkgMgr.installPackage(pkgURI, observer, 0, "my_test_app");
            // bad ! tbd some other methods
            while( !mInstallingFinished ) {
            	Thread.sleep( 1000 );
            }           
        } else {
            Log.i(TAG, "Replacing existing package:"+mPkgInfo.applicationInfo.packageName);
        }
    }
	
	private boolean isInstallingUnknownAppsAllowed() {
        return Settings.Secure.getInt( mContext.getContentResolver(), 
            Settings.Secure.INSTALL_NON_MARKET_APPS, 0) > 0;
    }
	
	class FreeStorageObserver extends IPackageDataObserver.Stub {
		@Override
		public void onRemoveCompleted(String pkgname, boolean success){
			mFreeingStorageFinished = true;
		}
	}
	
	class PackageInstallObserver extends IPackageInstallObserver.Stub {
        public void packageInstalled(String packageName, int returnCode) {
            Message msg = mHandler.obtainMessage(INSTALL_COMPLETE);
            msg.arg1 = returnCode;
            mHandler.sendMessage(msg);
        }
    }
}
