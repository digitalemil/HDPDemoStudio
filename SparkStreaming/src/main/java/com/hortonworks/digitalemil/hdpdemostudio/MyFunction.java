package com.hortonworks.digitalemil.hdpdemostudio;

import org.apache.spark.api.java.function.Function;

import scala.Tuple2;

public class MyFunction implements Function<Tuple2<String, String>, String> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String param;
	
	
	public String getParam() {
		return param;
	}


	public void setParam(String param) {
		this.param = param;
	}


	public String call(Tuple2<String, String> tuple2) throws Exception {
		String json= "["+tuple2._2+"]";
        //	String solrurl= new SparkConf().get("solrurl");
			String url= param+"?commit=false";
			System.out.println("Posted: "+json+" to: "+url);
	        
			Spark.post(url, json);
			return tuple2._2();
	}

}