package apps.archivist.json;



import java.util.*;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.*;

import mp.*;

public class UrlAggregatorJson {

	private static List<String> get_urls(String tweet_text) {
		
		List<String> ret = new ArrayList<String>();
		String[] parts = tweet_text.split(" ");
		for (String p : parts) {
			if (p.startsWith("HTTP://")) {
				
				ret.add(p.toLowerCase());
			}
		}
		
		return ret;
	}
	
	public static void process() {
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode vals = mapper.createObjectNode();
		ObjectNode to_send = mapper.createObjectNode(); 
		
		HashSet<String> changed_keys = new HashSet<String>();
		
		while (true) {
			
			JsonNode new_tweets = ShMem.AcquirePlain(0);
			JsonNode tweets = new_tweets.get("tweets");
			int num_tweets = tweets.size();
			
			for (int i = 0; i < num_tweets; ++i) {
				JsonNode tweet = tweets.get(i);
				String tweet_text = tweet.get("text").getTextValue().toUpperCase();
				List<String> tweet_urls = get_urls(tweet_text);
				
				for (String url : tweet_urls) {
					changed_keys.add(url);
					JsonNode count_wrapper = vals.get(url);
					if (count_wrapper != null) {
						vals.put(url, count_wrapper.getIntValue()+1);
					}
					else {
						vals.put(url, 1);
					}
				}
			}
			
			for (String key : changed_keys) {
				to_send.put(key,  vals.get(key));
			}
			ShMem.ReleasePlain(to_send, 0);
			to_send.removeAll();
			changed_keys.clear();
		}
	}
	
	public static void main(String [] args) {
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		UrlAggregatorJson.process();
	}
}
