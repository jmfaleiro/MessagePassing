package archivist;

import java.util.*;

import mp.*;

import org.json.simple.*;

public class WordAggregator implements IProcess{
	
	

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
    
    
    private List<String> get_excludes(String search_term) {
    	
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
    
    @SuppressWarnings("unchecked")
	private void process_tweet(JSONObject container, 
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
				
				int count = 0;
				if (container.containsKey(tweet_pieces[i])) {
					count = (Integer)container.get(tweet_pieces[i]);
				}
				++count;
				
				container.put(tweet_pieces[i],  count);
			}
		}
    }
	
	@SuppressWarnings("unchecked")
	@Override
	public void process() {
		
		String search_term = (String)ShMem.state.get("search_term");
		// We expect that the caller will give us only new tweets. 
		JSONArray tweets = (JSONArray)ShMem.state.get("tweets");
		JSONObject vals = (JSONObject)ShMem.state.get("word-aggregate");
		
		List<String> exclude_words = get_excludes(search_term);
		
		
		for (Object obj : tweets) {
			
			// Extract tweet text and sanitize it. 
			JSONObject tweet = (JSONObject)obj;
			String tweet_text = (String)tweet.get("text");
			process_tweet(vals, tweet_text, exclude_words);
		}
	}
	
	public static void main(String [] args) {
		
		IProcess word_agg_proc = new WordAggregator();
		ShMemServer s = new ShMemServer(word_agg_proc, 5);
		s.start();
	}
}
