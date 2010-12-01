package net.behoo.DownloadInstall;


import java.net.URI;
import java.util.ArrayList;


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
	
	@Override
	public IBinder onBind( Intent intent ) {
		Log.i(TAG, "onBind");
		return mBinder;
	}
	
	@Override
	public boolean onUnbind( Intent intent ) {
		Log.i(TAG, "onUnbind");
		return super.onUnbind( intent );
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mPackageInfo = new ArrayList<PackageInfo>();
		this.registerReceiver( mBroadcastReceiver, new IntentFilter(Downloads.ACTION_DOWNLOAD_COMPLETED) );
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver( mBroadcastReceiver );
		Log.i(TAG, "onDestroy");
	}
	
	@Override
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
            return false;
        }
        
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
        Log.i(TAG, "downloadAndInstall the uri: "+uri.toString());
        ContentValues values = new ContentValues();
        values.put(Downloads.COLUMN_URI, uri.toString() );
        values.put(Downloads.COLUMN_USER_AGENT, "");
        values.put(Downloads.COLUMN_NOTIFICATION_PACKAGE, this.getPackageName());
        values.put(Downloads.COLUMN_NOTIFICATION_CLASS, DownloadInstallService.class.getCanonicalName());
        values.put(Downloads.COLUMN_VISIBILITY, Downloads.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        values.put(Downloads.COLUMN_MIME_TYPE, mimetype);
        values.put(Downloads.COLUMN_FILE_NAME_HINT, filename);
        values.put(Downloads.COLUMN_DESCRIPTION, uri.getHost());
        values.put(Downloads.COLUMN_NOTIFICATION_EXTRAS, url);
        if (contentLength > 0) {
            values.put(Downloads.COLUMN_TOTAL_BYTES, contentLength);
        }
        if (mimetype != null) {
        	PackageInfo info = new PackageInfo( url, 0, Constants.PackageState.downloading );
            mPackageInfo.add( info );
            
            final Uri contentUri = getContentResolver().insert(Downloads.CONTENT_URI, values);
            
//            DownloadAndInstallInfo info = new DownloadAndInstallInfo();
//        	info.mCursor = getContentResolver().query(Downloads.CONTENT_URI, 
//                new String [] {"_id", Downloads.COLUMN_TITLE, Downloads.COLUMN_STATUS,
//                Downloads.COLUMN_TOTAL_BYTES, Downloads.COLUMN_CURRENT_BYTES, 
//                Downloads._DATA, Downloads.COLUMN_DESCRIPTION, 
//                Downloads.COLUMN_MIME_TYPE, Downloads.COLUMN_LAST_MODIFICATION,
//                Downloads.COLUMN_VISIBILITY}, 
//                null, null, null );
//        	info.mObserver = new DownloadObserver( mDownloadAndInstall.size() );
//        	info.mCursor.registerContentObserver( info.mObserver );
//        	mDownloadAndInstall.add( info );
        	return true;
        } else {
        	Log.e(TAG, "you must supply mimetype.");
            return false;
        }
	}
	
	public boolean install( String uri ) {
		Uri u = Uri.parse( uri );
		// fetch the package information from content provider
		PackageInfo pkgInfo = new PackageInfo( u.toString(), 0, Constants.PackageState.unknown );
		InstallingThread thrd = new InstallingThread(
				this, 
				mSyncObject, 
				pkgInfo);
		thrd.start();
	}
/*	
	private class DownloadObserver extends ContentObserver {
		private int mCursorIndex = -1;
		public DownloadObserver( int cursorIndex ) {
			super( new Handler() );
			
			mCursorIndex = cursorIndex;
		}
		@Override
		public boolean deliverSelfNotifications() {
			return true;
		}
		
		@Override
		public void onChange( boolean selfChange ) {
			
		}
	}
*/	

	private class DownloadBroadcastReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			// update the local database
			Uri uri = intent.getData();
			Bundle bundle = intent.getExtras();
			String url = bundle.getCharSequence( Downloads.COLUMN_NOTIFICATION_EXTRAS ).toString();
			PackageInfo info = null;
			for( int i = 0; i < mPackageInfo.size(); ++i ) {
				info = mPackageInfo.get( i );
				if( info.mUrl.compareTo( url ) == 0 ) {
					info.mState = Constants.PackageState.installing;
					break;
				}
			}
			Log.i(TAG, "DownloadBroadcastReceiver, finished of "+url);
			
			// broadcast
			Intent i = new Intent(Constants.ACTION_STATE);
			i.putExtra(Constants.PKG_ID, 123);
			i.putExtra(Constants.PAK_URI, uri);
			i.putExtra(Constants.DOWNLOAD_STAGE, true);
			DownloadInstallService.this.sendBroadcast(i);
			
			// fetch the package information from content provider
			InstallingThread thrd = new InstallingThread(
					(Context)DownloadInstallService.this, 
					mSyncObject, 
					info);
			thrd.start();
		}
	}
}
