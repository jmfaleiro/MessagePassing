package archivist;

import org.json.simple.*;

import mp.*;

public class RetweetAggregator implements IProcess{

	public JSONObject process(JSONArray jobj) {
		
		int length = jobj.size();
		// We expect that the caller will give us only new tweets. 
		JSONArray tweets = (JSONArray)(((JSONObject)jobj.get(length-1)).get("tweets"));
		long old_count = (Long)((JSONObject)jobj.get(length-2)).get("retweet-aggregate");
		long count = 0;
		
		for (Object obj : tweets) {
			
			String tweet_text = (String)((JSONObject)obj).get("text");
			if (tweet_text.toUpperCase().contains("RT @"))
				++count;
		}
		
		count += old_count;
		
		JSONObject ret = new JSONObject();
		ret.put("retweet-aggregate",  count);
		
		return ret;
	}
}
