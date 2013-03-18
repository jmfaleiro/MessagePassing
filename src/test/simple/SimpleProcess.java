package test.simple;

import org.json.simple.JSONArray;

import mp.*;


// 
// Super simple process that just returns arguments. Use this to ensure
// that JSON serialization and client/server code works properly. 
// 
public class SimpleProcess implements IProcess{

	// Just return the argument. 
	public JSONArray process(JSONArray jobj) {
		
		return jobj;
	}

}
