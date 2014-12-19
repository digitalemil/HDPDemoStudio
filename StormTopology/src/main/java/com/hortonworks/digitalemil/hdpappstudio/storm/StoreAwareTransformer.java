package com.hortonworks.digitalemil.hdpappstudio.storm;

import org.json.JSONObject;

public class StoreAwareTransformer extends TupleTransformer {
	public final static int keys[]= {0, 1, 2, 3, 4, 5, 6, 7, 8};
	public final static String locations[]= {"0.0", "37.775,-122.4183333", "37.4419444,-122.1419444", "43.479929,-110.762428", "32.7152778,-117.1563889", "41.850033,-87.6500523",
		"42.3584308,-71.0597732", "34.0522342,-118.2436849", "41.9015141,12.4607737", "51.536086,-0.131836", "55.751849,37.573242", "48.864715,2.329102" };
	public final static String cities[]= { "Nowhere", "San Francisco", "Palo Alto", "SanDiego", "Jackson", "Chicago", "Boston", "Los Angeles", "Rome", "London", "Moscow", "Paris" };
		

	public StoreAwareTransformer(String[] definedKeys) {
		super(definedKeys);
	}

	@Override
	protected JSONObject transform(JSONObject json) {
		int storekey = 0;
		try {
			Integer.parseInt(json.get("StoreKey").toString());
		} catch (Exception e) {
		}
		if(storekey>= 0 && storekey< locations.length) {
			json.remove("location");
			json.append("location", locations[storekey]);
		}
		return json;
	}

}
