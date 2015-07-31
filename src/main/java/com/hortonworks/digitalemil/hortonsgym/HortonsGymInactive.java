package com.hortonworks.digitalemil.hortonsgym;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
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
import org.apache.zookeeper.KeeperException;
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
public class HortonsGymInactive extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private HashMap<String, HeartRateMeasurement> last = new HashMap<String, HeartRateMeasurement>();
	public static HashMap<String, String> colors = new HashMap<String, String>();
	public static HashSet<String> steps = new HashSet<String>();
	public static HashSet<String> heartrates = new HashSet<String>();

	private static String modelString = "";

	public static void setModelString(String s) {
		modelString = s;
	}

	public static String getModelString() {
		return modelString;
	}

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public HortonsGymInactive() {
		super();

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

		try {
			jobj = new JSONObject(json.toString());
		} catch (JSONException e) {
			response.setStatus(500);

			e.printStackTrace();
		}

		System.out.println("Received JSON Activity (steps): " + jobj + " \n"
				+ json);
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		BufferedReader reader = request.getReader();

		if (request.getRequestURI().contains("model")) {
			StringBuffer m= new StringBuffer();
			do {

				String line = reader.readLine();
				if (line == null)
					break;
				m.append(line);
				
			} while (true);
			modelString= m.toString();
			return;
		}

		if (request.getRequestURI().contains("push")) {
			do {
				String line = reader.readLine();
				if (line == null)
					break;
				try {
					JSONObject c = new JSONObject(line);
					colors.put(c.getString("user"), c.getString("color"));
					System.out.println("Color update: " + c.getString("user")
							+ " " + c.getString("color"));
				} catch (JSONException e) {
					response.setStatus(500);
					e.printStackTrace();
				}
			} while (true);
			return;
		}

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

		try {
			jobj = new JSONObject(json.toString());
		} catch (JSONException e) {
			response.setStatus(500);

			e.printStackTrace();
		}
		HeartRateMeasurement hrm = new HeartRateMeasurement(jobj, colors);
		System.out.println(jobj);
		last.put(jobj.getString("deviceid"), hrm);

		System.out.println("Received JSON: " + jobj);
		heartrates.add(jobj.toString());
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Writer writer = response.getWriter();

		if (request.getRequestURI().contains("model")) {

			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						this.getClass().getResourceAsStream("/model.html")));
				String line;
				while ((line = br.readLine()) != null) {
					if (line.contains("PMMLCONTENT")) {
						line = modelString;
					}
					writer.write(line + "\n");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			writer.flush();
			return;
		}

		if (request.getRequestURI().contains("pull")) {
			response.setContentType("plain/text");
			for (String s : heartrates) {
		//		s= s.replace("heartrate", "amount");
				writer.write(s + "|");
			}
			writer.flush();
			heartrates.clear();
			return;
		}

		String ret = "{\"session\":{\"begincomment\":null,\"dayssince01012012\":0,\"dummy\":null,\"endcomment\":null,\"ended\":null,\"groupid\":{\"id\":1,\"name\":\"Default\"},\"id\":0,\"start\":0},\"users\":[";
		writer.write(ret);
		boolean first = true;
		for (HeartRateMeasurement hrm : last.values()) {

			String json = hrm.toString();
			if (json.length() > 0) {
				if (!first)
					writer.write(", ");
				else
					first = false;

				writer.write(json);

			} else {
				colors.remove(hrm.user);
				last.remove(hrm.deviceid);
			}
		}
		writer.write("]}");
		writer.flush();
	}

}
