package net.behoo.appmarket.downloadinstall;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
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
import android.os.FileUtils;

public class PackageInstaller {
	
	private static final String TAG = "PackageInstaller";
	private static final String TMP_INSTALL_FILE_NAME="tmpInstallCopy.apk";
	private static final int SUCCEEDED = 1;
    private static final int FAILED = 0;
    
	private Context mContext = null;
	private PackageManager mPkgMgr = null;
	private PackageParser.Package mPkgInfo = null;
	
	// ApplicationInfo object primarily used for already existing applications
    private ApplicationInfo mAppInfo = null;
    
    private boolean mInstallingFinished = false;
    private Object mInstallingSyncObject = new Object();
    private boolean mInstallingSuccess = false;
    
    private boolean mFreeingStorageFinished = false;
    private Object mFreeingStorageSyncObject = new Object();
    
    private Object mUninstallSyncObject = new Object();
    private boolean mUninstallFinished = false;
    private boolean mUninstallSuccess = false;
    
	public PackageInstaller(Context c) {
		mContext = c;
		mPkgMgr = c.getPackageManager();
	}
	
	// the source file uri
	public boolean installPackage(Uri pkgURI) {
		mAppInfo = null;
		mPkgInfo = PackageUtils.getPackageInfo(pkgURI);
		if(mPkgInfo == null) {
			Log.i(TAG, "installPackage invalid package");
            return false;
        }
		Log.i(TAG, "installPackage pkg info: "+mPkgInfo.mPath);
		//compute the size of the application. just an estimate
        checkOutOfSpace( pkgURI.getPath() );
		
		return makeTempCopyAndInstall( pkgURI.getPath() );
	}
	
	public boolean uninstallPackage(String pkgName) {
		mAppInfo = null;
		try {	 
			mAppInfo = mPkgMgr.getApplicationInfo(
				pkgName, PackageManager.GET_UNINSTALLED_PACKAGES);
		} catch (NameNotFoundException e) {
		}
		
		if (null != mAppInfo) {
			// wait unitl the freeing process finished
	        boolean bWait = true;
	        while (bWait){
	        	try {
	        		synchronized (mUninstallSyncObject) {
	                	if(!mUninstallFinished) {
	                		mUninstallSyncObject.wait();
	                	}
	                	bWait = false;
	                } 
	    		} catch ( InterruptedException e ) {
	    			bWait = true;
	    		}
	        }
		}
		else {
			Log.i(TAG, "uninstallPackage: pkg "+pkgName+" is not installed yet");
			mUninstallSuccess = true;
		}
		return mUninstallSuccess;
	}
	
	private void checkOutOfSpace( String apkPath ) {
		File apkFile = new File( apkPath );
		long size = 4*apkFile.length();
        Log.i(TAG, "Checking for "+size+" number of bytes");  
        
        FreeStorageObserver observer = new FreeStorageObserver();
        mPkgMgr.freeStorageAndNotify( size, observer );
        
        // wait unitl the freeing process finished
        boolean bWait = true;
        while( bWait ){
        	try {
        		synchronized ( mFreeingStorageSyncObject ) {
                	if( !mFreeingStorageFinished ) {
                		mFreeingStorageSyncObject.wait();
                	}
                	bWait = false;
                } 
    		} catch ( InterruptedException e ) {
    			bWait = true;
    		}
        }

        Log.i(TAG, "checkOutOfSpace procedure has finished!");
    }
	
	private boolean makeTempCopyAndInstall(String filePath) {
        // Check if package is already installed. display confirmation dialog if replacing pkg
        try {
            mAppInfo = mPkgMgr.getApplicationInfo( mPkgInfo.packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES );
        } catch (NameNotFoundException e) {
            mAppInfo = null;
        }
        
        int installFlags = 0;
        if (null != mAppInfo) {
        	Log.i(TAG, "Replacing existing package:"+mPkgInfo.applicationInfo.packageName);
        	installFlags |= PackageManager.INSTALL_REPLACE_EXISTING;
        }
        
    	Log.i(TAG, "install package: "+mPkgInfo.applicationInfo.packageName);
    	// create the temp file
    	File tmpPackageFile  = mContext.getFileStreamPath( TMP_INSTALL_FILE_NAME );
    	if (tmpPackageFile == null) {
            Log.w(TAG, "Failed to create temp file");
            return false;
        }
    	Log.i(TAG, "makeTempCopyAndInstall tmp file: "+tmpPackageFile.getAbsolutePath());
    	if (tmpPackageFile.exists()) {
            tmpPackageFile.delete();
        }
    	// Open file to make it world readable
        FileOutputStream fos;
        try {
            fos = mContext.openFileOutput( TMP_INSTALL_FILE_NAME, Context.MODE_WORLD_READABLE );
        } catch (FileNotFoundException e1) {
            Log.e(TAG, "Error opening file " + TMP_INSTALL_FILE_NAME);
            //throw e1;/// tbd self-defined exception
            return false;
        }
        
        try {
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error close file " + TMP_INSTALL_FILE_NAME);
            //throw e;/// tbd self-defined exception
            return false;
        }

        // copy the file from source to temp
        File srcPackageFile = new File( filePath );
        Log.i(TAG, "makeTempCopyAndInstall the source file: "+srcPackageFile.getAbsolutePath());
        if (!FileUtils.copyFile(srcPackageFile, tmpPackageFile)) {
            Log.w(TAG, "Failed to make copy of file: " + srcPackageFile);
            //throw new NullPointerException();/// tbd self-defined exception
            return false;
        }
        
        Uri pkgURI = Uri.parse("file://" + tmpPackageFile.getPath());
        PackageInstallObserver observer = new PackageInstallObserver();
        mPkgMgr.installPackage(pkgURI, observer, installFlags, "net.behoo.appmarket");
       
        // wait until the installing process finished
        boolean bWait = true;
        while( bWait ){
        	try {
        		synchronized ( mInstallingSyncObject ) {
                	if ( !mInstallingFinished ){
                		mInstallingSyncObject.wait();
                	}
                	bWait = false;
                }
        	} catch ( InterruptedException e ) {
        		bWait = true;
    		}
        }
        
        // delte the temp file
        if( tmpPackageFile.exists() ) {
        	tmpPackageFile.delete();
        }
        
        return mInstallingSuccess;
    }
	
	private boolean isInstallingUnknownAppsAllowed() {
        return Settings.Secure.getInt( mContext.getContentResolver(), 
            Settings.Secure.INSTALL_NON_MARKET_APPS, 0) > 0;
    }
	
	class FreeStorageObserver extends IPackageDataObserver.Stub {	
		public void onRemoveCompleted(String pkgname, boolean success){
			synchronized ( mFreeingStorageSyncObject ) {
				mFreeingStorageFinished = true;
	        	mFreeingStorageSyncObject.notify();
			}
		}
	}
	
	class PackageInstallObserver extends IPackageInstallObserver.Stub {
        public void packageInstalled(String packageName, int returnCode) {
        	synchronized ( mInstallingSyncObject ) {
        		mInstallingFinished = true;
        		mInstallingSuccess = ( returnCode == SUCCEEDED );
            	mInstallingSyncObject.notify();
            }
        	Log.i(TAG, "PackageInstallObserver installed");
        }
    }
	
	class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
         public void packageDeleted(boolean succeeded) {
        	 synchronized (mUninstallSyncObject) {
        		 mUninstallFinished = true;
        		 mUninstallSuccess = succeeded;
        		 mUninstallSyncObject.notify();
        	 }
         }
     }
}
