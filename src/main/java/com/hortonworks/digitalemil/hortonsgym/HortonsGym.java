package com.hortonworks.digitalemil.hortonsgym;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.hortonworks.digitalemil.hdpappstudio.web.AppStudioDataListener;

/**
 * Servlet implementation class HortonsGym
 */
public class HortonsGym extends AppStudioDataListener {
	private static final long serialVersionUID = 1L;
    private HashMap<String, HeartRateMeasurement> last= new HashMap<String, HeartRateMeasurement>();
  
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HortonsGym() {
        super();
        // TODO Auto-generated constructor stub
    }
    
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		BufferedReader reader = request.getReader();
		
		StringBuffer json= new StringBuffer();
		JSONObject jobj= null;
		
		do {
			String line= reader.readLine();
			if(line== null)
				break;
			json.append(line);
		} while(true);
		
		try {
			jobj= new JSONObject(json.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		HeartRateMeasurement hrm= new HeartRateMeasurement(jobj);
		System.out.println("HRM created: "+hrm+" key: "+jobj.getString("deviceid"));
		last.put(jobj.getString("deviceid"), hrm);
		
		if(jobj.has("topic")) {
			try {
				topic= jobj.getString("topic");
			} catch (JSONException e) {
				e.printStackTrace();
				topic= DEFAULTQUEUENAME;
			}
		}
		
		System.out.println("Received JSON: "+jobj);
		sendDataToKafka(topic, jobj.toString());
	}


	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//sessions/runing
		//hr/sessions/running
		
		String ret= "{\"session\":{\"begincomment\":null,\"dayssince01012012\":0,\"dummy\":null,\"endcomment\":null,\"ended\":null,\"groupid\":{\"id\":1,\"name\":\"Default\"},\"id\":0,\"start\":0},\"users\":[";
		
		Writer writer = response.getWriter();
		System.out.println("Last Values: "+last.size());
		writer.write(ret);
		boolean first= true;
		for (HeartRateMeasurement hrm : last.values()) {
			
			String json= hrm.toString();
			System.out.println("HRM: "+hrm);
			if(json.length()>0) {
				if(!first)
					writer.write(", ");
				else
					first= false;

				writer.write(json);

			}
		}
		writer.write("]}");
		writer.flush();
	}
}
