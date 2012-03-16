package edu.ucsb.geog.blakeregalia;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.http.*;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.*;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.DefaultedHttpParams;
import org.apache.http.message.*;

import android.util.Log;

public class HTTP_Uploader {

	public static final String SERVER_URL = "http://ags2.geog.ucsb.edu/wap-tracer/upload.php";

	private ucsb_wap_activity main;
	private HttpURLConnection conn;

	public HTTP_Uploader(ucsb_wap_activity main_activity) {
		main = main_activity;
	}
	
	public void save(File upload) {
		copyFile(upload);
		/*
        File root = Environment.getExternalStorageDirectory();

        try {
			FileOutputStream f = new FileOutputStream(new File(root, ""));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        Calendar now = Calendar.getInstance();
        String fname = ""+(now.get(Calendar.MONTH)+1)+"."+now.get(Calendar.DAY_OF_MONTH)+"."+(now.get(Calendar.YEAR)+"").substring(2)+"-"+now.get(Calendar.HOUR_OF_DAY)+"."+now.get(Calendar.MINUTE)+"."+now.get(Calendar.SECOND)+".bin";

        File dir = new File(root, "./ucsb-wap-traces/");
        dir.mkdir();
        File rename = new File(dir, fname);
        
        main.debug("rename successful:"+upload.renameTo(rename));
        
        /*
        try {
			//main.debug("saved to: "+rename.getCanonicalPath()+"\n"+rename.length()+"b");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	private String zero_pad(int d) {
		if(d < 10) {
			return "0"+d;
		}
		return ""+d;
	}
	
	public void copyFile(File upload) {
        File root = Environment.getExternalStorageDirectory();
        try {
			FileOutputStream f = new FileOutputStream(new File(root, ""));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        Calendar now = Calendar.getInstance();
        String fname = ""+(now.get(Calendar.YEAR)+"")+"."+zero_pad(now.get(Calendar.MONTH)+1)+"."+zero_pad(now.get(Calendar.DAY_OF_MONTH))+"-"+zero_pad(now.get(Calendar.HOUR_OF_DAY))+"."+zero_pad(now.get(Calendar.MINUTE))+"."+zero_pad(now.get(Calendar.SECOND))+".bin";

        File dir = new File(root, "./ucsb-wap-traces/");
        dir.mkdir();
        
        File output = new File(dir, fname);
        
        FileOutputStream f = null;
        InputStream in = null;
		try {
			f = new FileOutputStream(output);
			in = new FileInputStream(upload);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        byte[] buffer = new byte[1024];
        int len = 0;
        try {
			while ((len = in.read(buffer)) > 0) {
			    f.write(buffer, 0, len);
			}
			f.close();
	        in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        try {
			main.debug("saved to: "+output.getCanonicalPath()+"\n"+output.length()+"b");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	}

	public void submit(File upload) {
		HttpClient client = new DefaultHttpClient();

        HttpPost post = new HttpPost("http://ags2.geog.ucsb.edu/wap-tracer/upload.php");
//        System.err.println("send to server "+s);
        List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();

		try {
			pairs.add(new BasicNameValuePair("test", "blake"));
			//entity = new ByteArrayEntity(getBytesFromFile(upload));
			
	        post.setEntity(new UrlEncodedFormEntity(pairs));

            // Execute HTTP Post Request
            HttpResponse response = client.execute(post);
            HttpEntity entity = response.getEntity();
            DataInputStream dis = new DataInputStream(entity.getContent());
            System.out.println("postData: "+ response.getStatusLine().toString());
            
			System.out.println(response.toString());
			StringBuilder rep = new StringBuilder();
			String tmp;
			while((tmp = dis.readLine()) != null) {
				rep.append(tmp);
			}
			System.out.println(rep.toString());
            main.debug(rep.toString());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}  

	}

	public void submitfuck(File upload) {
//        HttpClient client = new DefaultHttpClient((ConnectionManager) main.getSystemService(Context.CONNECTIVITY_SERVICE), new BasicHttpParams());
		HttpClient client = new DefaultHttpClient();

        HttpPost method = new HttpPost("http://ags2.geog.ucsb.edu/wap-tracer/upload.php");
//        System.err.println("send to server "+s);

        HttpEntity entity = null;
		try {
			entity = new ByteArrayEntity(getBytesFromFile(upload));
	        method.setEntity(entity);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}  

        try {
			HttpResponse resp = client.execute(method);
			DataInputStream dis = new DataInputStream(resp.getEntity().getContent());
			StringBuilder rep = new StringBuilder();
			String tmp;
			while((tmp = dis.readLine()) != null) {
				rep.append(tmp);
			}
			main.debug(rep.toString());
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private byte[] getBytesFromFile(File file) throws IOException {
	    InputStream is = new FileInputStream(file);

	    // Get the size of the file
	    long length = file.length();

	    // You cannot create an array using a long type.
	    // It needs to be an int type.
	    // Before converting to an int type, check
	    // to ensure that file is not larger than Integer.MAX_VALUE.
	    if (length > Integer.MAX_VALUE) {
	        // File is too large
	    }

	    // Create the byte array to hold the data
	    byte[] bytes = new byte[(int)length];
	    
	    (new DataInputStream(is)).readFully(bytes);

	    // Close the input stream and return bytes
	    is.close();
	    return bytes;
	}
	
	

	public void submit2(File upload) {
		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary = "*****";
		try {
			// ------------------ CLIENT REQUEST

			FileInputStream fileInputStream = new FileInputStream(upload);

			// open a URL connection to the Servlet

			URL url = new URL(SERVER_URL);

			// Open a HTTP connection to the URL

			conn = (HttpURLConnection) url.openConnection();

			// Allow Inputs
			conn.setDoInput(true);

			// Allow Outputs
			conn.setDoOutput(true);

			// Don't use a cached copy.
			conn.setUseCaches(false);

			// Use a post method.
			conn.setRequestMethod("POST");

			conn.setRequestProperty("Connection", "keep-alive");

			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

			DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
			DataInputStream dis = new DataInputStream(conn.getInputStream());

			dos.writeBytes(twoHyphens + boundary + lineEnd);
			dos.writeBytes("Content-Disposition: form-data; name=\"userfile\"; filename=\"trace.bin\"" + lineEnd);
			dos.writeBytes("Content-Type: application/octet-stream" + lineEnd);
			dos.writeBytes(lineEnd);

			// create a buffer of maximum size

			/*
			int iter = 0;
			while(fileInputStream.available() == 0) {
				System.out.println("input stream unavailable. "+iter);
				if(iter > 100) break;
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}*/
			int bytesAvailable = fileInputStream.available();
			int maxBufferSize = 1000;
			// int bufferSize = Math.min(bytesAvailable, maxBufferSize);
			byte[] buffer = new byte[bytesAvailable];

			// read file and write it into form...

			int bytesRead = fileInputStream.read(buffer, 0, bytesAvailable);
			System.out.println("bytes read: "+bytesRead);

			while (bytesRead > 0) {
				dos.write(buffer, 0, bytesAvailable);
				System.out.println("wrote "+bytesAvailable+" bytes to dos");
				bytesAvailable = fileInputStream.available();
				bytesAvailable = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bytesAvailable);
			}

			// send multipart form data necesssary after file data...

			dos.writeBytes(lineEnd);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

			// close streams
			fileInputStream.close();
			dos.flush();
			dos.close();
			
			StringBuilder response = new StringBuilder();
			String tmp;
			while((tmp = dis.readLine()) != null) {
				response.append(tmp+"\n");
			}
			dis.close();
			main.debug(conn.getResponseCode()+": "+conn.getResponseMessage()+"\n"+response.toString());

		} catch (MalformedURLException ex) {
		}

		catch (IOException ioe) {
		}

		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				Log.e("Dialoge Box", "Message: " + line);
			}
			rd.close();

		} catch (IOException ioex) {
			Log.e("MediaPlayer", "error: " + ioex.getMessage(), ioex);
		}
	}
}
