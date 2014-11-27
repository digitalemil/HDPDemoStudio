package com.hortonworks.digitalemil.hdpappstudio.storm;

import backtype.storm.tuple.Tuple;

public class RecordFormat implements org.apache.storm.hdfs.bolt.format.RecordFormat {

	public byte[] format(Tuple tuple) {
		return (((new String((byte[]) tuple.getValueByField(tuple.getFields().get(0)))))+"/n").getBytes();
	}

}
