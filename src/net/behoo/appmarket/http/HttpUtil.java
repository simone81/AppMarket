package net.behoo.appmarket.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.protocol.HTTP;

public class HttpUtil {
	private static final int TIMEOUT = 10000;// 10sec
	
	private HttpURLConnection mConnection = null;
	
    public InputStream httpGet(String urlStr) throws IOException {
    	InputStream inputStream = null;
    	URL url = new URL(urlStr);
		mConnection = (HttpURLConnection)url.openConnection();
		mConnection.setConnectTimeout(TIMEOUT);
		mConnection.setRequestMethod("GET");
    	mConnection.connect();
	    inputStream = mConnection.getInputStream();
    	return inputStream;
    }
    
    public InputStream httpPost(String urlStr, String requestStr) throws IOException {
    	try { 
    		URL url = new URL(urlStr);
    		mConnection = (HttpURLConnection)url.openConnection();
    		mConnection.setConnectTimeout(TIMEOUT);
    		mConnection.setRequestMethod("POST");
    		mConnection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
    		
    		mConnection.setDoOutput(true);
    		mConnection.setChunkedStreamingMode(0);

    		OutputStream out = new BufferedOutputStream(mConnection.getOutputStream());
    		byte [] data = requestStr.getBytes(HTTP.UTF_8);
    		out.write(data);

    		return mConnection.getInputStream();
    	} catch (ClientProtocolException e) { 
    		  // TODO Auto-generated catch block 
    		  e.printStackTrace(); 
    	} catch (IOException e) { 
    		  // TODO Auto-generated catch block 
    		  e.printStackTrace(); 
    	} 
    	return null;
    }
    
    public void disconnect() {
    	if (null != mConnection) {
    		mConnection.disconnect();
    	}
    }
}
