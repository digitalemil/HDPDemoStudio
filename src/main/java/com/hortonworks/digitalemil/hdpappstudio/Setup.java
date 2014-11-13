package com.hortonworks.digitalemil.hdpappstudio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Properties;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class Setup {
	public static final String MARKER1 = "DO NOT REMOVE ME NEEDED FOR THE SOLRWORKSHOP ITEM1";
	public static final String MARKER2 = "DO NOT REMOVE ME NEEDED FOR THE SOLRWORKSHOP ITEM2";
	public static final String MARKER3 = "<MYMARKER1/>";
	public static final String MARKER4 = "/*MYMARKER2*/";
	public static final String MARKER5 = "bg.jpg";
	public static final String MARKER6 = "<MYTITLE>";
	public static final String MARKER7 = "MYMARKER7";
	public static final String APPSFOLDER= "apps"
;
	public static final String OTHERS = " indexed=\"true\" stored=\"true\" multiValued=\"false\"";

	public static void main(String[] args) throws IOException, InterruptedException {
		Properties props = new Properties();
		String appname= args[0];
		String path= APPSFOLDER+"/"+appname+"/";
		
		Runtime.getRuntime().exec("mkdir "+path).waitFor();
		Runtime.getRuntime().exec("cp samples/hdp.jpg "+path).waitFor();
			
		try {
			InputStream is = new FileInputStream(args[1]);
			props.load(is);
		} catch (Exception e) {
			e.printStackTrace(); 
			return;
		}

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(props
					.getClass().getResourceAsStream("/schema.xml")));
			BufferedWriter bw = new BufferedWriter(new FileWriter(path+"schema.xml"));
			String line;

			while ((line = br.readLine()) != null) {
				if (line.contains(MARKER1)) {
					int i = 0;
					do {
						String name = props.getProperty("name_" + i);
						String type = props.getProperty("type_" + i);
						String others = props.getProperty("other_" + i);

						if (name == null || type == null)
							break;
						if (others == null)
							others = OTHERS;

						bw.write("<field name=\"" + name + "\" type=\"" + type
								+ "\" " + others + "/>\n");
						i++;
					} while (true);
				} else {
					if (line.contains(MARKER2)) {
						int i = 4;
						do {
							String name = props.getProperty("name_" + i);
							if (name == null)
								break;
							bw.write("<copyField source=\"" + name
									+ "\" dest=\"text\"/>\n");
							i++;
						} while (true);
					} else {
						bw.write(line + "\n");
					}
				}
			}
			br.close();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(props
					.getClass().getResourceAsStream("/index.html")));
			BufferedWriter bw = new BufferedWriter(new FileWriter(path+"index.html"));
			String line;

			while ((line = br.readLine()) != null) {
				if(line.contains(MARKER5)) {
					String imgpath = props.getProperty("bgimg");
					String imgname= imgpath;
					if(imgpath.contains("/")) {
						imgname= imgpath.substring(imgpath.lastIndexOf("/")+1);
					}
					Runtime.getRuntime().exec("cp "+imgpath+" "+path+imgname).waitFor();
					line= line.replaceFirst(MARKER5, imgname);
				}
				if(line.contains(MARKER6)) {
					String title = props.getProperty("title");
					line= line.replaceFirst(MARKER6, title);
				}
				if(line.contains(MARKER7)) {
					String showLocation = props.getProperty("showLocation");
					line= line.replaceFirst(MARKER7, showLocation);
				}
				if (line.contains(MARKER3)) {
					int i = 5;
					do {
						String name = props.getProperty("name_" + i);
						if (name == null)
							break;
						System.out.println("Adding: " + "<tr><td>" + name
								+ "</td><td><input type=\"text\" name=\""
								+ name + "\" id=\"" + name
								+ "\" size=\"40\"/></td></tr> to index.html");
						bw.write("<tr><td>" + name
								+ "</td><td><input type=\"text\" name=\""
								+ name + "\" id=\"" + name
								+ "\" size=\"40\"/></td></tr>");
						i++;
					} while (true);
				} else {
					if (line.contains(MARKER4)) {
						int i = 5;
						bw.write("+'");
						do {
							String name = props.getProperty("name_" + i);
							if (name == null) {
								bw.write("'");
								break;
							}
							bw.write(", \""+name+"\":\"'+document.getElementById(\""+name+"\").value+'\"");
							i++;
						} while (true);
					} else {
						bw.write(line + "\n");
					}
				}
			}
			br.close();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
