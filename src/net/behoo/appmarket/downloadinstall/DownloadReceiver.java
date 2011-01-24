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
		Intent i = new Intent(DownloadReceiver.DOWNLOAD_COMPLETED);
		i.setClass(context, DownloadInstallService.class);
		Bundle bundle = intent.getExtras();
		String extraData = bundle.getCharSequence(Downloads.COLUMN_NOTIFICATION_EXTRAS).toString();
		String [] values = {intent.getData().toString(), extraData};
		i.putExtra(DOWNLOAD_COMPLETED, values);
		context.startService(i);
	}
}
