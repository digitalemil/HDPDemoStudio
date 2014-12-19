package com.hortonworks.digitalemil.hdpappstudio.storm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.*;

import backtype.storm.coordination.BatchOutputCollector;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.*;

public class TupleTransformer extends BaseRichBolt {
	OutputCollector col;
	String[] definedKeys;

	public boolean keyContained(String key) {
		if (key == null)
			return false;
		for (int i = 0; i < definedKeys.length; i++) {
			if (definedKeys[i] == null)
				continue;
			if (definedKeys[i].equals(key)) {
				return true;
			}
		}
		return false;
	}
	
	public String transform(String in) {
		return in;
	}

	public void transformTuple(Tuple tuple) {
		// Extract the data from Kafka and construct JSON doc
		Fields fields = tuple.getFields();
		String data = new String((byte[]) tuple.getValueByField(fields.get(0)));
		System.out.println("Tuple Key: " + fields.get(0));
		System.out.println("Tuple Value: " + data);

		JSONObject json = transform(new JSONObject(new JSONTokener(data)));
		
		
		Values val = new Values();

		String values[] = new String[definedKeys.length];

		for (int i = 0; i < definedKeys.length; i++) {
			try {
				values[i] = json.getString(definedKeys[i]);
				val.add(i, transform(values[i]));
				System.out.println("Key: " + definedKeys[i] + " value: "
						+ values[i]);
			} catch (JSONException ex) {
				val.add(i, "null");
			}
		}

		col.emit(tuple, val);
		col.ack(tuple);
		System.out.println("Emitting values: " + val);
	}

	protected JSONObject transform(JSONObject jsonObject) {
		return jsonObject;
	}

	public TupleTransformer(String[] definedKeys) {
		this.definedKeys = definedKeys;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void execute(Tuple tuple) {
		try {
			transformTuple(tuple);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void prepare(Map map, TopologyContext ctx, OutputCollector col) {
		this.col = col;
	}

	public void declareOutputFields(OutputFieldsDeclarer decl) {
		List<String> fields = new ArrayList<String>();
		for (int i = 0; i < definedKeys.length; i++) {
			fields.add(definedKeys[i]);
		}
		decl.declare(new Fields(fields));
	}

	public void prepare(Map conf, TopologyContext context,
			BatchOutputCollector collector, Object id) {
		// TODO Auto-generated method stub	
	}

	public void finishBatch() {
		// TODO Auto-generated method stub
		
	}
}
