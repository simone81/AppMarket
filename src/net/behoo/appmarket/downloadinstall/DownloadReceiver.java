package net.behoo.appmarket.downloadinstall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Downloads;

public class DownloadReceiver extends BroadcastReceiver {

	private static final String TAG = "DownloadReceiver";

	public static final String DOWNLOAD_COMPLETED = "net.behoo.appmarket.downloadinstall.DOWNLOAD_COMPLETED";
	public static final String DOWNLOAD_DATA_URI = "data_uri";
	
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "onReceive ");
		Intent i = new Intent(DownloadReceiver.DOWNLOAD_COMPLETED);
		// the data
		Uri uri = intent.getData();
		i.putExtra(DOWNLOAD_DATA_URI, uri.toString());
		// extra data
		Bundle bundle = intent.getExtras();
		String url = bundle.getCharSequence(Downloads.COLUMN_NOTIFICATION_EXTRAS).toString();
		i.putExtra(Downloads.COLUMN_NOTIFICATION_EXTRAS, url);
		context.sendBroadcast( i );
	}
}
