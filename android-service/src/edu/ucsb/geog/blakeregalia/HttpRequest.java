package edu.ucsb.geog.blakeregalia;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class HttpRequest {

	public static final int GET = 0;
	public static final int POST = 1;

	private static String DEFAULT_SERVER = "http://anteater.geog.ucsb.edu";
	private static String DEFAULT_URL = "/wap/mobile.php";

	protected String server = null;
	protected String url = null;
	protected List<BasicNameValuePair> pairs;

	public HttpRequest() {
		server = DEFAULT_SERVER;
		url = DEFAULT_URL;
		pairs = new ArrayList<BasicNameValuePair>();
	}

	public HttpRequest(String _url) {
		server = DEFAULT_SERVER;
		url = _url;
		pairs = new ArrayList<BasicNameValuePair>();
	}

	public void addPair(String key, String value) {
		pairs.add(new BasicNameValuePair(key, value));
	}
	
	public void attempt(int type, Handler handler) {
		String response_str = submit(type);
		if(!handler.response(response_str)) {
			
		}
	}

	public String submit(int type) {

		String request_url = server+url;
		String response_str = null;

		HttpClient client = new DefaultHttpClient();

		switch(type) {

		case GET:
			HttpGet get = new HttpGet(request_url);
			response_str = "911";
			break;

		case POST:
			try {
				System.out.println(request_url);
				HttpPost post = new HttpPost(request_url);

				post.setEntity(new UrlEncodedFormEntity(pairs));

				HttpResponse http_response = client.execute(post);
				HttpEntity entity = http_response.getEntity();
				DataInputStream dis = new DataInputStream(entity.getContent());

				StringBuilder rep = new StringBuilder();
				String tmp;
				while((tmp = dis.readLine()) != null) {
					rep.append(tmp);
				}
				response_str = rep.toString();
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			break;
		}

		return response_str;
	}
	
	
	public interface Handler {
		public boolean response(String response_str);
	}
	
}
