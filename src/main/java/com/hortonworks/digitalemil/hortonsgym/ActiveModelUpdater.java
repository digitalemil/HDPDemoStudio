package com.hortonworks.digitalemil.hortonsgym;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.zookeeper.*;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class ModelUpdater
 */
public class ActiveModelUpdater extends HttpServlet implements
		Runnable {
	private static final long serialVersionUID = 1L;
	static ZooKeeper zookeeper = null;
	String zookeeperString;
	boolean dead = false;
	static String znode = "/hortonsgym/pmml";
	static String pmml= null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ActiveModelUpdater() {
		super();

		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(ServletConfig cfg) throws ServletException {
		super.init(cfg);
		zookeeperString = cfg.getInitParameter("zookeeper");
		initZK(zookeeperString);
		System.out.println("Getting Model: "+pmml);
		getModel();
		System.out.println("Model: "+pmml);
	}

	private void initZK(String zk) {
		try {
			zookeeper = new ZooKeeper(zk, 3000, null);
			//zookeeper.exists(znode, this);
			dead = false;
			new Thread(new ActiveModelUpdater()).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getModel() {
		if(pmml!= null)
			return pmml;
		try {
			pmml = new String(zookeeper.getData(znode, false, null));
			zookeeper.close();
		} catch (KeeperException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		return pmml;
	}
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		Writer writer = response.getWriter();
		
		String pmml= getModel();
		System.out.println("pmml: " + pmml);

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this
					.getClass().getResourceAsStream("/model.html")));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("PMMLCONTENT")) {
					line = pmml;
				}
				writer.write(line + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		BufferedReader reader = request.getReader();
		StringBuffer in = new StringBuffer();

		do {
			String line = reader.readLine();
			if (line == null)
				break;
			in.append(line);
		} while (true);

		pmml= in.toString();
		
		initZK(zookeeperString);		
		System.out.println("Zookeeper Post: " + pmml);

		try {
			zookeeper.setData(znode, pmml.getBytes(), -1);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		try {
			zookeeper.close();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		post(HortonsGymActive.getInactive()+"/model/update", in.toString());
	
	}

	/*
	public void process(WatchedEvent event) {
		System.out.println("Zookeeper process: " + event);
		String path = event.getPath();
		if (event.getType() == Event.EventType.None) {
			// We are are being told that the state of the
			// connection has changed
			switch (event.getState()) {
			case SyncConnected:
				// In this particular example we don't need to do anything
				// here - watches are automatically re-registered with
				// server and any watches triggered while the client was
				// disconnected will be delivered (in order of course)
				break;
			case Disconnected:
			case Expired:
				// It's all over
				dead = true;
				closing(KeeperException.Code.SessionExpired);
				initZK(zookeeperString);
				break;
			}
		} else {
			if (path != null && path.equals(znode)) {
				// Something has changed on the node, let's find out
				try {
					zookeeper.exists(znode, this);
				} catch (KeeperException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
*/
	private boolean post(String serverUrl, String content) {
		URL url = null;
		try {
			url = new URL(serverUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setInstanceFollowRedirects(false);
		try {
			connection.setRequestMethod("POST");
		} catch (ProtocolException e) {
			e.printStackTrace();
		}
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Length",
				"" + Integer.toString(content.getBytes().length));
		connection.setUseCaches(false);

		try {
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(content);
			wr.flush();
			wr.close();
			DataInputStream r = new DataInputStream(connection.getInputStream());
			String line = null;
			do {
				line = r.readLine();
				if (line != null)
					System.out.println(line);
			} while (line != null);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		try {
			System.out.println("ResponseCode: " + connection.getResponseCode());
			if (connection.getResponseCode() != 200) {
				connection.disconnect();
				return false;
			}

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		connection.disconnect();
		post(HortonsGymActive.inactive+"/model/update", ActiveModelUpdater.getModel());
		return true;
	}

	public void run() {
		System.out.println("ModellUpdater run...");

		try {
			synchronized (this) {
				while (!dead) {
					wait();
				}
			}
		} catch (InterruptedException e) {
		}
	}
/*
	public void closing(int rc) {
		synchronized (this) {
			notifyAll();
		}
	}

	public void exists(byte[] data) {
		System.out.println("Zookeeper Exists: " + new String(data));
	}
*/
}
