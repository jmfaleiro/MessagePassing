package archivist;

import mp.*;

import org.json.simple.*;

public class SourceAggregator implements IProcess {

	public void process() {
		
		// We expect that the caller will give us only new tweets. 
		JSONArray tweets = (JSONArray)ShMem.state.get("tweets");
		JSONObject vals = (JSONObject)ShMem.state.get("source-aggregate");
		
		for (Object obj : tweets) {
			
			JSONObject tweet = (JSONObject)obj;
			String source_string = new String((String)tweet.get("source"));
			
			source_string.replaceAll("\\.com/&quot", "\\.com&quot");
			int count = 0;
			if (vals.containsKey(source_string)) {
				count = (Integer)vals.get(source_string);
			}
			++count;
			vals.put(source_string,  count);
		}
	}
	
	public static void main(String [] args) {
		
	
		IProcess source_agg_proc = new SourceAggregator();
		ShMemServer s = new ShMemServer(source_agg_proc, 1);
		s.start();
	}
	
}
