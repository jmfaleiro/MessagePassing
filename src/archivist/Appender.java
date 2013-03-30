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
		
		JSONObject empty = new JSONObject();
		ShMem.state.put("volume-aggregate",  empty);
		ShMem.state.put("user-aggregate",  empty);
		ShMem.state.put("word-aggregate",  empty);
		ShMem.state.put("url-aggregate",  empty);
		ShMem.state.put("source-aggregate",  empty);
		ShMem.state.put("retweet-aggregate",  0);
		ShMem.state.put("tweets", tweets);
		ShMem.state.put("search_term",  query_string);
		
		ShMem retweet_aggregation = ShMem.fork(0);
		ShMem source_aggregation = ShMem.fork(1);
		ShMem url_aggregation = ShMem.fork(2);
		ShMem user_aggregation = ShMem.fork(3);
		ShMem volume_aggregation = ShMem.fork(4);
		ShMem work_aggregation = ShMem.fork(5); 
		
		retweet_aggregation.join();
		ShMem.state.put("tweets",  tweets);
		
		source_aggregation.join();
		ShMem.state.put("tweets", tweets);

		url_aggregation.join();
		ShMem.state.put("tweets", tweets);
		
		user_aggregation.join();
		ShMem.state.put("tweets", tweets);
		
		volume_aggregation.join();
		ShMem.state.put("tweets", tweets);
		
		work_aggregation.join();
		ShMem.state.put("tweets", tweets);
		
		
		/*
		int len = versioned_state.size();
		JSONObject vol = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("volume-aggregate");
		JSONObject use = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("user-aggregate");
		JSONObject words = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("word-aggregate");
		JSONObject urls = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("url-aggregate");
		JSONObject sources = (JSONObject)((JSONObject)versioned_state.get(len-1)).get("source-aggregate");
		long rt_count = (Long)((JSONObject)versioned_state.get(len-1)).get("retweet-aggregate");
		*/
		return 0;
	}
	
	
	public static void main(String [] args) throws ShMemFailure, ParseException, TwitterException{
		
		Appender blah;
		
		blah = new Appender("xbox"); 
		blah.Search();
	}
}
