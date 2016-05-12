package com.hortonworks.digitalemil.hdpdemostudio;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;

public class MyFunction2 implements Function<JavaRDD<String>, Void> {
	private static final long serialVersionUID = 1L;
	public static String solrurl, namenode, folder;
	static transient public HTable table;
	
	public String getSolrurl() {
		return solrurl;
	}
	
	public HTable getTable() {
		return table;
	}

	public static void setTable(HTable t) {
		table = t;
	}

	public void setSolrurl(String solrurl) {
		this.solrurl = solrurl;
	}
	
	public void setNamenode(String nn) {
		this.namenode = nn;
	}

	public void setFolder(String f) {
		this.folder = f;
	}

	public Void call(JavaRDD<String> rdd) throws Exception {
		System.out.println("MyFunction2 call(): " + rdd+" table: "+table+" solrurl: "+solrurl);
		if(rdd.count()> 0) {
		
			if(table!= null) {
				System.out.println("Table: "+table);
				table.flushCommits();
				table.close();
			}
			else {
				System.err.println("table==null");			
			}
		
			if(solrurl!= null) {
				Spark.post(solrurl+"?commit=true", "");
			}
			else {
				System.err.println("SolrURL==null");
			}
			
			if(folder!= null && namenode != null) {
				String fname= namenode+folder+rdd.hashCode()+"_"+System.currentTimeMillis();
				try {
					rdd.saveAsTextFile(fname);		
				}
				catch(Exception e) {
					e.printStackTrace();
					System.err.println("Can't write: "+fname);
					System.err.println("rdd.length: "+rdd.count());
							
				}
			}
			else {
				System.out.println("Namenode or hdfs folder == null: "+namenode+" "+folder);
			}
		
		}
		return null;
	}
}
