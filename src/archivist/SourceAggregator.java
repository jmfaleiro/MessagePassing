package archivist;

import mp.*;

import org.json.simple.*;

public class SourceAggregator implements IProcess {

	public void process() {
		
		// We expect that the caller will give us only new tweets. 
		JSONArray tweets = (JSONArray)ShMem.state.get("tweets");
		ShMemObject vals = (ShMemObject)ShMem.state.get("source-aggregate"); 
		
		for (Object obj : tweets) {
			
			JSONObject tweet = (JSONObject)obj;
			String source_string = new String((String)tweet.get("source"));
			
			source_string.replaceAll("\\.com/&quot", "\\.com&quot");
			int count = 0;
			if (vals.containsKey(source_string)) {
				count = (Integer)vals.get(source_string);
			}
			++count;
			try {
				vals.put(source_string,  count);
			}
			catch(Exception e) {
				System.exit(-1);
			}
		}
	}
	
	public static void main(String [] args) {
		
	
		IProcess source_agg_proc = new SourceAggregator();
		ShMemAcquirer s = new ShMemAcquirer(source_agg_proc, 1);
		s.start();
	}
	
}
