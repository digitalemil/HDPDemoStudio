package com.hortonworks.digitalemil.hortonsgym;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kafka.javaapi.consumer.ConsumerConnector;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.json.JSONException;
import org.json.JSONObject;

import com.hortonworks.digitalemil.hdpappstudio.web.AppStudioDataListener;

/**
 * Servlet implementation class HortonsGym
 */
public class HortonsGym extends AppStudioDataListener implements Runnable {
	private static final long serialVersionUID = 1L;
	private HashMap<String, HeartRateMeasurement> last = new HashMap<String, HeartRateMeasurement>();
	public static HashMap<String, String> colors;
	private KafkaStream stream;
	private int threadNumber;
	private ConsumerConnector consumer;
	private String readtopic = "color";
	private ExecutorService executor;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public HortonsGym() {
		super();
		colors = new HashMap<String, String>();

		Thread thread = new Thread(this);
		thread.start();
		// TODO Auto-generated constructor stub
	}

	private static ConsumerConfig createConsumerConfig(String zookeeper,
			String group) {
		Properties props = new Properties();
		props.put("zookeeper.connect", zookeeper);
		props.put("group.id", group);
		props.put("zookeeper.session.timeout.ms", "400");
		props.put("zookeeper.sync.time.ms", "200");
		props.put("auto.commit.interval.ms", "1000");

		return new ConsumerConfig(props);
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		BufferedReader reader = request.getReader();

		StringBuffer json = new StringBuffer();
		JSONObject jobj = null;

		do {
			String line = reader.readLine();
			if (line == null)
				break;
			json.append(line + "\n");
		} while (true);

		if (request.getRequestURI().contains("upload")) {
			try {
				System.out.println("Received docs: " + json);
				Configuration configuration = new Configuration();
				System.out.println("Config: " + configuration);
				configuration.set("fs.default.name",
						"hdfs://sandbox.hortonworks.com:8020");
				System.out.println("Config1: " + configuration);

				configuration.set("fs.hdfs.impl",
						org.apache.hadoop.hdfs.DistributedFileSystem.class
								.getName());
				System.out.println("Config2: " + configuration);

				configuration.set("fs.file.impl",
						org.apache.hadoop.fs.LocalFileSystem.class.getName());
				System.out.println("Config3: " + configuration);
				System.out.println("Creating FS...");
				FileSystem hdfs = FileSystem.get(configuration);
				System.out.println("FS: " + hdfs);
				Path file = new Path(
						"hdfs://sandbox.hortonworks.com:8020/user/guest/uploads/upload"
								+ System.currentTimeMillis() + ".txt");
				if (hdfs.exists(file)) {
					hdfs.delete(file, true);
				}

				OutputStream os = hdfs.create(file);
				BufferedWriter br = new BufferedWriter(new OutputStreamWriter(
						os, "UTF-8"));
				br.write(json.toString());
				br.close();
				hdfs.close();
			} catch (Exception e) {
				System.out.println("Error uploading file: " + e);
				e.printStackTrace();
			}
			return;
		}

		try {
			jobj = new JSONObject(json.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		HeartRateMeasurement hrm = new HeartRateMeasurement(jobj);
		System.out.println("HRM created: " + hrm + " key: "
				+ jobj.getString("deviceid"));
		last.put(jobj.getString("deviceid"), hrm);

		if (jobj.has("topic")) {
			try {
				topic = jobj.getString("topic");
			} catch (JSONException e) {
				e.printStackTrace();
				topic = DEFAULTQUEUENAME;
			}
		}

		System.out.println("Received JSON: " + jobj);
		if (consumer != null)
			sendDataToKafka(topic, jobj.toString());
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		// sessions/runing
		// hr/sessions/running

		String ret = "{\"session\":{\"begincomment\":null,\"dayssince01012012\":0,\"dummy\":null,\"endcomment\":null,\"ended\":null,\"groupid\":{\"id\":1,\"name\":\"Default\"},\"id\":0,\"start\":0},\"users\":[";

		Writer writer = response.getWriter();
		// System.out.println("Last Values: " + last.size());
		writer.write(ret);
		boolean first = true;
		for (HeartRateMeasurement hrm : last.values()) {

			String json = hrm.toString();
			System.out.println("HRM: " + hrm);
			if (json.length() > 0) {
				if (!first)
					writer.write(", ");
				else
					first = false;

				writer.write(json);

			}
		}
		writer.write("]}");
		writer.flush();
	}

	public void run(int numThreads) {

		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		topicCountMap.put(readtopic, new Integer(numThreads));

		Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer
				.createMessageStreams(topicCountMap);

		List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(readtopic);

		for (KafkaStream s : streams) {
			stream = s;
			Thread thread = new Thread(this);
			thread.start();
		}
	}

	public void run() {
		if (consumer == null) {
			try {
				System.out.println("Creating Kafka Consumer...");
				consumer = kafka.consumer.Consumer
						.createJavaConsumerConnector(createConsumerConfig(
								"sandbox:2181", "group"));
			} catch (Exception e) {
				return;
			}
			System.out.println("Kafka Consumer created");
			run(1);
		}
		ConsumerIterator<byte[], byte[]> it = stream.iterator();
		System.out.println("Listing for Kafka Messages on Topic: " + readtopic);
		while (it.hasNext()) {
			// stream.
			String msg = new String(it.next().message());
			if (!msg.contains(":"))
				continue;
			String user = msg.substring(0, msg.indexOf(":"));
			String color = msg.substring(msg.indexOf(":") + 1);
			System.out.println("Color for user: " + user + " = " + color);
			colors.put(user, color);
		}
		System.out.println("Shutting down Thread: " + threadNumber);
	}
}
