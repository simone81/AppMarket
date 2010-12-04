package net.behoo.appmarket.downloadinstall;


import java.net.URI;
import java.util.ArrayList;

import junit.framework.Assert;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.webkit.URLUtil;
import android.net.WebAddress;
import android.provider.Downloads;

public class DownloadInstallService extends Service {
	
	private static final String TAG = "PackageInstallerService";
	
	private final IBinder mBinder = new LocalServiceBinder();
	
	private Object mSyncObject = new Object();
	private ArrayList<PackageInfo> mPackageInfo = null;
	
	// download completed broadcast receiver
	private DownloadBroadcastReceiver mBroadcastReceiver = new DownloadBroadcastReceiver();
	
	public class LocalServiceBinder extends Binder {
		public DownloadInstallService getService() {
            return DownloadInstallService.this;
        }
    }
	
	
	public IBinder onBind( Intent intent ) {
		Log.i(TAG, "onBind");
		return mBinder;
	}
	
	
	public boolean onUnbind( Intent intent ) {
		Log.i(TAG, "onUnbind");
		return super.onUnbind( intent );
	}
	
	
	public void onCreate() {
		super.onCreate();
		mPackageInfo = new ArrayList<PackageInfo>();
		this.registerReceiver( mBroadcastReceiver, new IntentFilter(Downloads.ACTION_DOWNLOAD_COMPLETED) );
	}
	
	
	public void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver( mBroadcastReceiver );
		Log.i(TAG, "onDestroy");
	}
	
	
	public int onStartCommand(Intent intent, int flags, int startId) {
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}
	
	public boolean downloadAndInstall(String url, String mimetype,
            String contentDisposition, long contentLength ) {
		
		String filename = URLUtil.guessFileName(url,contentDisposition, mimetype);
		Log.i(TAG, "downloadAndInstall file: "+filename);
		URI uri = null;
        try {
            // Undo the percent-encoding that KURL may have done.
            String newUrl = new String(URLUtil.decode(url.getBytes()));
            // Parse the url into pieces
            WebAddress w = new WebAddress(newUrl);
            String frag = null;
            String query = null;
            String path = w.mPath;
            // Break the path into path, query, and fragment
            if (path.length() > 0) {
                // Strip the fragment
                int idx = path.lastIndexOf('#');
                if (idx != -1) {
                    frag = path.substring(idx + 1);
                    path = path.substring(0, idx);
                }
                idx = path.lastIndexOf('?');
                if (idx != -1) {
                    query = path.substring(idx + 1);
                    path = path.substring(0, idx);
                }
            }
            uri = new URI(w.mScheme, w.mAuthInfo, w.mHost, w.mPort, path,
                    query, frag);
        } catch (Exception e) {
            Log.e(TAG, "Could not parse url for download: " + url, e);
            throw new IllegalArgumentException();// tbd
        }
        
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
        Uri theUri = Uri.parse( uri.toString() );
        Log.i(TAG, "downloadAndInstall the uri: "+uri.toString());
        ContentValues values = new ContentValues();
        values.put(Downloads.COLUMN_URI, uri.toString() );
        values.put(Downloads.COLUMN_NOTIFICATION_PACKAGE, this.getPackageName());
        values.put(Downloads.COLUMN_NOTIFICATION_CLASS, DownloadInstallService.class.getCanonicalName());
        values.put(Downloads.COLUMN_VISIBILITY, Downloads.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        values.put(Downloads.COLUMN_MIME_TYPE, mimetype);
        values.put(Downloads.COLUMN_FILE_NAME_HINT, filename);
        values.put(Downloads.COLUMN_DESCRIPTION, uri.getHost());
        values.put(Downloads.COLUMN_NOTIFICATION_EXTRAS, theUri.toString());
        if (contentLength > 0) {
            values.put(Downloads.COLUMN_TOTAL_BYTES, contentLength);
        }
        if (mimetype != null) {
        	PackageInfo info = new PackageInfo( theUri, null, 0, Constants.PackageState.downloading );
            mPackageInfo.add( info );
            info.sendPackageStateBroadcast(this);
            getContentResolver().insert(Downloads.CONTENT_URI, values);
        	return true;
        } else {
        	Log.e(TAG, "you must supply mimetype.");
            return false;
        }
	}
	
	
	public boolean install( String uri ) {
		PackageInfo pkgInfo = new PackageInfo( Uri.parse("local"), Uri.parse( uri ), 
				0, Constants.PackageState.unknown );
		InstallingThread thrd = new InstallingThread(this, mSyncObject, pkgInfo);
		thrd.start();
		return true;
	}
	
	private class DownloadBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			// update the local database
			Bundle bundle = intent.getExtras();
			String url = bundle.getCharSequence( Downloads.COLUMN_NOTIFICATION_EXTRAS ).toString();
			PackageInfo info = null;
			for( int i = 0; i < mPackageInfo.size(); ++i ) {
				info = mPackageInfo.get( i );
				if( info.mDownloadUri.toString().compareTo( url ) == 0 ) {
					info.mState = Constants.PackageState.download_succeeded;
					info.sendPackageStateBroadcast(DownloadInstallService.this);
					// get the file name from database
					Cursor c = getContentResolver().query(Downloads.CONTENT_URI, 
							new String [] { Downloads.COLUMN_URI, Downloads._DATA }, 
							null, null, null);
					Assert.assertNotNull(c);
					Uri uri = intent.getData();
					int uriId = c.getColumnIndexOrThrow(Downloads.COLUMN_URI);
					int filenameId = c.getColumnIndexOrThrow(Downloads._DATA);
					for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
						if (0 == c.getString(uriId).compareTo(uri.toString())) {
							String filename = c.getString(filenameId);
							info.mInstallUri = Uri.parse("file:///cache/"+filename);
							Log.i(TAG, "The downloaded file: "+info.mInstallUri.toString());
							break;
						}
					}
					c.close();
					break;
				}
			}
			Log.i(TAG, "DownloadBroadcastReceiver, finished of "+url);
			Assert.assertNotNull(info);
			Assert.assertNotNull(info.mInstallUri);
			// fetch the package information from content provider
			InstallingThread thrd = new InstallingThread(DownloadInstallService.this, mSyncObject, info);
			thrd.start();
		}
	}
}
