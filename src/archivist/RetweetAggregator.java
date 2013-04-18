package archivist;

import org.json.simple.*;

import mp.*;

public class RetweetAggregator implements IProcess {

	public void process() {
		
		// We expect that the caller will give us only new tweets. 
		JSONArray tweets = (JSONArray)ShMem.state.get("tweets");
		long old_count = (Long)ShMem.state.get("retweet-aggregate");
		long count = 0;
		
		for (Object obj : tweets) {
			
			String tweet_text = (String)((JSONObject)obj).get("text");
			if (tweet_text.toUpperCase().contains("RT @"))
				++count;
		}
		
		count += old_count;
		try {
			ShMem.state.put("retweet-aggregate",  count);
		}
		catch(Exception e) {
			System.exit(-1);
		}
	}
	
	public static void main(String [] args) {
		
		
		IProcess rt_agg_proc = new RetweetAggregator();
		ShMemServer s = new ShMemServer(rt_agg_proc, 0);
		s.start();
	}
}
