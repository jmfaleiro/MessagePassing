package archivist;

import org.json.simple.*;

import mp.*;

public class UserAggregator implements IProcess {

	@SuppressWarnings("unchecked")
	@Override
	public void process() {
		
	
		// We expect that the caller will give us only new tweets. 
		JSONArray tweets = (JSONArray)ShMem.state.get("tweets");
		ShMemObject vals = (ShMemObject)ShMem.state.get("user-aggregate");
		
		for (Object obj : tweets) {
			
			// Extract the date from the tweet
			JSONObject tweet = (JSONObject)obj;
			String username = (String)tweet.get("user");
			
			int count = 0;
			
			// If the date is already in the dictionary, then get its value.
			if (vals.containsKey(username)) {
				count = (java.lang.Integer)vals.get(username);
			}
			
			// Increment count and add it to the dictionary.
			++count;
			try {
			vals.put(username, count);
			}
			catch(Exception e) {
				System.exit(-1);
			}
		}
	}
	
	public static void main(String [] args) {
		
		
		IProcess user_agg_proc = new UserAggregator();
		ShMemServer s = new ShMemServer(user_agg_proc, 3);
		s.start();
	}
}
