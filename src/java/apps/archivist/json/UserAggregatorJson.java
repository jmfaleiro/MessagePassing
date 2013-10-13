package apps.archivist.json;

import java.util.HashSet;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.*;

import mp.*;

public class UserAggregatorJson {
	
	public static void process() {
		int next_tweet = 0;
		String next_tweet_string;
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode vals = mapper.createObjectNode();
		ObjectNode to_send = mapper.createObjectNode();
		HashSet<String> new_users = new HashSet<String>();
		
		while (true) {
			
			JsonNode tweet_wrapper = ShMem.AcquirePlain(0);
			ArrayNode tweets = (ArrayNode)tweet_wrapper.get("tweets");
			int tweet_count = tweets.size();
			for (int i = 0; i < tweet_count; ++i) {
				JsonNode tweet = tweets.get(i);
				String username = tweet.get("user").getTextValue();
				JsonNode count_wrapper = vals.get(username);
				new_users.add(username);
				if (count_wrapper != null) {
					vals.put(username, count_wrapper.getIntValue() + 1);
				}
				else {
					vals.put(username, 1);
				}
				
				next_tweet += 1;
			}
			
			for (String username : new_users) {
				to_send.put(username,  vals.get(username));
			}
			
			ShMem.ReleasePlain(to_send, 0);
			to_send.removeAll();
			new_users.clear();
		}
	}
	
	public static void main(String [] args) {
		
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		UserAggregatorJson.process();
	}
}
