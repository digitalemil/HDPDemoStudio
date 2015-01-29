package com.hortonworks.digitalemil.hdpappstudio;

import java.util.Calendar;
import java.util.TimeZone;

public class GenerateData {
	final static String locations[]= {"37.775,-122.4183333", "37.4419444,-122.1419444", "44.7694,-106.969", "32.7152778,-117.1563889", "43.479929,-110.762428", "41.850033,-87.6500523", "42.3584308,-71.0597732", "34.0522342,-118.2436849", "41.9015141,12.4607737", "51.536086,-0.131836", "55.751849,37.573242", "48.864715,2.329102"};
	static String DELIMITER= "|";
	static int LINES= 512;
	
	public static void main(String[] args) {
		String fields[] = new String[args.length-2];
		LINES= new Integer(args[0]);
		DELIMITER= args[1];
		
		for (int i = 0; i < fields.length; i++) {
			if(i> 0)
				System.out.print(DELIMITER);
			fields[i] = args[i+2];
			System.out.print(fields[i]);
		}
		System.out.println();
		
		long id = System.currentTimeMillis();
		StringBuffer line = new StringBuffer();
		for (int i = 0; i < LINES; i++) {

			for (int f = 0; f < fields.length; f++) {
				long now = System.currentTimeMillis();
				if(f> 0)
					line.append(DELIMITER);
				if (fields[f].equals("id")) {
					line.append((id++));
					continue;
				}
				if (fields[f].equals("event_timestamp")) {
					Calendar cal = Calendar.getInstance(TimeZone
							.getTimeZone("UTC"));
					String time = cal.get(Calendar.YEAR)
							+ "-"
							+ (new Integer(cal.get(Calendar.MONTH)) + 1)
							+ "-"
							+ cal.get(Calendar.DAY_OF_MONTH)
							+ "T"
							+ cal.get(Calendar.HOUR_OF_DAY)
							+ ":"
							+ cal.get(Calendar.MINUTE)
							+ ":"
							+ cal.get(Calendar.SECOND)
							+ "."
							+ ("" + (new Integer(cal.get(Calendar.MILLISECOND)) / 1000.0f))
									.substring(2) + "Z";
					line.append(time);
					continue;
				}
				if (fields[f].equals("location")) {
					line.append(locations[(int)(locations.length*Math.random())]);
					continue;
				}
				if (fields[f].equals("heartrate")) {
					String val = (long)(80+60*Math.random())+"";
					line.append(val);
					continue;
				}
				String val = (long)(((0.1f + Math.random()) * (i + 2) * 876243763486f))+"";
				line.append(val);
			}
			System.out.println(line.toString());
			line.setLength(0);
		}

	}
}
//java -classpath "/usr/hdp/2.2.0.0-2041/hadoop-yarn/*:/usr/hdp/2.2.0.0-2041/hadoop-mapreduce/*:/opt/solr/lw/lib/*:/usr/hdp/2.2.0.0-2041/hadoop/lib/*:/opt/solr/lucidworks-hadoop-lws-job-1.3.0.jar:/usr/hdp/2.2.0.0-2041/hadoop/*" com.lucidworks.hadoop.ingest.IngestJob -DcsvFieldMapping=0=id,1=location,2=event_timestamp,3=deviceid,4=heartrate,5=user -DcsvDelimiter="|" -Dlww.commit.on.close=true -cls com.lucidworks.hadoop.ingest.CSVIngestMapper -c hr -i ./csv -of com.lucidworks.hadoop.io.LWMapRedOutputFormat -s http://172.16.227.204:8983/solr