package com.hortonworks.digitalemil.hdpdemostudio;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.api.java.function.Function;
import org.json.JSONObject;
import org.json.JSONTokener;

import scala.Tuple2;

public class MyFunction3 implements Function<Tuple2<String, String>, String> {
	private static final long serialVersionUID = 1L;
	public String hbasetable, hbasecf, hbaserootdir, zparent;
	transient Configuration conf;
	transient HTable table;
	boolean retry = false;
	
	public void init() {
		System.out.println("initializing HBase Table: "+hbasetable+" cf: "+hbasecf+" rootDir: "+hbaserootdir+" zparent: "+zparent);
		conf = HBaseConfiguration.create();
		conf.set("zookeeper.znode.parent", zparent);
		conf.set("hbase.rootdir", hbaserootdir);
		try {
			table = new HTable(conf, hbasetable);
			MyFunction2.setTable(table);
			System.out.println("Table set.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String call(Tuple2<String, String> tuple2) throws Exception {
		try {
			JSONObject json = new JSONObject(new JSONTokener(tuple2._2));
			
			String id= json.getString("id");
			
			Put put = new Put(Bytes.toBytes(id));
			System.out.println("id: "+id+" "+json);
			for(int i= 0; i< json.names().length(); i++) {
				String col= json.names().getString(i);
				if("id".equals(col))
					continue;
				put.add(Bytes.toBytes(hbasecf), Bytes.toBytes(col), Bytes.toBytes(json.getString(col)));
			}
			table.put(put);
			retry = false;
		} catch (Exception e) {
			if (!retry) {
				retry = true;
				init();
				call(tuple2);
			}
		}
		return null;
	}

	public HTable getHbasetable() {
		return table;
	}

	public void setHbasetable(String hbasetable) {
		this.hbasetable = hbasetable;
	}

	public String getHbasecf() {
		return hbasecf;
	}

	public void setHbasecf(String hbasecf) {
		this.hbasecf = hbasecf;
	}

	public void setHbaseRootDir(String rootdir) {
		this.hbaserootdir= rootdir;		
	}

	public void setZNodeParent(String zp) {
		this.zparent= zp;		
	}

}