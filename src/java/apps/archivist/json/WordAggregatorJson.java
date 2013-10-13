package apps.archivist.json;

import java.util.*;

import mp.*;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.*;

public class WordAggregatorJson {
	
	

    private static String[] Words = { "rt", "about", "after", "again", "air", "all", "along", 
    						   "also", "an", "and", "another", "any", "are", "around", 
    						   "as", "at", "away", "back", "be", "because", "been", "before", 
    						   "below", "between", "both", "but", "by", "came", "can", "come", 
    						   "could", "day", "did", "different", "do", "does", "don't", "down", 
    						   "each", "end", "even", "every", "few", "find", "first", "for", "found", 
    						   "from", "get", "give", "go", "good", "great", "had", "has", "have", "he", 
    						   "help", "her", "here", "him", "his", "home", "house", "how", "I", "if", "in", 
    						   "into", "is", "it", "its", "just", "know", "large", "last", "left", "like", "line", 
    						   "little", "long", "look", "made", "make", "man", "many", "may", "me", "men", 
    						   "might", "more", "most", "Mr.", "must", "my", "name", "never", "new", "next", 
    						   "no", "not", "now", "number", "of", "off", "old", "on", "one", "only", "or", 
    						   "other", "our", "out", "over", "own", "part", "people", "place", "put", "read", 
    						   "right", "said", "same", "saw", "say", "see", "she", "should", "show", "small",
    						   "so", "some", "something", "sound", "still", "such", "take", "tell", "than", 
    						   "that", "the", "them", "then", "there", "these", "they", "thing", "think", 
    						   "this", "those", "thought", "three", "through", "time", "to", "together", 
    						   "too", "two", "under", "up", "us", "use", "very", "want", "water", "way", 
    						   "we", "well", "went", "were", "what", "when", "where", "which", "while", 
    						   "who", "why", "will", "with", "word", "work", "world", "would", "write", 
    						   "year", "you", "your", "was" };
    
    private static HashSet<String> s_changes = new HashSet<String>();
    
    
    private static List<String> get_excludes(String search_term) {
    	
    	List<String >exclude_words = new ArrayList<String>();
    	
    	for (int i = 0; i < Words.length; ++i) {
    		String upper_word = Words[i].toUpperCase();
    		exclude_words.add(upper_word);
    	}
    	
    	exclude_words.addAll(Arrays.asList(Words));
    	String [] search_parts = search_term.split("\\s|\\+");
    	
    	for (int i = 0; i < search_parts.length; ++i) {
    		exclude_words.add(search_parts[i].toUpperCase());
    	}
    	
    	exclude_words.add(search_term.toUpperCase());
    	return exclude_words;
    }
    
    
	private static void process_tweet(ObjectNode container, 
							   		  String tweet_text,
							   		  List<String> exclude_words) {
    	
    	tweet_text = tweet_text.toUpperCase();
		tweet_text = tweet_text.replace("#", "");
		tweet_text = tweet_text.replace(".", "");
		tweet_text = tweet_text.replace(",", "");
		
		String tweet_pieces[] = tweet_text.split(" ");
		
		for (int i = 0; i < tweet_pieces.length; ++i) {
			
			if (tweet_pieces[i].length() > 1 &&
				!exclude_words.contains(tweet_pieces[i]) &&
				!tweet_pieces[i].startsWith("@") &&
				!tweet_pieces[i].startsWith("HTTP://")) {
				
				JsonNode count_wrapper = container.get(tweet_pieces[i]);
				s_changes.add(tweet_pieces[i]);
				if (count_wrapper != null) {
					container.put(tweet_pieces[i],  count_wrapper.getIntValue()+1);
				}
				else {
					container.put(tweet_pieces[i],  1);
				}
			}
		}
    }
	
	public static void process() {
		
		// Create a new mapper and an object node to hold aggregated values. 
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode vals = mapper.createObjectNode();
		ObjectNode to_send = mapper.createObjectNode();
		
		List<String> exclude_words = null;
		ShMem.ReleasePlain(vals, 0);
		while (true) {
			
			// Get new tweets from the appender process. 
			ObjectNode new_tweet_wrapper = ShMem.AcquirePlain(0);
			ArrayNode tweets = (ArrayNode)new_tweet_wrapper.get("tweets");
			int num_tweets = tweets.size();
			String search_term = new_tweet_wrapper.get("search_term").getTextValue();
			
			if (exclude_words == null) {
				exclude_words = get_excludes(search_term);
			}
			
			for (int i = 0; i < tweets.size(); ++i) {
				JsonNode tweet = tweets.get(i);
				String tweet_text = tweet.get("text").getTextValue();
				process_tweet(vals, tweet_text, exclude_words);
			}
			
			for (String key : s_changes) {
				to_send.put(key,  vals.get(key));
			}
			
			ShMem.ReleasePlain(to_send, 0);
			s_changes.clear();
			to_send.removeAll();
		}
	}
	
	public static void main(String [] args) {
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		WordAggregatorJson.process();
	}
}
