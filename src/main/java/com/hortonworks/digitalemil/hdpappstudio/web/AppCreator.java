package com.hortonworks.digitalemil.hdpappstudio.web;

import com.hortonworks.digitalemil.hdpappstudio.Setup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class AppCreator
 */
public class AppCreator extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static String HDPAPPSTUDIOHOME= "/var/lib/ambari-server/resources/views/work/HDPDemoStudio{2.4.0}";
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AppCreator() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		BufferedReader reader = request.getReader();

		StringBuffer props = new StringBuffer();

		do {
			String line = reader.readLine();
			if (line == null)
				break;
			props.append(line+"\n");
		} while (true);

		System.out.println("Properties received: " + props);

		props.append("\nname_0=_version_\nname_1=_root_\nname_2=location\nname_3=text\nname_4=event_timestamp\nname_5=id\ntype_0=long\ntype_1=string\ntype_2=location\ntype_3=text_general\ntype_4=date\ntype_5=string\nother_0=indexed=\"true\" stored=\"true\"\nother_1=indexed=\"true\" stored=\"false\"\nother_2=indexed=\"true\" stored=\"true\" multiValued=\"false\"\nother_3=indexed=\"true\" stored=\"false\" multiValued=\"true\"\nother_4=indexed=\"true\" stored=\"true\" default=\"NOW\" multiValued=\"false\"\nother_5=indexed=\"true\" stored=\"true\" required=\"true\" multiValued=\"false\"");
		Properties p = new Properties();
		p.load(new StringReader(props.toString()));

		String appname = p.getProperty("appname");
		if (appname == null || appname.length() == 0)
			appname = "yourapp";

		String jdkhome= p.getProperty("jdkhome");
		if (jdkhome == null || jdkhome.length() == 0) {
			jdkhome = "/usr/lib/jvm/java-1.7.0-openjdk.x86_64";
		}
		p.remove("jdkhome");
		
		String solrhome= p.getProperty("solrhome");
		if (solrhome == null || solrhome.length() == 0) {
			solrhome = "/opt/lucidworks-hdpsearch/solr";
		}
		p.remove("solrhome");
		
		String solrurl= p.getProperty("solrurl");
		if (solrurl == null || solrurl.length() == 0) {
			solrurl="http://127.0.0.1:8983/solr";
		}
		
		String namenode= p.getProperty("namenode");
		if (namenode == null || namenode.length() == 0) {
			namenode = "/opt/lucidworks-hdpsearch/solr";
		}
		p.remove("namenode");
		
		try {
			Runtime.getRuntime().exec("mkdir samples", new String[0], new File(HDPAPPSTUDIOHOME)).waitFor();
			p.save(new FileOutputStream(new File(HDPAPPSTUDIOHOME+"/samples/" + appname
					+ ".properties")), "");
		} catch (Exception e) {
			e.printStackTrace();
		}

		String args[] = new String[2];
		args[0] = appname;
		args[1] = "samples/" + appname + ".properties";

	/*	
		try {
			Process proc= Runtime.getRuntime().exec("cp /tmp/bg.jpg bg.jpg", new String[0], new File(HDPAPPSTUDIOHOME+"/apps/"+appname+"/jar"));
			proc.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Process proc= Runtime.getRuntime().exec("cp /tmp/bg.jpg bg.jpg", new String[0], new File(HDPAPPSTUDIOHOME+"/apps/"+appname+"/war"));
			proc.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
*/
		try {
			System.out.println("Executing: "+"java -cp "+HDPAPPSTUDIOHOME+":"+HDPAPPSTUDIOHOME+"/json-20140107.jar com.hortonworks.digitalemil.hdpappstudio.Setup "+appname+" "+"samples/" + appname
					+ ".properties "+jdkhome+" "+solrhome+" "+namenode+" ");
			Process proc= Runtime.getRuntime().exec("java -cp "+HDPAPPSTUDIOHOME+":"+HDPAPPSTUDIOHOME+"/json-20140107.jar com.hortonworks.digitalemil.hdpappstudio.Setup "+appname+" "+"samples/" + appname
					+ ".properties "+jdkhome+" "+solrurl+" "+solrhome+" "+namenode+" ", new String[0], new File(HDPAPPSTUDIOHOME));
			proc.waitFor();
			BufferedReader stdInput = new BufferedReader(new 
				     InputStreamReader(proc.getInputStream()));

				BufferedReader stdError = new BufferedReader(new 
				     InputStreamReader(proc.getErrorStream()));

				// read the output from the command
				String s = null;
				while ((s = stdInput.readLine()) != null) {
				    System.out.println(s);
				}

				// read any errors from the attempted command
				while ((s = stdError.readLine()) != null) {
				    System.out.println(s);
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			Thread.currentThread().sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	try {
			
			Process proc= Runtime.getRuntime().exec("sh apps/"+appname+"/start.sh "+solrhome, new String[0], new File(HDPAPPSTUDIOHOME));
			proc.waitFor();
			BufferedReader stdInput = new BufferedReader(new 
				     InputStreamReader(proc.getInputStream()));

				BufferedReader stdError = new BufferedReader(new 
				     InputStreamReader(proc.getErrorStream()));

				// read the output from the command
				String s = null;
				while ((s = stdInput.readLine()) != null) {
				    System.out.println(s);
				}

				// read any errors from the attempted command
				while ((s = stdError.readLine()) != null) {
				    System.out.println(s);
				}
		} catch (Exception e) {
			e.printStackTrace();
		}

	
		Writer writer = response.getWriter();
		writer.write("Application on the way...\nPlease execute: start.sh");
	}

}
