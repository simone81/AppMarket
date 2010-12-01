package net.behoo.AppMarket;


import net.behoo.DownloadInstall.Constants;
import net.behoo.DownloadInstall.DownloadInstallService;

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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AppMarket extends Activity implements OnClickListener {
	
	private static final String TAG = "AppMarket";
	
	private boolean mServiceBound = false;
	private DownloadInstallService mInstallService = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button button = ( Button )findViewById( R.id.call_pkg_installer );
        button.setOnClickListener( this );  
        
        button = ( Button )findViewById( R.id.install_from_sdcard );
        button.setOnClickListener( this );
        
        TextView tv = ( TextView )findViewById( R.id.main_input );
        tv.setText( "http://192.168.1.50/ScreenTests.apk" );
        
        tv = ( TextView )findViewById( R.id.main_input_sdcard );
        tv.setText( "file:///sdcard/ScreenTests.apk" );
        
        startService( new Intent(this, DownloadInstallService.class) );
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	bindService( new Intent( this, DownloadInstallService.class ), mServiceConn, Context.BIND_AUTO_CREATE );
    	registerReceiver( mReceiver, new IntentFilter( Constants.ACTION_STATE ) );
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	
    	unbindService( mServiceConn );
    	unregisterReceiver( mReceiver );
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	this.stopService( new Intent(this, DownloadInstallService.class) );
    }
    
    @Override
	public void onClick(View v) {
    	//this.startActivity( new Intent( this, DetailsPage.class ) );
    	if( v.getId() == R.id.call_pkg_installer ) {
    		// download and install
    		if( mServiceBound ) {
        		TextView ev = ( TextView )findViewById( R.id.main_input );
        		updateStatusUI( "begin downloading!" );
        		mInstallService.downloadAndInstall(ev.getText().toString(), 
        				"application/vnd.android.package-archive", null, -1);
        	}
        	else {
        		updateStatusUI( "service unbound!" );
        	}
    	}
    	else {
    		// install from sdcard
    		if( mServiceBound ) {
        		TextView ev = ( TextView )findViewById( R.id.main_input_sdcard );
        		updateStatusUI( "begin installing!" );
        		mInstallService.install( ev.getText().toString() );
        	}
        	else {
        		updateStatusUI( "service unbound!" );
        	}
    	}
    	
    	
//    	Intent intent = new Intent();
//    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//    	intent.setAction(android.content.Intent.ACTION_VIEW);
//    	intent.setDataAndType(Uri.parse("file://" + "/sdcard/ScreenTests.apk"),
//    	    "application/vnd.android.package-archive");
//    	this.startActivity(intent);
    	
    	/*
    	Uri uri = Uri.parse( "file://" + "/sdcard/ScreenTests.apk" );
    	TextView tv = ( TextView )this.findViewById( R.id.main_state );
    	String str = uri.getPath();
    	str += "\n";
    	str += uri.getHost();
    	str += "\n";
    	str += uri.getEncodedPath();
    	str += "\n";
    	str += uri.getFragment();
    	str += "\n";
    	str += "end";
    	tv.setText( str );
    	*/
    	
    	/*
    	Uri uri=Uri.parse("package:com.gameclient");
    	Intent intent = new Intent(Intent.ACTION_DELETE,uri);
    	this.startActivity(intent);
    	*/ 	
    }
    
    private void updateStatusUI( String status ) {
    	TextView tv = ( TextView )findViewById( R.id.main_state );
    	tv.setText( status );
    }
    
    private ServiceConnection mServiceConn = new ServiceConnection() {
    	@Override
    	public void onServiceConnected(ComponentName cname, IBinder binder){
    		mInstallService = ((DownloadInstallService.LocalServiceBinder)binder).getService();
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