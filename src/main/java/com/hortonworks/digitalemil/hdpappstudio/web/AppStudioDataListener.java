package com.hortonworks.digitalemil.hdpappstudio.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.json.*;

/**
 * Servlet implementation class AppStudioDataListener
 */
public class AppStudioDataListener extends HttpServlet {
	private static final long serialVersionUID = 1L;
    public final static String DEFAULTQUEUENAME= "default";  
    private static Producer<String, String> producer;
    private static 	ProducerConfig config;
    protected String topic= DEFAULTQUEUENAME;
    
    @Override
    public void init(ServletConfig cfg) throws ServletException {
        super.init(cfg);
        Properties props = new Properties();

        String brokerList = cfg.getInitParameter("brokerList");
		String serializer = cfg.getInitParameter("serializer");
		String partitioner = cfg.getInitParameter("partitioner");
		String acks = cfg.getInitParameter("acks");
		topic= cfg.getInitParameter("topic");
		
		props.put("metadata.broker.list", brokerList);
		props.put("serializer.class", serializer);
		props.put("partitioner.class", partitioner);
		props.put("request.required.acks", (new Integer(acks)).toString());
		config = new ProducerConfig(props);	
		producer = new Producer<String, String>(config);
		System.out.println("Kafka Producer created: "+producer+" broker: "+brokerList);
    }
    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AppStudioDataListener() {
        super();
       
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
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

	
	public void sendDataToKafka(String queue, String topic) {
		KeyedMessage<String, String> data = new KeyedMessage<String, String>(
				queue, topic);
		System.out.println("Sending data to kafka topic: "+queue+" data: " +topic);
		try {
			producer.send(data);
		} catch (Exception e) {
			try {
				producer.close();
			} catch (Exception e1) {

			}
			try {
				Thread.currentThread().sleep(1000);
			} catch (Exception e2) {

			}
			producer = new Producer<String, String>(config);
		}
	}
}
