package apps.archivist.json;

import java.util.HashSet;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.*;

import mp.*;

public class SourceAggregatorJson {

	public static void process() {
		
		ObjectMapper mapper = new ObjectMapper();
		int next_tweet = 0;
		String next_tweet_string;
		HashSet<String> changes = new HashSet<String>();
		
		// source_counts is a dictionary of source counts, while to_send
		// contains data we want to send to a remote node. 
		ObjectNode source_counts = mapper.createObjectNode();
		ObjectNode to_send = mapper.createObjectNode();
		
		while (true) {
			
			// Acquire new tweets from the master. 
			ObjectNode recvd_object = ShMem.AcquirePlain(0);
			ArrayNode tweets = (ArrayNode)recvd_object.get("tweets");
			int num_tweets = tweets.size();
			
			for (int i = 0; i < num_tweets; ++i) {
				JsonNode cur_tweet = tweets.get(i);
				String source_string = cur_tweet.get("source").getTextValue();
				JsonNode count_wrapper = source_counts.get(source_string);
				changes.add(source_string);
				if (count_wrapper != null) {
					source_counts.put(source_string,  count_wrapper.getIntValue()+1);
				}
				else {
					source_counts.put(source_string, 1);
				}
			}
			
			// Add all the changes to the object to send out. 
			for (String source : changes) {
				JsonNode new_value = source_counts.get(source);
				to_send.put(source,  new_value);
			}
			
			// Send the changes, clear the set of changes, and clear the objects
			// to send. 
			ShMem.ReleasePlain(to_send, 0);
			changes.clear();
			to_send.removeAll();
		}
	}
	
	public static void main(String [] args) {
		
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		SourceAggregatorJson.process();
	}
	
}
