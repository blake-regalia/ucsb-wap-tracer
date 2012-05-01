package edu.ucsb.geog.blake_regalia.wap_tracer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings.Secure;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

public class Registration {
	
	public static final int VERSION = 6;
	
	private static final String REGISTRAR_PHONE_NUMBER = "19252856284";
	private static final String REGISTRATION_FILENAME = "registration.txt";
	
	private Context context;
	private TelephonyManager mTelephonyManager;
	private SmsManager mSmsManager = null;
	
	private String androidId;
	private String phoneNumber;
	
	public Registration(Context _context) {
		context = _context;
		
		System.out.println("File?");
		
		try {
			FileInputStream fis = context.openFileInput(REGISTRATION_FILENAME);
			long length = (new File(REGISTRATION_FILENAME)).length();
			String registration = new String(getBytesFromInputStream(fis, length));

			System.out.println(registration);
			
			int lineBreak = registration.indexOf('\n');
			
			System.out.println(lineBreak);
			
			// if this device has already registered
			if(lineBreak != -1) {
				androidId = registration.substring(0,  lineBreak);
				phoneNumber = registration.substring(lineBreak + 1);
				return;
			}
		} catch (FileNotFoundException e) {
			// continue, device never registered
		}
		
		mTelephonyManager =(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		
		if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
			mSmsManager = SmsManager.getDefault();
		}
		
		androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
	}

	private byte[] getBytesFromInputStream(FileInputStream is, long length) {
	    try {
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
	
	private void callback(Runnable code) {
		Thread thread = new Thread(code);
		thread.start();
	}
	
	private void register_via_sms() {
		String register_text = "wto_register:"+androidId;
		mSmsManager.sendTextMessage(REGISTRAR_PHONE_NUMBER, null, register_text, null, null);
	}
	
	private void saveRegistration() {
		FileOutputStream fos;
		try {
			fos = context.openFileOutput(REGISTRATION_FILENAME, Context.MODE_PRIVATE);
			fos.write(androidId.getBytes());
			fos.write("\n".getBytes());
			fos.write(phoneNumber.getBytes());
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public String getAndroidId() {
		return androidId;
	}
	
	public String getPhoneNumber() {
		return phoneNumber;
	}
	
	public boolean register(Runnable code, Runnable fail) {
		
		File file = context.getFileStreamPath(REGISTRATION_FILENAME);
		
		if(phoneNumber != null) {
			return true;
		}
		
		else {
			
			// device has telephone
			if(mSmsManager != null) {
				
				phoneNumber = mTelephonyManager.getLine1Number();

				System.out.println(phoneNumber);
				
				// if this phone number is available
				if(phoneNumber != null && phoneNumber.length() > 5) {
					HttpRequest hpr = new HttpRequest("/wap/mobile.php");
					hpr.addPair("phone-number", phoneNumber);
					hpr.addPair("android-id", androidId);
					String response_str = hpr.submit(HttpRequest.POST);
					
					if(response_str.equals(androidId+":"+phoneNumber)) {
						saveRegistration();
						return true;
					}
					else {
						callback(fail);
						return false;
					}
				}
				// otherwise, send SMS to collector
				else {
					register_via_sms();
					return false;
				}
			}
			// device has no cell service
			else {
				HttpRequest hpr = new HttpRequest("/wap/mobile.php");
				phoneNumber = "not a phone";
				hpr.addPair("phone-number", phoneNumber);
				hpr.addPair("android-id", androidId);
				String response_str = hpr.submit(HttpRequest.POST);
				
				if(response_str.equals(androidId+":"+phoneNumber)) {
					saveRegistration();
					return true;
				}
				else {
					callback(fail);
					return false;
				}
			}
		}
	}
	
	
}
