package com.hortonworks.digitalemil.hortonsgym;

import org.json.JSONException;
import org.json.JSONObject;

public class HeartRateMeasurement {
	String hr, deviceid, user, location, id;
	long created;
	public HeartRateMeasurement(JSONObject json) {
		try {
			id= json.getString("id");
			user= json.getString("user");
			location= json.getString("location");
			hr= json.getString("heartrate");
			deviceid= json.getString("deviceid");
		}
		catch(Exception e) {
			
		}
		created= System.currentTimeMillis();
	}
	
	public String toString() {
		if(System.currentTimeMillis()> created+ 30*1000)
			return "";
		String color= HortonsGym.colors.get(user);
		if(color== null)
			color= "0x80FFFFFF";
		return "{\"calories\":\"\",\"color\":\""+color+"\",\"hr\":\""+hr+"\",\"name\":\""+user+"\",\"recovery\":\"\",\"zone\":\""+deviceid+"\"}";

	}
}
