package edu.ucsb.geog.blakeregalia;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

public class HTTP_Uploader {

	public static final String SERVER_URL = "http://blurcast.net/wap/upload.php";
	
	private File last_try = null;

	private ucsb_wap_activity main;
	public HTTP_Uploader(ucsb_wap_activity main_activity) {
		main = main_activity;
	}
	
	public int retry() {
		if(last_try != null) {
			String response = upload(last_try, last_try.getName());

			if(!response.equals(last_try.length()+"")) {
				main.debug("failed to upload file.");
				return 0;
			}
			else {
				main.debug("uploaded "+last_try.getName()+" to server; "+last_try.length()+" bytes");
				last_try.delete();
				last_try = null;
				return 1;
			}
		}
		return 0;
	}
	
	private String upload(File trace_file, String fname) {
		List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
		pairs.add(new BasicNameValuePair("file",
				new String(
						Base64.encode(
								getBytesFromFile(trace_file)
								)
						)
				));
		pairs.add(new BasicNameValuePair("name", fname));
		pairs.add(new BasicNameValuePair("android-id", ucsb_wap_activity.android_id));
		pairs.add(new BasicNameValuePair("phone-number", ucsb_wap_activity.phone_number));
		pairs.add(new BasicNameValuePair("version", ucsb_wap_activity.VERSION+""));
		//pairs.add(new BasicNameValuePair("user", Info.getUserHash()+""));
		String response = submitPOST(pairs);
		return response;
	}
	
	public int save(File trace_file) {
		String file_size_str = trace_file.length()+"";

        Calendar now = Calendar.getInstance();
        String fname = ""+(now.get(Calendar.YEAR)+"")+"."
        		+zero_pad(now.get(Calendar.MONTH)+1)+"."
        		+zero_pad(now.get(Calendar.DAY_OF_MONTH))+"-"
        		+zero_pad(now.get(Calendar.HOUR_OF_DAY))+"."
        		+zero_pad(now.get(Calendar.MINUTE))+"."
        		+zero_pad(now.get(Calendar.SECOND))+"_"
        		+"v"+ucsb_wap_activity.VERSION+".bin";
		
		File sd_file = save_to_SD(trace_file, fname);
		
		String response = upload(trace_file, fname);

		if(!response.equals(file_size_str)) {
			main.debug("failed to upload file.");
			last_try = sd_file;
			return 0;
		}
		else {
			main.debug("uploaded "+fname+" to server; "+trace_file.length()+" bytes");
			sd_file.delete();
			trace_file.delete();
			last_try = null;
			return 1;
		}
	}
	
	private String zero_pad(int d) {
		if(d < 10) {
			return "0"+d;
		}
		return ""+d;
	}
	
	public File save_to_SD(File trace_file, String file_name) {
        File root = Environment.getExternalStorageDirectory();

        File dir = new File(root, "./ucsb-wap-traces/");
        dir.mkdir();
        
        File output = new File(dir, file_name);
        
        FileOutputStream f = null;
        InputStream in = null;
		try {
			f = new FileOutputStream(output);
			in = new FileInputStream(trace_file);
		} catch (FileNotFoundException e) {
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
			e.printStackTrace();
		}

        try {
			main.debug("saved to: "+output.getCanonicalPath()+"\n"+output.length()+"b");
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        return output;
	}

	public String submitPOST(List<BasicNameValuePair> pairs) {
		String response_str = "";
		
		HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(SERVER_URL);

		try {
			
	        post.setEntity(new UrlEncodedFormEntity(pairs));

            // Execute HTTP Post Request
            HttpResponse http_response = client.execute(post);
            HttpEntity entity = http_response.getEntity();
            DataInputStream dis = new DataInputStream(entity.getContent());
            
			System.out.println(http_response.toString());
			StringBuilder rep = new StringBuilder();
			String tmp;
			while((tmp = dis.readLine()) != null) {
				rep.append(tmp);
			}
			response_str = rep.toString();
		} catch (IOException e1) {
			e1.printStackTrace();
		}  
		
		return response_str;
	}
	
	private byte[] getBytesFromFile(File file) {
	    try {
		    InputStream is = new FileInputStream(file);

		    // Get the size of the file
		    long length = file.length();

		    // You cannot create an array using a long type.
		    // It needs to be an int type.
		    // Before converting to an int type, check
		    // to ensure that file is not larger than Integer.MAX_VALUE.
		    if (length > Integer.MAX_VALUE) {
		    	main.debug("Log file too large");
		        // File is too large
		    }

		    // Create the byte array to hold the data
		    byte[] bytes = new byte[(int)length];
		    
			(new DataInputStream(is)).readFully(bytes);
		    // Close the input stream and return bytes
		    is.close();

		    return bytes;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return new byte[0];
	}
	
}
