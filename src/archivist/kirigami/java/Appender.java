package archivist.kirigami.java;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import mp.java.*;

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
	
	public int Search() throws TwitterException {
		String next_tweet_string;
		int next_tweet = 0;
		
		ObjectMapper mapper = new ObjectMapper();
		
		ShMemObject tweets = new ShMemObject();
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
				
				ObjectNode cur_tweet = mapper.createObjectNode();
				cur_tweet.put("created_at",  date_string);
				cur_tweet.put("user",  user_string);
				cur_tweet.put("text",  tweet_string);
				cur_tweet.put("source", source_string);
				
				next_tweet_string = String.valueOf(next_tweet);
				tweets.put(next_tweet_string, cur_tweet);
				next_tweet += 1;
			}
			++i;
			
		} while((query = result.nextQuery()) != null);
	
		
		// Add dummy state for volume and user aggregations to use. 
		ShMem.s_state.put("volume-aggregate",  new ShMemObject());
		ShMem.s_state.put("word-aggregate",  new ShMemObject());
		ShMem.s_state.put("user-aggregate",  new ShMemObject());
		ShMem.s_state.put("url-aggregate",  new ShMemObject());
		ShMem.s_state.put("source-aggregate", new ShMemObject());
		ShMem.s_state.put("retweet-aggregate",  0);
		ShMem.s_state.put("tweets", tweets);
		ShMem.s_state.put("search_term",  query_string);
		
		for (i = 1; i < 7; ++i) {
			ShMem.Release(i);
		}
		
		for (i = 1; i < 7; ++i) {
			try {
				ShMem.Acquire(i);
			}
			catch (ShMemObject.MergeException e) {
				e.printStackTrace(System.err);
				System.out.println("Merge failed!");
				System.exit(-1);
			}
		}
		return 0;
	}
	
	
	public static void main(String [] args) throws TwitterException {
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		Appender blah;
		
		blah = new Appender("xbox"); 
		blah.Search();
	}
}
