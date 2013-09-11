package archivist;

import org.json.simple.*;

import java.util.*;

import mp.*;

public class UrlAggregator implements IProcess {

	private List<String> get_urls(String tweet_text) {
		
		List<String> ret = new ArrayList<String>();
		String[] parts = tweet_text.split(" ");
		for (String p : parts) {
			if (p.startsWith("HTTP://")) {
				
				ret.add(p.toLowerCase());
			}
		}
		
		return ret;
	}
	
	public void process() {
		
		JSONArray tweets = (JSONArray)ShMem.state.get("tweets");
		ShMemObject vals = (ShMemObject)ShMem.state.get("url-aggregate");
		
		for (Object obj : tweets) {
			
			String tweet_text = new String((String)((JSONObject)obj).get("text"));
			tweet_text = tweet_text.toUpperCase();
			List<String> tweet_urls = get_urls(tweet_text);
			
			for (String url : tweet_urls) {
				
				int count = 0;
				if (vals.containsKey(url)) {
					count = (Integer)vals.get(url);
				}
				
				++count;
				try {
					vals.put(url,  count);
				}
				catch(Exception e) {
					System.exit(-1);
				}
			}
		}
	}
	
	public static void main(String [] args) {
		
		
		IProcess url_agg_proc = new UrlAggregator();
		ShMemAcquirer s = new ShMemAcquirer(url_agg_proc, 2);
		s.start();
	}
}
