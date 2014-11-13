package com.hortonworks.digitalemil.hdpappstudio.storm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;

import org.apache.storm.hbase.bolt.HBaseBolt;
import org.apache.storm.hbase.bolt.mapper.SimpleHBaseMapper;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;


public class Topology {

	public static final String KAFKA_SPOUT_ID = "kafka-spout";
	public static final String INDEXSOLR_BOLT_ID = "solr-bolt";
	public static final String HBASE__BOLT_ID = "hbase-bolt";

	public static final String TUPLETRANSFORMER_BOLT_ID = "tupletransformer-bolt";
	
	public static void main(String[] args) throws Exception {
		
		if(args.length< 7) {
			System.err.println("Error Deploying Topology. Too few arguments. I needed:\n0. Topology Name\n1. ZooKeeper Hosts  e.g. 127.0.0.1\n2. Solr URL e.g. http://127.0.0.1:8983/solr/locations/update/json?commit=true\n3. HBaseTablename\n4. HBaseColumnFamily\n5. Kafka Topic\n6. field0\n...fieldN");
		}
		String tname= args[0];
		BrokerHosts hosts = new ZkHosts(args[1]);
		
		String kafkaTopic= args[5];
		System.out.println("Kafka Topic: "+kafkaTopic);
		
		SpoutConfig spoutConfig = new SpoutConfig(hosts, kafkaTopic, "/kafkastorm","src");
	//	spoutConfig.forceFromStart= true;
		spoutConfig.useStartOffsetTimeIfOffsetOutOfRange= true;
		spoutConfig.startOffsetTime= System.currentTimeMillis();
	
		KafkaSpout kafkaSpout = new KafkaSpout(spoutConfig);
		IndexSolr index= new IndexSolr(args[2]);
		
		int l= args.length- 6;
		String [] keys= new String[l];
		
		for(int i=0; i< l; i++) {
			keys[i]= args[i+6];
			System.out.print("Field: "+keys[i]+" ");
		}
		System.out.println();
			
		List<String> fields = new ArrayList<String>();
		for (int i = 0; i < keys.length; i++) {
			fields.add(keys[i]);
		}
	
		String hbasetable= args[3];
		String columnfamily= args[4];
		
		System.out.println("HBase Table: "+hbasetable);
		System.out.println("HBase CF: "+columnfamily);
		
		Config stormconfig = new Config();

        Map<String, Object> hbConf = new HashMap<String, Object>();
        hbConf.put("hbase.rootdir", "hdfs://sandbox:8020/apps/hbase/data/");
        hbConf.put("zookeeper.znode.parent", "/hbase-unsecure");
        stormconfig.put("hbase.conf", hbConf);
        
		
		SimpleHBaseMapper mapper = new SimpleHBaseMapper() 
        .withRowKeyField(keys[0])
        .withColumnFields(new Fields(fields))
        .withColumnFamily(columnfamily);
		
		HBaseBolt hbolt = new HBaseBolt(hbasetable, mapper).withConfigKey("hbase.conf");
		
		TupleTransformer tt= new TupleTransformer(keys);

		TopologyBuilder builder = new TopologyBuilder();		
		
		builder.setSpout(KAFKA_SPOUT_ID, kafkaSpout);
			
		builder.setBolt(TUPLETRANSFORMER_BOLT_ID, tt).shuffleGrouping(KAFKA_SPOUT_ID);
		
		//builder.setBolt(INDEXSOLR_BOLT_ID, index).shuffleGrouping(TUPLETRANSFORMER_BOLT_ID);
		builder.setBolt(HBASE__BOLT_ID, hbolt).shuffleGrouping(TUPLETRANSFORMER_BOLT_ID);
		
		
		//LocalCluster cluster = new LocalCluster();
		//cluster.submitTopology(TOPOLOGY_NAME, config, builder.createTopology());
		//Utils.waitForSeconds(1000);
		//cluster.killTopology(TOPOLOGY_NAME);
		//cluster.shutdown();
		
		StormSubmitter.submitTopology(tname, stormconfig, builder.createTopology());
	}
}

