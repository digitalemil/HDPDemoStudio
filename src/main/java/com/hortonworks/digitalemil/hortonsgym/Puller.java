package com.hortonworks.digitalemil.hortonsgym;

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

import org.json.JSONObject;

public class Puller implements Runnable {
	String url;
	HortonsGymActive hortonsgym;
	
	public Puller(String u, HortonsGymActive hga) {
		url= u;
		hortonsgym= hga;
	}
	
	public void run() {
		while (true) {
			try {
				Thread.currentThread().sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String in= getContent(url);
			BufferedReader reader = new BufferedReader(new StringReader(in));
			StringBuffer line= new StringBuffer();
			
			do {
				try {
					String l= reader.readLine();
					if(l== null)
						break;
					line.append(l);
				} catch (IOException e) {
					break;
				}
			}
			while(true);
		//	if(line.length()> 0)
		//		System.out.println("Pulled: "+line);
			StringTokenizer st= new StringTokenizer(line.toString(), "|");
			while(st.hasMoreTokens()) {
				String t= st.nextToken();
				System.out.println("In: "+t);
				hortonsgym.publishHR(t);
			}
		}
	}

	public String getContent(String u) {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";
		try {
			url = new URL(u);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			rd = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null) {
				result += line;
			}
			rd.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

}
