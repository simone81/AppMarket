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
	
	private static final String TAG = "DownloadInstallService";
	
	private final IBinder mBinder = new LocalServiceBinder();
	
	private ArrayList<PackageInfo> mPackageInfo = null;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			// update the local database
			Bundle bundle = intent.getExtras();
			//String uri = bundle.getCharSequence(DownloadReceiver.DOWNLOAD_DATA_URI).toString();
			String urlExtra = bundle.getCharSequence(Downloads.COLUMN_NOTIFICATION_EXTRAS).toString();
			Log.i(TAG, "onReceive uri: "+" "+urlExtra);
			PackageInfo info = null;
			for( int i = 0; i < mPackageInfo.size(); ++i ) {
				info = mPackageInfo.get(i);
				if( info.mDownloadUri.toString().compareTo(urlExtra) == 0 ) {
					info.mState = Constants.PackageState.download_succeeded;
					info.sendPackageStateBroadcast(context);
					// get the file name from database
					Cursor c = context.getContentResolver().query(Downloads.CONTENT_URI, 
							new String [] {Downloads._DATA, Downloads.COLUMN_DESCRIPTION}, 
							null, null, null);
					Assert.assertNotNull(c);
					
					int filenameId = c.getColumnIndexOrThrow(Downloads._DATA);
					int descriptionId = c.getColumnIndexOrThrow(Downloads.COLUMN_DESCRIPTION);
					for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
						if (0 == c.getString(descriptionId).compareTo(urlExtra)) {
							String filename = c.getString(filenameId);
							info.mInstallUri = Uri.parse("file://"+filename);
							Log.i(TAG, "The downloaded file: "+info.mInstallUri.toString());
							break;
						}
					}
					c.close();
					break;
				}
			}
			Assert.assertNotNull(info);
			Assert.assertNotNull(info.mInstallUri);
			// fetch the package information from content provider
			InstallingThread thrd = new InstallingThread(context, info);
			thrd.start();
		}
	};
	
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
		IntentFilter filter = new IntentFilter(DownloadReceiver.DOWNLOAD_COMPLETED);
		this.registerReceiver(mReceiver, filter);
	}
	
	
	public void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(mReceiver);
	}
	
	
	public int onStartCommand(Intent intent, int flags, int startId) {
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}
	
	public boolean downloadAndInstall(String url, String mimetype,
            String contentDisposition, long contentLength ) {
		
		String filename = URLUtil.guessFileName(url,contentDisposition, mimetype);
		Log.i(TAG, "downloadAndInstall guess file: "+filename);
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
            throw new IllegalArgumentException();
        }
        
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
        Uri theUri = Uri.parse( uri.toString() );
        Log.i(TAG, "downloadAndInstall the uri: "+uri.toString());
        ContentValues values = new ContentValues();
        values.put(Downloads.COLUMN_URI, uri.toString());
        values.put(Downloads.COLUMN_NOTIFICATION_PACKAGE, this.getPackageName());
        values.put(Downloads.COLUMN_NOTIFICATION_CLASS, DownloadReceiver.class.getCanonicalName());
        values.put(Downloads.COLUMN_VISIBILITY, Downloads.VISIBILITY_HIDDEN);
        values.put(Downloads.COLUMN_MIME_TYPE, mimetype);
        values.put(Downloads.COLUMN_FILE_NAME_HINT, theUri.toString());
        values.put(Downloads.COLUMN_DESCRIPTION, theUri.toString());
        values.put(Downloads.COLUMN_NOTIFICATION_EXTRAS, theUri.toString());
        values.put(Downloads.COLUMN_DESTINATION, Downloads.DESTINATION_CACHE_PARTITION_PURGEABLE);
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
		InstallingThread thrd = new InstallingThread(this, pkgInfo);
		thrd.start();
		return true;
	}
}
