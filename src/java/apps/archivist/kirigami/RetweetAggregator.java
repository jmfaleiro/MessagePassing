package apps.archivist.kirigami;

import org.codehaus.jackson.JsonNode;

import mp.*;

public class RetweetAggregator {

	public static void process() {
		int next_tweet = 0;
		String next_tweet_string;
		while (true) {
			
			try {
			ShMem.Acquire(0);
			}
			catch (ShMemObject.MergeException e) {
				e.printStackTrace(System.err);
				System.out.println("Merge failed!");
				System.exit(-1);
			}
			
			JsonNode tweets = ShMem.s_state.get("tweets");
			int old_count = ShMem.s_state.get("retweet-aggregate").getIntValue();
			
			int num_tweets = tweets.size();
			while (next_tweet < num_tweets) {
				next_tweet_string = String.valueOf(next_tweet);
				String tweet_text = tweets.get(next_tweet_string).get("text").getTextValue();
				if (tweet_text.contains("RT @")) {
					old_count += 1;
				}
				next_tweet += 1;
			}
			
			ShMem.s_state.put("retweet-aggregate",  old_count);
			ShMem.Release(0);
		}
	}
	
	public static void main(String [] args) {
		
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		RetweetAggregator.process();
	}
}
