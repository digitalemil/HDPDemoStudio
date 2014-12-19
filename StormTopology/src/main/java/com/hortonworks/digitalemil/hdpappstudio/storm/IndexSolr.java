package com.hortonworks.digitalemil.hdpappstudio.storm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;
import java.util.StringTokenizer;

import org.json.JSONObject;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class IndexSolr extends BaseRichBolt {
	private static final long serialVersionUID = 1L;

	private String SOLRURL = "solrURL";

	private OutputCollector collector;

	public IndexSolr(String solr) {
		SOLRURL = solr;
	}

	private boolean post(String serverUrl, String json) {
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
				"" + Integer.toString(json.getBytes().length));
		connection.setUseCaches(false);

		try {
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(json);
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
			System.out.println("ResponseCode: "+connection.getResponseCode());
			if (connection.getResponseCode() != 200) {
				connection.disconnect();
				return false;
			}

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		connection.disconnect();
		return true;
	}

	public void execute(Tuple tuple) {
		JSONObject json= new JSONObject();
		
		for(int i= 0; i< tuple.size(); i++) {
			json.put(tuple.getFields().get(i), tuple.getString(i));
		}
			
		System.out.println("SolrIndexer: "+json.toString());
		if(post(SOLRURL, "["+json.toString()+"]")) {
			collector.ack(tuple);
		}
		else {
			collector.fail(tuple);
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// this bolt does not emit anything
	}

	public void cleanup() {
		// nothing to cleanup
	}

	public void prepare(Map config, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;
		// nothing to prepare
	}

}
