package com.hortonworks.digitalemil.hdpdemostudio;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

import scala.Tuple2;

import com.google.common.collect.Lists;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;

/**
 * Consumes messages from one or more topics in Kafka and does wordcount.
 *
 * Usage: JavaKafkaWordCount <zkQuorum> <group> <topics> <numThreads>
 *   <zkQuorum> is a list of one or more zookeeper servers that make quorum
 *   <group> is the name of kafka consumer group
 *   <topics> is a list of one or more kafka topics to consume from
 *   <numThreads> is the number of threads the kafka consumer should use
 *
 * To run this example:
 *   `$ bin/run-example org.apache.spark.examples.streaming.JavaKafkaWordCount zoo01,zoo02, \
 *    zoo03 my-consumer-group topic1,topic2 1`
 */

public final class Spark {
  final String defaults[]= {"127.0.0.1:2181", "hr-spark", "hr-spark", "1", "http://sandbox:8983/solr/", "hr", "hr", "all", "hdfs://sandbox:8020/apps/hbase/data/", "http://sandbox:8020", "/user/guest/spark", "/hbase-unsecure"};
  String solrurl, namenode, folder, zookeeperznodeparent;
  private Spark() {
  }

  public static void main(String[] args) {
	  Spark spark= new Spark();
	  spark.init(args);
  }
  
  public void init(String[] args) {
	  
   if (args.length < 12) {
    	args= defaults;
    }
    solrurl= args[4]+args[5]+"/update/json";
    namenode= args[9];
    folder= args[10];
    zookeeperznodeparent= args[11];
    
    System.out.println("Starting SparkStreaming for HDPDemoStudio.");
    for(int i= 0; i< args.length; i++) {
    	System.out.println("Argument["+i+"]: "+args[i]);
    }
    System.out.println("Solrurl: "+solrurl);
    SparkConf sparkConf = new SparkConf().setAppName("HortonsSparkStreaming");
    sparkConf.set("solrurl", solrurl);
   
    // Create the context with a 1 second batch size
    JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(5000));

    int numThreads = Integer.parseInt(args[3]);
    Map<String, Integer> topicMap = new HashMap<String, Integer>();
    String[] topics = args[2].split(",");
    for (String topic: topics) {
      topicMap.put(topic, numThreads);
    }

    JavaPairReceiverInputDStream<String, String> messages =
            KafkaUtils.createStream(jssc, args[0], args[1], topicMap);
    MyFunction solr= new MyFunction();
    solr.setParam(solrurl);
    MyFunction2 hdfsAndSolrCommit= new MyFunction2();
    hdfsAndSolrCommit.setSolrurl(solrurl);
    hdfsAndSolrCommit.setNamenode(namenode);
    hdfsAndSolrCommit.setFolder(folder);
    
     
    MyFunction3 hbase= new MyFunction3();
    hbase.setHbasetable(args[6]);
    hbase.setHbasecf(args[7]);
    hbase.setHbaseRootDir(args[8]);
    hbase.init();
    MyFunction2 hbaseCommit= new MyFunction2();
    
    messages.map(solr).foreachRDD(hdfsAndSolrCommit);
    messages.map(hbase).foreachRDD(hbaseCommit);
     
    jssc.start();
    jssc.awaitTermination();
  }
  
  public static boolean post(String serverUrl, String json) {
	   System.out.println("Spark post: "+json+" to: "+serverUrl);
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
		System.out.println("Spark post success");
		
		return true;
	}
}
