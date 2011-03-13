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

	public static final String DOWNLOAD_COMPLETED = 
		"net.behoo.appmarket.downloadinstall.DOWNLOAD_COMPLETED";
	public static final String EXTRA_DOWNLOAD_URI = "download_uri";
	public static final String EXTRA_APP_CODE = "app_code";
	
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		CharSequence extraChars = bundle.getCharSequence(Downloads.COLUMN_NOTIFICATION_EXTRAS);
		String uri = intent.getData().toString();
		String appCode = extraChars.toString();
		Log.d(TAG, "onReceive uri: "+uri+"--app: "+appCode);
		
		Intent i = new Intent(DownloadReceiver.DOWNLOAD_COMPLETED);
		i.setClass(context, DownloadInstallService.class);
		i.putExtra(EXTRA_DOWNLOAD_URI, uri);
		i.putExtra(EXTRA_APP_CODE, appCode);
		context.startService(i);
	}
}
