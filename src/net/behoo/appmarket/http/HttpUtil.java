package net.behoo.appmarket.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.protocol.HTTP;

import android.util.Log;

public class HttpUtil {

	private static final String TAG = "HttpUtil";
	
	private static final int CONN_TIMEOUT = 10000;// 10sec
	private static final int READ_TIMEOUT = 10000;// 10sec
	
	private HttpURLConnection mConnection = null;
	
	// reentrant not supported
    public InputStream httpGet(String urlStr) throws IOException {
    	try {
        	URL url = new URL(urlStr);
    		mConnection = (HttpURLConnection)url.openConnection();
    		mConnection.setConnectTimeout(CONN_TIMEOUT);
    		mConnection.setReadTimeout(READ_TIMEOUT);
    		mConnection.setRequestMethod("GET");
        	mConnection.connect();
    	    return mConnection.getInputStream();
    	} catch (Throwable tr) {
    		Log.w(TAG, "httpGet "+tr.getLocalizedMessage());
    	} 
    	return null;
    }
    
    // reentrant not supported
    public InputStream httpPost(String urlStr, String requestStr) throws IOException {
    	try { 
    		URL url = new URL(urlStr);
    		mConnection = (HttpURLConnection)url.openConnection();
    		mConnection.setConnectTimeout(CONN_TIMEOUT);
    		mConnection.setReadTimeout(READ_TIMEOUT);
    		mConnection.setRequestMethod("POST");
    		mConnection.setRequestProperty("Content-Type", "Application/xml; charset=utf-8");
    		
    		mConnection.setDoOutput(true);
    		mConnection.setChunkedStreamingMode(0);

    		OutputStream out = new BufferedOutputStream(mConnection.getOutputStream());
    		byte [] data = requestStr.getBytes(HTTP.UTF_8);
    		out.write(data);

    		return mConnection.getInputStream();
    	} catch (Throwable tr) { 
    		Log.w(TAG, "httpGet "+tr.getLocalizedMessage());
    	}
    	return null;
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
