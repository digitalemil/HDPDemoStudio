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


/**
 * Servlet implementation class HortonsGym
 */
public class HortonsGym extends HttpServlet  {
	private static final long serialVersionUID = 1L;
	private HashMap<String, HeartRateMeasurement> last = new HashMap<String, HeartRateMeasurement>();
	public static HashMap<String, String> colors;
	public static Logger hrlogger;
	public static Logger stepslogger;

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
		}
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

		if (!insafemode)
			stepslogger.severe(json.toString());
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
		if (request.getRequestURI().contains("safe")) {
			insafemode= true;
			return;
		}
		if (request.getRequestURI().contains("color")) {
			handleColor(request, response);
			return;
		}
		if (request.getRequestURI().contains("steps")) {
			handleSteps(request, response);
			return;
		}
		
		// Handling Heartrate
		StringBuffer json = new StringBuffer();
		JSONObject jobj = null;

		do {
			String line = reader.readLine();
			if (line == null)
				break;
			json.append(line + "\n");
		} while (true);

		if (!insafemode)
			hrlogger.severe(json.toString());

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

		System.out.println("Received JSON: " + jobj);

		if (!insafemode) {
			try {
				colors.put(jobj.getString("user"),
						getColor(jobj.getDouble("heartrate")));
			} catch (Exception e) {
			}
		}
	}

	private void handleColor(HttpServletRequest request,
			HttpServletResponse response) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		String ret = "{\"session\":{\"begincomment\":null,\"dayssince01012012\":0,\"dummy\":null,\"endcomment\":null,\"ended\":null,\"groupid\":{\"id\":1,\"name\":\"Default\"},\"id\":0,\"start\":0},\"users\":[";

		Writer writer = response.getWriter();
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

	//
	// Needed to run without Hadoop
	//
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

	static private boolean insafemode = false;
	private static ModelEvaluator modelEvaluator;
	private static String modelString = "";
	PMML pmml;
	Map<FieldName, FieldValue> arguments = new LinkedHashMap<FieldName, FieldValue>();

}
