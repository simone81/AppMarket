package net.behoo.AppMarket;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

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
	
	Thread mInstallThread = new Thread(new Runnable() {

		@Override
		public void run() {
			
			PackageManager pm = PackageInstallerService.this.getPackageManager();
			Context c = PackageInstallerService.this.getApplicationContext();
			while( !mThreadExit ) {
				try {
					Thread.sleep( 2000 );
					
					PackageInstaller pi = new PackageInstaller(c, pm);
					pi.installPackage( Uri.parse("file://" + "/sdcard/ScreenTests.apk") );
					//Intent intent = new Intent( TEST_BROADCAST );
					//PackageInstallerService.this.sendBroadcast(intent);
				}
				catch ( Throwable tr ) {
					Log.e(TAG, tr.getMessage());
				}
			}
		}
	});
}
