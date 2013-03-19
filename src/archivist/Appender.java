package archivist;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.*;

import org.json.simple.*;
import org.json.simple.parser.*;

import mp.*;

public class Appender {
	
	private static int pages = 15;
	
	private long since_id = 0;
	private String query_string;
	
	private Twitter twitter;
	
	public Appender(String query_string) { 
		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey("Z5lqe8Rf3k9vJ2Kk7XIRw")
		  .setOAuthConsumerSecret("4V4XYAC4Ujp8KlRKh65ZlvSSFQh4aDuiZqxWDo4U")
		  .setOAuthAccessToken("253532484-12uMOisz0lm48KKhxKMIZFp0giMTgsil9E4QihNg")
		  .setOAuthAccessTokenSecret("zbO9LGmYWq32DnuVAnBAGFJfCyUVBtlyI3Mtd6GKxUU");
		
		this.query_string = query_string;
		
		twitter = new TwitterFactory(cb.build()).getInstance();
		
		
	}
	
	@SuppressWarnings("unchecked")
	public int Search() throws TwitterException, ParseException, ShMemFailure {
		
		
		JSONArray tweets = new JSONArray();
		
		Query query = new Query(query_string);
		query.setCount(100);
		query.setSinceId(this.since_id);
		QueryResult result;
		
		int i = 0;
		
		do {
			if (i >= pages)
				break;
			
			result = twitter.search(query);
			List<Status> temp = result.getTweets();
			this.since_id = result.getMaxId()+1;
			
			
			for (Status s : temp) {
				
				this.since_id = s.getId();
				String date_string = s.getCreatedAt().toString();
				String user_string = s.getUser().getName();
				
				
				JSONObject cur_tweet = new JSONObject();
				cur_tweet.put("created_at",  date_string);
				cur_tweet.put("user",  user_string);
				tweets.add(cur_tweet);
			}
			++i;
			
		} while((query = result.nextQuery()) != null);
	
		
		// Add dummy state for volume and user aggregations to use. 
		JSONObject first_iteration = new JSONObject();
		JSONObject empty = new JSONObject();
		first_iteration.put("volume-aggregate",  empty);
		first_iteration.put("user-aggregate",  empty);
		
		
		JSONObject second_iteration = new JSONObject();
		second_iteration.put("tweets",  tweets);
		
		JSONArray versioned_state = new JSONArray();
		versioned_state.add(first_iteration);
		versioned_state.add(second_iteration);
		
		ShMemClient volume_aggregation = new ShMemClient(versioned_state, 0);
		volume_aggregation.fork();
		
		ShMemClient user_aggregation = new ShMemClient(versioned_state, 1);
		user_aggregation.fork();
		
		versioned_state = volume_aggregation.merge(versioned_state);
		versioned_state = user_aggregation.merge(versioned_state);
		
		int len = versioned_state.size();
		JSONObject vol = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("volume-aggregate");
		JSONObject use = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("user-aggregate");
		
		return 0;
	}
	
	
	public static void main(String [] args) {
		
		Appender blah;
		IProcess volume_proc = new VolumeAggregator();
		IProcess user_proc = new UserAggregator();
		
		@SuppressWarnings("unused")
		Aggregator volume_service = new Aggregator(0, volume_proc);
		
		@SuppressWarnings("unused")
		Aggregator user_service = new Aggregator(1, user_proc);
		
		try {
			System.out.println("blah");
			blah = new Appender("xbox");
			blah.Search();
		}
		catch (Exception te) {
            te.printStackTrace();
            System.out.println("Failed to search tweets: " + te.getMessage());
            System.exit(-1);
		}
	}
	
	
}
