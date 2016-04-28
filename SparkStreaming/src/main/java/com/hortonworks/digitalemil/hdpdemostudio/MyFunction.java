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
		System.out.println("MyFunction call(): " + tuple2);
		String json = "[" + tuple2._2 + "]";
		String url = param + "?commit=false";
		System.out.println("Posted: " + json + " to: " + url);

		Spark.post(url, json);
		System.out.println("MyFunction done.");
		return tuple2._2();
	}

}