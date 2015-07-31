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

import javax.servlet.ServletConfig;
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
public class HortonsGymActive extends AppStudioDataListener implements Runnable {
	private static final long serialVersionUID = 1L;
	private KafkaStream stream;
	private int threadNumber;
	private ConsumerConnector consumer;
	private String readtopic;
	private ExecutorService executor;
	public static Logger hrlogger;
	public static Logger stepslogger;
	public static HashMap<String, String> colors= new HashMap<String, String>();
	Pusher pusher;
	static String inactive;
	static String zookeeperString;
	
	public static String getInactive() {
		return inactive;
	}

	public void publishHR(String hrAsJson) {
		if (consumer != null) {
			hrAsJson= hrAsJson.replace("heartrate", "amount");
			sendDataToKafka(topic, hrAsJson);
		}
	}
	
	@Override
	public void init(ServletConfig cfg) throws ServletException {
		super.init(cfg);
		
		inactive = cfg.getInitParameter("inactiveServer");
		readtopic = cfg.getInitParameter("readFromTopic");
		zookeeperString=  cfg.getInitParameter("zookeeper");

		Thread puller = new Thread(new Puller(inactive+"/data/pull", this));
		puller.start();
		System.out.println("Started Pull");
		
		pusher= new Pusher(inactive+"/data/push", this);
		Thread p= new Thread(pusher);
		p.start();
	//	System.out.println("Started Push");
	}

	public HortonsGymActive() {
		super();
		hrlogger = Logger
				.getLogger("com.hortonworks.digitalemil.hortonsgym.heartrates");
		stepslogger = Logger
				.getLogger("com.hortonworks.digitalemil.hortonsgym.steps");

		
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
	
	private static ConsumerConfig createConsumerConfig(String zookeeper,
			String group) {
		Properties props = new Properties();
		if(zookeeper== null) {
			System.out.println("zookeeper== null. Reverting to localhost:2181");
			zookeeper= "localhost:2181";
		}
		props.put("zookeeper.connect", zookeeper);
		props.put("group.id", group);
		props.put("zookeeper.session.timeout.ms", "400");
		props.put("zookeeper.sync.time.ms", "200");
		props.put("auto.commit.interval.ms", "1000");

		return new ConsumerConfig(props);
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
								zookeeperString, "group"));
				System.out.println("Kafka Consumer created: "+consumer+" @"+zookeeperString);
				
			} catch (Exception e) {
				e.printStackTrace();
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
			pusher.post(pusher.url, "{\"user\":\""+user+"\", \"color\":\""+color+"\"}\n");
			colors.put(user, color);
			cont= it.hasNext();
		}
		System.out.println("Shutting down Thread: " + threadNumber);
	}
}
