package net.behoo.appmarket.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtil {
	private static final int timeout = 10000;// 10sec
	
	public static final int DOWNLOAD_SUCCEED = 0;
	public static final int DOWNLOAD_FAILURE = 1;
	
	public HttpURLConnection connection = null;
	
    public InputStream httpGet(String urlStr) throws IOException {
        
        InputStream inputStream = null;
		URL url = new URL(urlStr);
		connection = (HttpURLConnection)url.openConnection();
		connection.setConnectTimeout(timeout);
		connection.setRequestMethod("GET");
		connection.connect();

        inputStream = connection.getInputStream();
		
		return inputStream;
    }
    
    public InputStream httpPost(String url) throws IOException {
    	return null;
    }
}
