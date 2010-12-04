package net.behoo.appmarket.downloadinstall;

import java.util.ArrayList;

import junit.framework.Assert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.provider.Downloads;

public class DownloadReceiver extends BroadcastReceiver {

	private static final String TAG = "DownloadReceiver";
	
	private ArrayList<PackageInfo> mPackages = null;
	
	public DownloadReceiver( ArrayList<PackageInfo> pkgs ) {
		mPackages = pkgs;
	}
	
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		// update the local database
		Uri uri = intent.getData();
		Log.i(TAG, "DownloadBroadcastReceiver uri: "+uri.toString());
		Bundle bundle = intent.getExtras();
		String url = bundle.getCharSequence( Downloads.COLUMN_NOTIFICATION_EXTRAS ).toString();
		PackageInfo info = null;
		for( int i = 0; i < mPackages.size(); ++i ) {
			info = mPackages.get( i );
			if( info.mDownloadUri.toString().compareTo( url ) == 0 ) {
				info.mState = Constants.PackageState.download_succeeded;
				info.sendPackageStateBroadcast(context);
				// get the file name from database
				Cursor c = context.getContentResolver().query(Downloads.CONTENT_URI, 
						new String [] { Downloads.COLUMN_URI, Downloads._DATA }, 
						null, null, null);
				Assert.assertNotNull(c);
				
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
		InstallingThread thrd = new InstallingThread(context, null, info);
		thrd.start();
	}
}
