package archivist.kirigami.java;

import java.io.*;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import twitter4j.*;
import twitter4j.conf.*;

public class TweetMiner {

	private Twitter m_twitter;
	private String m_query_string;
	private long m_since_id;
	private PrintStream m_output_stream;
	
	public long tweet_count;
	
	private static final int s_pages = 15;
	
	public TweetMiner(String query_string) throws FileNotFoundException {
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true)
		  .setOAuthConsumerKey("Z5lqe8Rf3k9vJ2Kk7XIRw")
		  .setOAuthConsumerSecret("4V4XYAC4Ujp8KlRKh65ZlvSSFQh4aDuiZqxWDo4U")
		  .setOAuthAccessToken("253532484-12uMOisz0lm48KKhxKMIZFp0giMTgsil9E4QihNg")
		  .setOAuthAccessTokenSecret("zbO9LGmYWq32DnuVAnBAGFJfCyUVBtlyI3Mtd6GKxUU");
		
		m_query_string = query_string;
		m_twitter = new TwitterFactory(cb.build()).getInstance();
		m_since_id = 0;
		
		tweet_count = 0;
		m_output_stream = new PrintStream(new FileOutputStream(query_string + ".tweets"), true);
	}
	
	public void Search() throws TwitterException {
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode cur_tweet = mapper.createObjectNode();
		
		Query query = new Query(m_query_string);
		query.setCount(100);
		query.setSinceId(m_since_id);
		QueryResult result;
		
		int i = 0;
		
		do {
			if (i >= s_pages)
				break;
			
			result = m_twitter.search(query);
			List<Status> temp = result.getTweets();
			m_since_id = result.getMaxId()+1;
			
			
			for (Status s : temp) {
				m_since_id = s.getId();
				String date_string = s.getCreatedAt().toString();
				String user_string = s.getUser().getName();
				String tweet_string = s.getText();
				String source_string = s.getSource();
				
				// Get  the data we care about from the tweet. 
				cur_tweet.put("created_at",  date_string);
				cur_tweet.put("user",  user_string);
				cur_tweet.put("text",  tweet_string);
				cur_tweet.put("source", source_string);
				
				// Dump tweet as json string to file and recycle our JsonNode. 
				m_output_stream.println(cur_tweet.toString());
				cur_tweet.removeAll();
				tweet_count += 1;
				
			}
			++i;
			System.out.println("here!");
		} while((query = result.nextQuery()) != null);
		
		m_output_stream.flush();
	}
	
	public void close() {
		m_output_stream.close();
	}
	
	
	public static void main(String[] args) {
		TweetMiner miner = null;
		try{
			miner = new TweetMiner(args[0]);
			while (miner.tweet_count < 10000000) {
				miner.Search();
				Thread.sleep(10*60000);
			}
		}
		catch (Exception e) {
			
		}
		finally {
			if (miner != null) {
				miner.close();
			}
		}
	}
}
