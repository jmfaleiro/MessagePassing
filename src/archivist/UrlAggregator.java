package archivist;

import org.json.simple.*;

import java.util.*;

import mp.*;

public class UrlAggregator implements IProcess {

	private List<String> get_urls(String tweet_text) {
		
		List<String> ret = new ArrayList<String>();
		String[] parts = tweet_text.split(" ");
		for (String p : parts) {
			if (p.startsWith("HTTP://")) {
				
				ret.add(p.toLowerCase());
			}
		}
		
		return ret;
	}
	
	public JSONObject process(JSONArray jobj) {
		
		JSONObject ret = new JSONObject();
		int length = jobj.size();
		JSONArray tweets = (JSONArray)(((JSONObject)jobj.get(length-1)).get("tweets"));
		JSONObject vals = (JSONObject)((JSONObject)jobj.get(length-2)).get("url-aggregate");
		
		for (Object obj : tweets) {
			
			String tweet_text = (String)((JSONObject)obj).get("text");
			tweet_text = tweet_text.toUpperCase();
			List<String> tweet_urls = get_urls(tweet_text);
			
			for (String url : tweet_urls) {
				
				int count = 0;
				if (vals.containsKey(url)) {
					count = (Integer)vals.get(url);
				}
				
				++count;
				vals.put(url,  count);
			}
		}
		
		ret.put("url-aggregate",  vals);
		return ret;
	}
}
