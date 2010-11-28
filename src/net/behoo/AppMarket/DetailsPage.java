package net.behoo.AppMarket;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;

public class DetailsPage extends Activity {
	private static final String TAG = "DetailsPage";
	
	private boolean mServiceBound = false;
	private PackageInstallerService mInstallService = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.details_page); 
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	bindService( new Intent( this, PackageInstallerService.class ), mServiceConn, Context.BIND_AUTO_CREATE );
    	registerReceiver( mReceiver, new IntentFilter( PackageInstallerService.TEST_BROADCAST ) );
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	
    	unbindService( mServiceConn );
    	unregisterReceiver( mReceiver );
    }
    
    private ServiceConnection mServiceConn = new ServiceConnection() {
    	@Override
    	public void onServiceConnected(ComponentName cname, IBinder binder){
    		mInstallService = ((PackageInstallerService.LocalServiceBinder)binder).getService();
    		mServiceBound = true;
    		Log.i(TAG, "onServiceConnected cname: " + cname.toShortString());
    	}
    	@Override
    	public void onServiceDisconnected(ComponentName cname){
    		mInstallService = null;
    		mServiceBound = false;
    		Log.i(TAG, "onServiceDisconnected");
    	}
    };
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "onReceive");
		}
	};
}
