package archivist.kirigami.java;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.*;

import mp.java.*;

public class SourceAggregator {

	public static void process() {
		
		int next_tweet = 0;
		String next_tweet_string;
		while (true) {
			
			try {
				ShMem.Acquire(0);
			}
			catch (ShMemObject.MergeException e) {
				System.out.println(e);
				System.out.println("Merge exception!");
				System.exit(-1);
			}
			
			// We expect that the caller will give us only new tweets. 
			ArrayNode tweets = (ArrayNode)ShMem.s_state.get("tweets");
			ShMemObject vals = (ShMemObject)ShMem.s_state.get("source-aggregate"); 
			
			int num_tweets = tweets.size();
			while (next_tweet != num_tweets - 1) {
				next_tweet_string = String.valueOf(next_tweet);
				JsonNode cur_tweet = tweets.get(next_tweet_string);
				String source_string = cur_tweet.get("source").getTextValue();
				
				JsonNode count_wrapper = vals.get(source_string);
				if (count_wrapper != null) {
					vals.put(source_string,  count_wrapper.getIntValue()+1);
				}
				
				else {
					vals.put(source_string, 1);
				}
				
				next_tweet += 1;
			}
			
			ShMem.Release(0);
		}
	}
	
	public static void main(String [] args) {
		
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		SourceAggregator.process();
	}
	
}
