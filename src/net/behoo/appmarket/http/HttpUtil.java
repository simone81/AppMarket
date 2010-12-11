package net.behoo.appmarket.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

public class HttpUtil {
	private static final int TIMEOUT = 10000;// 10sec
	
	public static final int DOWNLOAD_SUCCEED = 0;
	public static final int DOWNLOAD_FAILURE = 1;
	
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
    
    public InputStream httpPost(String url, String requestStr) throws IOException {
    	try { 
    		HttpPost httppost = new HttpPost(url);
        	StringEntity se = new StringEntity(requestStr, HTTP.UTF_8);
        	se.setContentType("text/xml");
        	httppost.setHeader("Content-Type","application/soap+xml;charset=UTF-8");
        	httppost.setEntity(se); 
    		
    		HttpParams params = new BasicHttpParams();
    		HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
    	    HttpConnectionParams.setSoTimeout(params, TIMEOUT);
    	    HttpClient httpclient = new DefaultHttpClient(params); 
    		HttpResponse response = httpclient.execute(httppost); 
    		
    		if (null != response && HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
    			return response.getEntity().getContent();
    		}
    		return null;
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
