package apps.archivist.kirigami;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import mp.*;

public class UserAggregator {
	
	public static void process() {
		int next_tweet = 0;
		String next_tweet_string;
		while (true) {
			
			try {
				ShMem.Acquire(0);
			}
			catch (ShMemObject.MergeException e) {
				System.out.println("Merge failed!");
				e.printStackTrace(System.err);
				System.exit(-1);
			}
			
			// We expect that the caller will give us only new tweets. 
			ArrayNode tweets = (ArrayNode)ShMem.s_state.get("tweets");
			ShMemObject vals = (ShMemObject)ShMem.s_state.get("user-aggregate");
			
			int tweet_count = tweets.size();
			while (next_tweet != tweet_count) {
				next_tweet_string = String.valueOf(next_tweet);
				JsonNode tweet = tweets.get(next_tweet_string);
				String username = tweet.get("user").getTextValue();
				
				JsonNode count_wrapper = vals.get(username);
				if (count_wrapper != null) {
					vals.put(username,  count_wrapper.getIntValue()+1);
				}
				else {
					vals.put(username, 1);
				}
				
				next_tweet += 1;
			}
			ShMem.Release(0);
		}
	}
	
	public static void main(String [] args) {
		
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		UserAggregator.process();
	}
}
