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
				String tweet_string = s.getText();
				String source_string = s.getSource();
				
				JSONObject cur_tweet = new JSONObject();
				cur_tweet.put("created_at",  date_string);
				cur_tweet.put("user",  user_string);
				cur_tweet.put("text",  tweet_string);
				cur_tweet.put("source", source_string);
				tweets.add(cur_tweet);
			}
			++i;
			
		} while((query = result.nextQuery()) != null);
	
		
		// Add dummy state for volume and user aggregations to use. 
		JSONObject first_iteration = new JSONObject();
		JSONObject empty = new JSONObject();
		first_iteration.put("volume-aggregate",  empty);
		first_iteration.put("user-aggregate",  empty);
		first_iteration.put("word-aggregate",  empty);
		first_iteration.put("url-aggregate",  empty);
		first_iteration.put("source-aggregate",  empty);
		first_iteration.put("retweet-aggregate",  0);
		
		JSONObject second_iteration = new JSONObject();
		second_iteration.put("tweets",  tweets);
		second_iteration.put("search_term",  query_string);
		
		JSONArray versioned_state = new JSONArray();
		versioned_state.add(first_iteration);
		versioned_state.add(second_iteration);
		
		ShMemClient volume_aggregation = new ShMemClient(versioned_state, 0);
		volume_aggregation.fork();
		
		ShMemClient user_aggregation = new ShMemClient(versioned_state, 1);
		user_aggregation.fork();
		
		ShMemClient word_aggregation = new ShMemClient(versioned_state, 2);
		word_aggregation.fork();
		
		ShMemClient url_aggregation = new ShMemClient(versioned_state, 3);
		url_aggregation.fork();
		
		ShMemClient source_aggregation = new ShMemClient(versioned_state, 4);
		source_aggregation.fork();
		
		ShMemClient retweet_aggregation = new ShMemClient(versioned_state, 5);
		retweet_aggregation.fork();
		
		volume_aggregation.merge(versioned_state);
		user_aggregation.merge(versioned_state);
		word_aggregation.merge(versioned_state);
		url_aggregation.merge(versioned_state);
		source_aggregation.merge(versioned_state);
		retweet_aggregation.merge(versioned_state);
		
		int len = versioned_state.size();
		JSONObject vol = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("volume-aggregate");
		JSONObject use = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("user-aggregate");
		JSONObject words = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("word-aggregate");
		JSONObject urls = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("url-aggregate");
		JSONObject sources = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("source-aggregate");
		long rt_count = (Long)((JSONObject)versioned_state.get(len-1)).get("retweet-aggregate");
		return 0;
	}
	
	
	public static void main(String [] args) {
		
		Appender blah;
		IProcess volume_proc = new VolumeAggregator();
		IProcess user_proc = new UserAggregator();
		IProcess word_proc = new WordAggregator();
		IProcess url_proc = new UrlAggregator();
		IProcess source_proc = new SourceAggregator();
		IProcess retweet_proc = new RetweetAggregator();
		
		@SuppressWarnings("unused")
		Aggregator volume_service = new Aggregator(0, volume_proc);
		
		@SuppressWarnings("unused")
		Aggregator user_service = new Aggregator(1, user_proc);
		
		Aggregator word_service = new Aggregator(2, word_proc);
		Aggregator url_service = new Aggregator(3, url_proc);
		Aggregator source_service = new Aggregator(4, source_proc);
		Aggregator retweet_service = new Aggregator(5, retweet_proc);
		
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
