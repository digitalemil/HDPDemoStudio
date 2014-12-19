package com.hortonworks.digitalemil.hdpappstudio;

import java.util.Calendar;
import java.util.TimeZone;

public class GenerateData {

	public static void main(String[] args) {
		String fname = args[0];
		String fields[] = new String[args.length - 1];

		for (int i = 0; i < fields.length; i++) {
			fields[i] = args[i + 1];
		}

		long id = System.currentTimeMillis();
		StringBuffer line = new StringBuffer();
		for (int i = 0; i < 2048; i++) {

			for (int f = 0; f < fields.length; f++) {
				long now = System.currentTimeMillis();

				if (fields[f].equals("id")) {
					line.append((id++)+"\t");
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
					line.append(time+"\t");
					continue;
				}
				if (fields[f].equals("location")) {
					line.append("0:0\t");
				}
				String val = ""
						+ (int) ((0.1f + Math.random()) * (i + 2) * 876243763486f);
				line.append(val);
			}
		}

	}
}
