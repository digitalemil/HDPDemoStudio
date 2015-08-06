package com.hortonworks.digitalemil.hortonsgym;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import kafka.javaapi.consumer.ConsumerConnector;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.zookeeper.ZooKeeper;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.IOUtil;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.NodeClassificationMap;
import org.jpmml.manager.PMMLManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

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
	public static Logger hrlogger;
	public static Logger stepslogger;
	PMML pmml;
	private static ModelEvaluator modelEvaluator;
	private static String modelString = "";
	ZooKeeper zookeeper;
	
	static private boolean insafemode = false;

	Map<FieldName, FieldValue> arguments = new LinkedHashMap<FieldName, FieldValue>();

	public static boolean isInSafeMode() {
		return insafemode;
	}

	public static void setModelString(String s) {
		modelString = s;
		try {
			createModelEvaluator(modelString);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	public static String getModelString() {
		return modelString;
	}

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public HortonsGym() {
		super();
		colors = new HashMap<String, String>();
		hrlogger = Logger
				.getLogger("com.hortonworks.digitalemil.hortonsgym.heartrates");
		stepslogger = Logger
				.getLogger("com.hortonworks.digitalemil.hortonsgym.steps");

	
		if (!insafemode) {
			try {
				Handler fileHandler = new FileHandler("logs/hrdata.out");
				LogFormatter formatter = new LogFormatter();
				fileHandler.setFormatter(formatter);
				hrlogger.addHandler(fileHandler);

				Handler stepsfileHandler = new FileHandler(
						"logs/stepsfromhkdata.out");
				stepsfileHandler.setFormatter(formatter);
				stepslogger.addHandler(stepsfileHandler);

			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Thread thread = new Thread(this);
			thread.start();
			
			
		}
		
		/*if (consumer == null) {
			insafemode = true;
			System.out.println("In savemode");
		}*/

		// TODO Auto-generated constructor stub
	}

	private void handleSteps(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		BufferedReader reader = request.getReader();
		StringBuffer json = new StringBuffer();
		JSONObject jobj = null;

		do {
			String line = reader.readLine();
			if (line == null)
				break;
			json.append(line + "\n");
		} while (true);

		if(!insafemode)
			stepslogger.severe(json.toString());
		try {
			jobj = new JSONObject(json.toString());
		} catch (JSONException e) {
			response.setStatus(500);

			e.printStackTrace();
		}

		System.out.println("Received JSON Activity (steps): " + jobj + " \n"
				+ json);
		if (consumer != null)
			sendDataToKafka("steps", jobj.toString());
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

		if (request.getRequestURI().contains("steps")) {
			handleSteps(request, response);
			return;
		}
		StringBuffer json = new StringBuffer();
		JSONObject jobj = null;

		do {
			String line = reader.readLine();
			if (line == null)
				break;
			json.append(line + "\n");
		} while (true);

		if(!insafemode)
			hrlogger.severe(json.toString());
		if (request.getRequestURI().contains("upload")) {
			if (insafemode)
				return;
			try {
				System.out.println("Received docs: " + json);
				hrlogger.severe(json.toString());
				Configuration configuration = new Configuration();
				configuration.set("fs.default.name",
						"hdfs://sandbox.hortonworks.com:8020");

				configuration.set("fs.hdfs.impl",
						org.apache.hadoop.hdfs.DistributedFileSystem.class
								.getName());

				configuration.set("fs.file.impl",
						org.apache.hadoop.fs.LocalFileSystem.class.getName());

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
				br.flush();
				br.close();
				hdfs.close();
			} catch (Exception e) {
				System.out.println("Error uploading file: " + e);
				response.setStatus(500);
				e.printStackTrace();
			}
			return;
		}

		try {
			jobj = new JSONObject(json.toString());
		} catch (JSONException e) {
			response.setStatus(500);

			e.printStackTrace();
		}
		HeartRateMeasurement hrm = new HeartRateMeasurement(jobj, colors);
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
		if (consumer != null && !insafemode) {
		
			if(jobj.has("user")) {
				String v= jobj.getString("user");
				jobj.remove("user");
				jobj.put("username", v);
			}
			sendDataToKafka(topic, jobj.toString());
			
		}
		else {
			try {
				colors.put(jobj.getString("user"),
						getColor(jobj.getDouble("heartrate")));
			} catch (Exception e) {

			}
		}
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
		System.out.println("Listening for Kafka Messages on Topic: "
				+ readtopic);
		boolean cont= true;
		try {
			cont= it.hasNext();
		}
		catch(IllegalStateException e) {
			it.resetState();
			cont= it.hasNext();
		}
		while (cont) {
			// stream.
			String msg = new String(it.next().message());
			if (!msg.contains(":"))
				continue;
			String user = msg.substring(0, msg.indexOf(":"));
			String color = msg.substring(msg.indexOf(":") + 1);
			System.out.println("Color for user: " + user + " = " + color+" "+"{\"user\":\""+user+"\", \"color\":\""+color+"\"}\n");
			colors.put(user, color);
			cont= it.hasNext();
		}
		System.out.println("Shutting down Thread: " + threadNumber);
	}

	// Needed to run without Hadoop
	private static void createModelEvaluator(String modelString)
			throws SAXException, JAXBException {

		InputStream is = new ByteArrayInputStream(modelString.getBytes());

		PMML pmml = IOUtil.unmarshal(is);

		PMMLManager pmmlManager = new PMMLManager(pmml);

		modelEvaluator = (ModelEvaluator) pmmlManager.getModelManager(null,
				ModelEvaluatorFactory.getInstance());

		System.out.println("New model created for: " + modelString);
	}

	public String getColor(Double hr) {
		if (modelString == null || modelString.length() <= 0
				|| modelEvaluator == null)
			return "0x80FFFFFF";

		List<FieldName> activeFields = modelEvaluator.getActiveFields();
		for (FieldName activeField : activeFields) {
			FieldValue activeValue = modelEvaluator.prepare(activeField, hr);
			arguments.put(activeField, activeValue);
		}

		Map<FieldName, ?> results = modelEvaluator.evaluate(arguments);

		FieldName targetName = modelEvaluator.getTargetField();
		Object targetValue = results.get(targetName);

		NodeClassificationMap nodeMap = (NodeClassificationMap) targetValue;
		return nodeMap.getResult();
	}
}
