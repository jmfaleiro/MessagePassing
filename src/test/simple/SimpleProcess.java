package test.simple;

import org.json.simple.*;

import mp.*;


// 
// Super simple process that just returns arguments. Use this to ensure
// that JSON serialization and client/server code works properly. 
// 
public class SimpleProcess implements IProcess{

	// Just return the argument. 
	@SuppressWarnings("unchecked")
	public JSONObject process(JSONArray jobj) {
		
		JSONObject ret = new JSONObject();
		ret.put("this",  "works");
		return ret;
	}

}
