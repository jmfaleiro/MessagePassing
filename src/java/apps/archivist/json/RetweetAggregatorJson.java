package apps.archivist.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.*;

import mp.*;

public class RetweetAggregatorJson {

	public static void process() {
		int old_count = 0;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode to_send = mapper.createObjectNode();
		
		while (true) {
			
			ObjectNode new_tweet_wrapper = ShMem.AcquirePlain(0);
			JsonNode tweets = new_tweet_wrapper.get("tweets");
			int tweet_count = tweets.size();
			for (int i = 0; i < tweet_count; ++i) {
				String tweet_text = tweets.get(i).get("text").getTextValue();
				if (tweet_text.contains("RT @")) {
					old_count += 1;
				}
			}
			
			to_send.put("retweet-aggregate",  old_count);
			ShMem.ReleasePlain(to_send,  0);
		}
	}
	
	public static void main(String [] args) {
		
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		RetweetAggregatorJson.process();
	}
}
