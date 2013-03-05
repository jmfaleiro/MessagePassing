package archivist;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import java.io.*;

import org.apache.commons.lang3.time.DateUtils;
import org.json.simple.*;

public abstract class Aggregator {

	private mp.NodeNew node;
	
	private static IUtils utils;
	
	private String file_name;
	
	public abstract JSONObject Aggregate(JSONArray tweets, JSONObject dict);
	
	public Aggregator(int node_id, int total_nodes) {
		
		node = new mp.NodeNew(3, 1);
	}
	
	public void Process() throws IOException {
		
		// Read the old dictionary of values into memory
		// TODO: Read the contents of the old dictionary from the file. 
		/*
		String fileContents = utils.readFile(file_name);
		Object old = JSONValue.parse(fileContents);
		JSONObject vals = (JSONObject) old;
		*/
		
		JSONObject vals = new JSONObject();
		
		JSONObject tweet_object = node.receiveMessage(0);
		JSONArray tweets = (JSONArray)tweet_object.get("Message");
		
		JSONObject new_vals = Aggregate(tweets, vals);
									
		System.out.println(new_vals);
		
		node.sendMessage(2,  new_vals);
	}
}
