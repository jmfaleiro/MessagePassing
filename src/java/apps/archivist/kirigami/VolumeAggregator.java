package apps.archivist.kirigami;

import java.util.*;
import java.text.*;

import org.apache.commons.lang3.time.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import mp.*;

public class VolumeAggregator {

		
		
	private static int round_type = Calendar.MINUTE;
	private static SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH);
	
	
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
			
			ArrayNode tweets = (ArrayNode)ShMem.s_state.get("tweets");
			ShMemObject vals = (ShMemObject)ShMem.s_state.get("volume-aggregate");
			int num_tweets = tweets.size();
			
			while (next_tweet != num_tweets) {
				next_tweet_string = String.valueOf(next_tweet);
				JsonNode tweet = tweets.get(next_tweet_string);
				String date_string = tweet.get("created_at").getTextValue();
				Date cur_date = null;
				
				try {
					Date temp = format.parse(date_string);
					cur_date = DateUtils.round(temp,  round_type);
				}
				catch (Exception e) {
					next_tweet += 1;
					continue;
				}
				
				date_string = cur_date.toString();
				JsonNode count_wrapper = vals.get(date_string);
				if (count_wrapper != null) {
					vals.put(date_string,  count_wrapper.getIntValue()+1);
				}
				else {
					vals.put(date_string, 1);
				}
				next_tweet += 1;
			}
			
			ShMem.Release(0);
		}
	}
	

	public static void main(String [] args) {
		
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		VolumeAggregator.process();
	}
}
