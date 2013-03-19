package archivist;

import org.json.simple.*;

import mp.*;

public class UserAggregator implements IProcess {

	
	public JSONObject Aggregate(JSONArray tweets, JSONObject dict) {
		
		return dict;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JSONObject process(JSONArray jobj) {
		
		int length = jobj.size();
		// We expect that the caller will give us only new tweets. 
		JSONArray tweets = (JSONArray)(((JSONObject)jobj.get(length-1)).get("tweets"));
		JSONObject vals = (JSONObject)((JSONObject)jobj.get(length-2)).get("user-aggregate");
		
		for (Object obj : tweets) {
			
			// Extract the date from the tweet
			JSONObject tweet = (JSONObject)obj;
			String username = (String)tweet.get("user");
			
			int count = 0;
			
			// If the date is already in the dictionary, then get its value.
			if (vals.containsKey(username)) {
				count = (java.lang.Integer)vals.get(username);
			}
			
			// Increment count and add it to the dictionary.
			++count;
			vals.put(username, count);
		}
		
		JSONObject ret = new JSONObject();
		ret.put("user-aggregate",  vals);
		
		return ret;
	}
}
