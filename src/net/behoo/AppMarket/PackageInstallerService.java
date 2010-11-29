package net.behoo.AppMarket;


import java.net.URI;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.content.ContentValues;
import android.webkit.URLUtil;
import android.net.WebAddress;
import android.provider.Downloads;

public class PackageInstallerService extends Service {
	
	public final static String TEST_BROADCAST = "net.behoo.AppMarket.PackageInstallerService.Notify";
	private static final String TAG = "PackageInstallerService";
	
	private final IBinder mBinder = new LocalServiceBinder();
	
	private boolean mThreadExit = false;
	
	public class LocalServiceBinder extends Binder {
		PackageInstallerService getService() {
            return PackageInstallerService.this;
        }
    }
	
	private Thread mInstallThread = new Thread(new Runnable() {

		@Override
		public void run() {
			
			PackageManager pm = PackageInstallerService.this.getPackageManager();
			Context c = PackageInstallerService.this.getApplicationContext();
			while( !mThreadExit ) {
				try {
					Thread.sleep( 2000 );
					
					PackageInstaller pi = new PackageInstaller(c, pm);
					boolean ret = pi.installPackage( Uri.parse("file://" + "/sdcard/ScreenTests.apk") );
					
					//Intent intent = new Intent( TEST_BROADCAST );
					//PackageInstallerService.this.sendBroadcast(intent);
				}
				catch ( Throwable tr ) {
					Log.e(TAG, tr.getMessage());
				}
			}
			Log.i(TAG, "the installation thread exit!");
		}
	});
	
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
	public void onRebind( Intent intent ) {
		super.onRebind( intent );
		Log.i(TAG, "onRebind");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mInstallThread.start();
		Log.i(TAG, "onCreate");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
	}
	
	@Override
	public void onStart( Intent intent, int startId ) {
		super.onStart( intent, startId );
		Log.i(TAG, String.format("onStart id: %d", startId));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
	    return START_STICKY;
	}
	
	public boolean downloadAndInstall(String url, String mimetype,
            String contentDisposition, long contentLength ) {
		
		mimetype = "application/vnd.android.package-archive";
		
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
            return false;
        }
        
        // XXX: Have to use the old url since the cookies were stored using the
        // old percent-encoded url.
        ContentValues values = new ContentValues();
        values.put(Downloads.COLUMN_URI, uri.toString() );
        values.put(Downloads.COLUMN_USER_AGENT, userAgent);
        values.put(Downloads.COLUMN_NOTIFICATION_PACKAGE,
                this.getPackageName());
        values.put(Downloads.COLUMN_NOTIFICATION_CLASS,
                BrowserDownloadPage.class.getCanonicalName());
        values.put(Downloads.COLUMN_VISIBILITY, Downloads.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);// the flag could show UI ? tbd
        values.put(Downloads.COLUMN_MIME_TYPE, mimetype);
        values.put(Downloads.COLUMN_FILE_NAME_HINT, filename);
        values.put(Downloads.COLUMN_DESCRIPTION, uri.getHost());
        if (contentLength > 0) {
            values.put(Downloads.COLUMN_TOTAL_BYTES, contentLength);
        }
        if (mimetype == null) {
            // We must have long pressed on a link or image to download it. We
            // are not sure of the mimetype in this case, so do a head request
            new FetchUrlMimeType(this).execute(values);
        } else {
            final Uri contentUri =
                    getContentResolver().insert(Downloads.CONTENT_URI, values);
            viewDownloads(contentUri);
        }
	}
}
