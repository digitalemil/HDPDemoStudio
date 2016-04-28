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
	public static String solrurl;
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
		System.out.println("MyFunction2 call(): " + rdd+" table: "+table+" solrurl: "+solrurl);
		if(rdd.count()> 0) {
		
			if(table!= null) {
				System.out.println("Table: "+table);
				table.flushCommits();
				table.close();
			}
		
			if(solrurl!= null) {
				Spark.post(solrurl+"?commit=true", "");
				String fname= "hdfs://sandbox:8020/user/guest/spark/dstream"+rdd.hashCode()+"_"+System.currentTimeMillis();
				try {
					rdd.saveAsTextFile(fname);		
				}
				catch(Exception e) {
					e.printStackTrace();
					System.err.println("Can't write: "+fname);
					System.err.println("rdd.length: "+rdd.count());
							
				}
			}
		
		}
		System.out.println("MyFunction2 done");
		return null;
	}
}
