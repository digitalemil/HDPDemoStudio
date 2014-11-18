package com.hortonworks.digitalemil.hdpappstudio.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

/**
 * Servlet implementation class AppStudioDataSearcher
 */
public class AppStudioDataSearcher extends HttpServlet {
	private static final long serialVersionUID = 1L;
	protected String hbasetable, hbasecolumnfamily, solrurl;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AppStudioDataSearcher() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(ServletConfig cfg) throws ServletException {
		super.init(cfg);

		hbasetable = cfg.getInitParameter("hbasetable");
		hbasecolumnfamily = cfg.getInitParameter("hbasecolumnfamily");
		solrurl = cfg.getInitParameter("solrurl");

		System.out.println("Search Params: ");
		System.out.println("HBase Table: " + hbasetable);
		System.out.println("HBase ColumnFamily: " + hbasecolumnfamily);
		System.out.println("HBase Solr URL: " + solrurl);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String locations = "", query = request.getQueryString();
		boolean hbase = false, map = true;

		Writer writer = response.getWriter();

		if (request.getRequestURI().contains("hbaseLocations")) {
			locations = queryLocationsViaHBase();
			hbase = true;
		} else {
			if (request.getRequestURI().contains("searchLocations")) {
				locations = searchLocationsViaSolr(request);
			} else {
				if (request.getRequestURI().contains("solrData")) {
					writer.write(searchLocationsViaSolr(request));
					writer.flush();
					return;
				}
				if (request.getRequestURI().contains("hbaseData")) {
					writer.write(queryLocationsViaHBase());
					writer.flush();
					return;
				}
				if (request.getRequestURI().contains("hbase")) {
					hbase= true;
					locations = getHBaseData();
					map= false;
				} else {
					locations = getSolrData(request);
					map= false;
				}
			}
		}

		if (map) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						this.getClass().getResourceAsStream("/map.html")));
				String line;
				while ((line = br.readLine()) != null) {
					if (line.contains("var LOCATIONS")) {
						System.out.println("LOCATIONS: " + locations);
						line = "var LOCATIONS = \"" + locations + "\";";
					}
					if (!hbase && line.contains("var SOLRQUERY")) {
						line = "var SOLRQUERY = \"" + query + "\";";
					}
					if (line.contains("var AUTOREFRESH")) {
						String auto = "true;";
						if (request.getParameter("refresh") != null
								&& !"checked".equals(request
										.getParameter("refresh"))) {
							auto = "false;";
						}
						line = "var AUTOREFRESH = " + auto + ";";
					}
					if (line.contains("var FORSOLR")) {
						line = "var FORSOLR = " + !hbase + ";";
					}
					if (line.contains("var SHOWQUERY")) {
						line = "var SHOWQUERY = " + !hbase + ";";
					}
					writer.write(line + "\n");
				}
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						this.getClass().getResourceAsStream("/listdata.html")));
				String line;
				while ((line = br.readLine()) != null) {
					if (line.contains("<DATAHERE/>")) {
						System.out.println("LOCATIONS: " + locations);
						line = locations;
					}
					if (line.contains("var FORSOLR")) {
						line = "var FORSOLR = " + !hbase + ";";
					}
					writer.write(line + "\n");
				}
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		writer.flush();
	}


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	public String getLocationsAsJSONString(HashMap<Location, Integer> locations) {
		int total = locations.size();
		StringBuffer ret = new StringBuffer("{ 'total':'" + total
				+ "', 'locations': [");
		Set<Location> keys = locations.keySet();
		int n = 0;
		for (Location l : keys) {
			ret.append(l.toString());
			if (n < total - 1)
				ret.append(", ");
			n++;
		}
		ret.append("] }");
		return ret.toString();
	}

	public String queryLocationsViaHBase() throws IOException {
		Configuration config = (Configuration) HBaseConfiguration.create();
		config.set("zookeeper.znode.parent", "/hbase-unsecure");
		config.set("hbase.rootdir", "hdfs://sandbox:8020/apps/hbase/data/");
		HTable table = new HTable(config, hbasetable);
		Scan scan = new Scan();
		scan.setCaching(1024);
		scan.setBatch(1024);
		scan.addFamily(Bytes.toBytes(hbasecolumnfamily));

		HashMap<Location, Integer> locations = new HashMap<Location, Integer>();

		ResultScanner scanner = table.getScanner(scan);
		for (Result result = scanner.next(); (result != null); result = scanner
				.next()) {
			List<KeyValue> kvs = result.getColumn(hbasecolumnfamily.getBytes(),
					"location".getBytes());
			Location loc = new Location();
			String location = new String(kvs.get(0).getValue());
			String latitude = location.substring(0, location.indexOf(","));
			String longitude = location.substring(location.indexOf(",") + 1);
			loc.latitude = latitude;
			loc.longitude = longitude;

			if (locations.containsKey(loc)) {
				Integer n = locations.get(loc);
				loc.n = n + 1;
				locations.remove(loc);
				locations.put(loc, loc.n);
			} else {
				loc.n = 1;
				locations.put(loc, 1);
			}
		}
		table.close();
		return getLocationsAsJSONString(locations);
	}

	public String getHBaseData() throws IOException {
		StringBuffer ret= new StringBuffer();
		Configuration config = (Configuration) HBaseConfiguration.create();
		config.set("zookeeper.znode.parent", "/hbase-unsecure");
		config.set("hbase.rootdir", "hdfs://sandbox:8020/apps/hbase/data/");
		HTable table = new HTable(config, hbasetable);
		Scan scan = new Scan();
		scan.setCaching(1024);
		scan.setBatch(1024);
		scan.addFamily(Bytes.toBytes(hbasecolumnfamily));

		ResultScanner scanner = table.getScanner(scan);

		boolean firstrow= true;
		for (Result result = scanner.next(); (result != null); result = scanner
				.next()) {
			NavigableMap<byte[],byte[]>	kvs= result.getFamilyMap(Bytes.toBytes(hbasecolumnfamily));
			boolean firstcol= true;
			if(firstrow) {
				for (byte[] key : kvs.keySet()) {
					if(!firstcol) {
						ret.append(", ");
					}
					else {
						firstcol= false;
					}
					ret.append(new String(key));
				}	
				ret.append("\n");
				firstrow= false;
			}
			firstcol= true;
			for (byte[] key : kvs.keySet()) {
				if(!firstcol) {
					ret.append(", ");
				}
				else {
					firstcol= false;
				}
				ret.append(new String(kvs.get(key)));				
			}
			ret.append("\n");
			
		}
		table.close();
		return ret.toString();
	}
	
	public String searchLocationsViaSolr(HttpServletRequest request) {
		HashMap<Location, Integer> locations = new HashMap<Location, Integer>();

		SolrServer server = new HttpSolrServer(solrurl);
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setRows(1024);

		Map<java.lang.String, java.lang.String[]> params = request
				.getParameterMap();
		for (Object param : params.keySet()) {
			StringBuffer buf = new StringBuffer();
			if (param.equals("refresh")) {
				continue;
			}
			for (int i = 0; i < params.get(param).length; i++) {
				buf.append(params.get(param)[i]);
			}
			solrQuery.set(param.toString(), buf.toString());
		}
		System.out.println("solr query: " + solrQuery);
		QueryResponse rsp = null;
		try {
			rsp = server.query(solrQuery);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		Iterator<SolrDocument> iter = rsp.getResults().iterator();

		while (iter.hasNext()) {
			Location loc = new Location();
			SolrDocument resultDoc = iter.next();
			String location = (String) resultDoc.getFieldValue("location");
			System.out.println("location: " + location);
			loc.latitude = location.substring(0, location.indexOf(","));
			loc.longitude = location.substring(location.indexOf(",") + 1);

			if (locations.containsKey(loc)) {
				Integer n = locations.get(loc);
				loc.n = n + 1;
				locations.remove(loc);
				locations.put(loc, loc.n);
			} else {
				loc.n = 1;
				locations.put(loc, 1);
			}
		}

		return getLocationsAsJSONString(locations);
	}

	public String getSolrData(HttpServletRequest request) {
		StringBuffer ret = new StringBuffer();

		SolrServer server = new HttpSolrServer(solrurl);
		SolrQuery solrQuery = new SolrQuery();
		solrQuery.setRows(1024);

		Map<java.lang.String, java.lang.String[]> params = request
				.getParameterMap();
		for (Object param : params.keySet()) {
			StringBuffer buf = new StringBuffer();
			if (param.equals("refresh")) {
				continue;
			}
			for (int i = 0; i < params.get(param).length; i++) {
				buf.append(params.get(param)[i]);
			}
			solrQuery.set(param.toString(), buf.toString());
		}
		QueryResponse rsp = null;
		try {
			rsp = server.query(solrQuery);
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
		Iterator<SolrDocument> iter = rsp.getResults().iterator();

		boolean firstrow= true;
		while (iter.hasNext()) {
			SolrDocument resultDoc = iter.next();
			Map<String, Object> kvs= resultDoc.getFieldValueMap();
			boolean firstcol= true;
			if(firstrow) {
				for (String key : kvs.keySet()) {
					if(key.contains("_version_"))
						continue;
					if(!firstcol) {
						ret.append(", ");
					}
					else {
						firstcol= false;
					}
					ret.append(key);
				}	
				ret.append("\n");
				firstrow= false;
			}
			
			firstcol= true;
			for (String key : kvs.keySet()) {
				if(key.contains("_version_"))
					continue;
				if(!firstcol) {
					ret.append(", ");
				}
				else {
					firstcol= false;
				}
				ret.append(resultDoc.getFieldValue(key));				
			}
			ret.append("\n");
			
		}

		return ret.toString();
	}

}
