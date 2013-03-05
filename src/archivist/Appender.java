package archivist;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.*;

public class Appender {

	
	
	private Query query;
	private static final String key = "Z5lqe8Rf3k9vJ2Kk7XIRw";
	private static final String secret = "4V4XYAC4Ujp8KlRKh65ZlvSSFQh4aDuiZqxWDo4U";
	
	
	private Twitter twitter;
	
	public Appender(String query_string) { 
		
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey("Z5lqe8Rf3k9vJ2Kk7XIRw")
		  .setOAuthConsumerSecret("4V4XYAC4Ujp8KlRKh65ZlvSSFQh4aDuiZqxWDo4U")
		  .setOAuthAccessToken("253532484-12uMOisz0lm48KKhxKMIZFp0giMTgsil9E4QihNg")
		  .setOAuthAccessTokenSecret("zbO9LGmYWq32DnuVAnBAGFJfCyUVBtlyI3Mtd6GKxUU");
		
		query = new Query(query_string);
		
		
		twitter = new TwitterFactory(cb.build()).getInstance();
		
		
	}
	
	public int Search() throws TwitterException{
		
		QueryResult result = twitter.search(query);
		List<Status> temp = result.getTweets();
		int count = 0;
		
		for (Status s : temp) {
			
			System.out.println(s.getText());
			System.out.println(count++);
		}
		
		return count;
	}
	
	
	public static void main(String [] args) {
		
		Appender blah;
		
		try {
			System.out.println("blah");
			blah = new Appender("xbox");
			blah.Search();
		}
		catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to search tweets: " + te.getMessage());
            System.exit(-1);
		}
	}
	
	
}
