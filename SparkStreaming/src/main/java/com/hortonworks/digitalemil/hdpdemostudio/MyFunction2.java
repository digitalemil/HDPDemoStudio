package com.hortonworks.digitalemil.hdpdemostudio;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;

public class MyFunction2 implements Function<JavaRDD<String>, Void> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String solrurl;
	static transient public HTable table;
	
	public String getSolrurl() {
		return solrurl;
	}
	
	public HTable getTable() {
		return table;
	}

	public static void setTable(HTable t) {
		table = t;
		System.out.println("Table set: "+t);
	}

	public void setSolrurl(String solrurl) {
		this.solrurl = solrurl;
	}
	
	public Void call(JavaRDD<String> rdd) throws Exception {
		if(rdd.count()> 0) {
			System.out.println("Table: "+table);
			if(table!= null) {
				table.flushCommits();
				table.close();
			}
			System.out.println("Solrurl: "+solrurl);
			if(solrurl!= null) {
				Spark.post(solrurl+"?commit=true", "");
				rdd.saveAsTextFile("hdfs://sandbox:8020/user/guest/spark/dstream"+System.currentTimeMillis());		
			}
		
		}
		return null;
	}
}
