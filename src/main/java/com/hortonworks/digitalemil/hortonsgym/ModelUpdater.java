package com.hortonworks.digitalemil.hortonsgym;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

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
public class ModelUpdater extends HttpServlet implements Watcher, Runnable {
	private static final long serialVersionUID = 1L;
	ZooKeeper zookeeper = null;
	String zookeeperString;
	boolean dead = false;
	String znode = "/hortonsgym/pmml";
	String pmml;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ModelUpdater() {
		super();

		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(ServletConfig cfg) throws ServletException {
		super.init(cfg);
		if(!HortonsGym.isInSafeMode()) {
			zookeeperString = cfg.getInitParameter("zookeeper");
			initZK(zookeeperString);
		}
	}

	private void initZK(String zk) {
		try {
			zookeeper = new ZooKeeper(zk, 3000, this);
			zookeeper.exists(znode, this);
			dead = false;
			new Thread(new ModelUpdater()).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		Writer writer = response.getWriter();
		
		
		if (HortonsGym.isInSafeMode()) {
			pmml = HortonsGym.getModelString();
		} else {
			try {
				pmml = new String(zookeeper.getData(znode, false, null));
			} catch (KeeperException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			System.out.println("pmml: " + pmml);
		}

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

		if (HortonsGym.isInSafeMode()) {
			HortonsGym.setModelString(in.toString());
		} else {
		
			System.out.println("Zookeeper Post: " + in.toString());

			try {
				zookeeper.setData(znode, in.toString().getBytes(), -1);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

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

	public void closing(int rc) {
		synchronized (this) {
			notifyAll();
		}
	}

	public void exists(byte[] data) {
		System.out.println("Zookeeper Exists: " + new String(data));
		pmml = new String(data);
	}

}
