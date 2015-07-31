package com.hortonworks.digitalemil.hortonsgym;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

public class Pusher implements Runnable {
	String url;
	HortonsGymActive hortonsgym;
	int n= 0;
	
	public Pusher(String u, HortonsGymActive hga) {
		url= u;
		hortonsgym= hga;
	}
	
	public void run() {
		StringBuffer content= new StringBuffer();
		while (true) {
			try {
				Thread.currentThread().sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			Iterator it = hortonsgym.colors.entrySet().iterator();
		    while (it.hasNext()) {
		    	 Map.Entry pair = (Map.Entry)it.next();
		    	content.append("{\"user\":\""+pair.getKey()+"\", \"color\":\""+pair.getValue()+"\"}\n");
		    }
		    post(url, content.toString());
		    content.setLength(0);
		    
		    n++;
		 //   System.out.println("Push Model...:"+n);
		    if((n%30)==1) {
		    	System.out.println("Push Model..."+HortonsGymActive.inactive+"/model/update");
		    	String pmml= ActiveModelUpdater.pmml;
		    	if(pmml != null) {
		    		System.out.println("Result: "+post(HortonsGymActive.inactive+"/model/update", pmml));
		    	}
		    }
		}
	}
	
	public boolean post(String serverUrl, String content) {
		URL url = null;
		try {
			url = new URL(serverUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setInstanceFollowRedirects(false);
		try {
			connection.setRequestMethod("POST");
		} catch (ProtocolException e) {
			e.printStackTrace();
		}
		connection.setRequestProperty("Content-Type", "text/plain");
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Length",
				"" + Integer.toString(content.getBytes().length));
		connection.setUseCaches(false);

		try {
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(content);
			wr.flush();
			wr.close();
			DataInputStream r = new DataInputStream(connection.getInputStream());
			String line = null;
			do {
				line = r.readLine();
				if (line != null)
					System.out.println(line);
			} while (line != null);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		try {
			if (connection.getResponseCode() != 200) {
				System.out.println("ResponseCode: "+connection.getResponseCode());
				
				connection.disconnect();
				return false;
			}

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		connection.disconnect();
		return true;
	}

}
