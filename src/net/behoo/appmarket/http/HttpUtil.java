package net.behoo.appmarket.http;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class HttpUtil {

	private static final String TAG = "HttpUtil";
	
	private static final int CONN_TIMEOUT = 10000;// 10sec
	private static final int READ_TIMEOUT = 10000;// 10sec
	
	private HttpURLConnection mConnection = null;
	
	// reentrant not supported
    public InputStream httpGet(String urlStr) throws IOException {
    	URL url = new URL(urlStr);
		mConnection = (HttpURLConnection)url.openConnection();
		mConnection.setConnectTimeout(CONN_TIMEOUT);
		mConnection.setReadTimeout(READ_TIMEOUT);
		mConnection.setRequestMethod("GET");
    	mConnection.connect();
	    return mConnection.getInputStream();
    }
    
    // reentrant not supported
    public InputStream httpPost(String urlStr, String requestStr) throws IOException {
		URL url = new URL(urlStr);
		mConnection = (HttpURLConnection)url.openConnection();
		mConnection.setConnectTimeout(CONN_TIMEOUT);
		mConnection.setReadTimeout(READ_TIMEOUT);
		mConnection.setRequestMethod("POST");
		mConnection.setRequestProperty("Content-Type", "Application/xml; charset=utf-8");
		mConnection.setRequestProperty("Content-Length", 
				""+Integer.toString(requestStr.getBytes().length));
		
		mConnection.setDoInput(true);
		mConnection.setDoOutput(true);
		mConnection.setFixedLengthStreamingMode(requestStr.getBytes().length);

		DataOutputStream out = new DataOutputStream(mConnection.getOutputStream());
		out.writeBytes(requestStr);
		out.flush();
		out.close();

		return mConnection.getInputStream();
    }
    
    public void disconnect() {
    	if (null != mConnection) {
    		try {
    			mConnection.disconnect();
    			mConnection = null;
    		} catch (Throwable tr) {
    			Log.w(TAG, "disconnect "+tr.getLocalizedMessage());
    		}
    	}
    }
}
