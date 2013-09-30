package archivist.kirigami.java;



import java.util.*;

import org.codehaus.jackson.JsonNode;

import mp.java.*;

public class UrlAggregator {

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
			ShMemObject vals = (ShMemObject)ShMem.s_state.get("url-aggregate");
			int num_tweets = tweets.size();
			
			while (next_tweet != num_tweets) {
				next_tweet_string = String.valueOf(next_tweet);
				JsonNode tweet = tweets.get(next_tweet_string);
				String tweet_text = tweet.get("text").getTextValue().toUpperCase();
				List<String> tweet_urls = get_urls(tweet_text);
				for (String url : tweet_urls) {
					JsonNode count_wrapper = vals.get(url);
					if (count_wrapper != null) {
						vals.put(url,  count_wrapper.getIntValue()+1);
					}
					else {
						vals.put(url,  1);
					}
				}
				next_tweet += 1;
			}
			ShMem.Release(0);
		}
	}
	
	public static void main(String [] args) {
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		UrlAggregator.process();
	}
}
