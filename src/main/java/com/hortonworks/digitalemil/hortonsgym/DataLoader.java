package com.hortonworks.digitalemil.hortonsgym;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;
import java.io.*;

public class DataLoader {
	static String server;
	
	final static int SanFrancisco= 0, PaloAlto=1, Sheridan= 2, SanDiego= 3, Jackson= 4, Chicago= 5, Boston= 6, LosAngeles= 7, Rome= 8, London= 9, Moscow= 10, Paris= 11;  
	final static String locations[]= {"37.775,-122.4183333", "37.4419444,-122.1419444", "44.7694,-106.969", "32.7152778,-117.1563889", "43.479929,-110.762428", "41.850033,-87.6500523", "42.3584308,-71.0597732", "34.0522342,-118.2436849", "41.9015141,12.4607737", "51.536086,-0.131836", "55.751849,37.573242", "48.864715,2.329102"};
	final static String names[]= {"Shaun", "Jim", "Herb", "Jamie", "Dave", "Tim", "Rob", "Scott", "Mitch", "David", "Mark", "Greg"};
	final static String devices[]= {"16380", "14321", "15121", "17445", "12444", "16453", "19201", "20452", "21345", "13896", "22783", "23999"};

	final static int hrs[]= { 120, 140, 90, 110, 150, 130, 100, 160, 130, 140, 150, 120 };
	
	public static void main(String[] args) throws InterruptedException {
		server= args[0];
		int n=4;
		
		try {
			n= new Integer(args[1]);
		}
		catch(Exception e) {
			n= names.length;
		}
		
			
		while(true) {
			for(int i= 0; i< n; i++) {
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
				
				String time= cal.get(Calendar.YEAR)+"-"+(new Integer(cal.get(Calendar.MONTH))+1)+"-"+cal.get(Calendar.DAY_OF_MONTH)+"T"+cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND)+"."+(""+(new Integer(cal.get(Calendar.MILLISECOND))/1000.0f)).substring(2)+"Z";
			        String bpm= ""+(int)(hrs[i]- 10+ Math.random()*20);
			        String json = "{'id':'" + System.currentTimeMillis() + "', 'location':'" + locations[i] + "', 'event_timestamp':'" + time + "', 'deviceid':'" + devices[i] + "', 'user':'" + names[i] + "', 'heartrate':'" + bpm + "'}";
			        post(json);
			}
			Thread.currentThread().sleep(1000);
		}
	
	}
	
	public static void post(String json) {
       
        String url = "http://" + server + "/data/publish";

        try {
        	System.out.println("Posting: "+json+ " to: "+url+" Content-Type: application/json");
            //Connect
            HttpURLConnection httpcon = (HttpURLConnection) ((new URL(url).openConnection()));
            httpcon.setDoOutput(true);
            httpcon.setRequestProperty("Content-Type", "application/json");
            httpcon.setRequestProperty("Accept", "application/json");
            httpcon.setRequestMethod("POST");
            httpcon.connect();

            //Write
            OutputStream os = httpcon.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

            writer.write(json);
            writer.close();
            os.close();

            //Read
            BufferedReader br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));

            String line = null;
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            br.close();
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }


}
