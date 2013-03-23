package archivist;

import mp.*;

import org.json.simple.*;

public class SourceAggregator implements IProcess {

	public JSONObject process(JSONArray jobj) {
		
		JSONObject ret = new JSONObject();
		
		int length = jobj.size();
		// We expect that the caller will give us only new tweets. 
		JSONArray tweets = (JSONArray)(((JSONObject)jobj.get(length-1)).get("tweets"));
		JSONObject vals = (JSONObject)((JSONObject)jobj.get(length-2)).get("source-aggregate");
		
		for (Object obj : tweets) {
			
			JSONObject tweet = (JSONObject)obj;
			String source_string = (String)tweet.get("source");
			
			source_string.replaceAll("\\.com/&quot", "\\.com&quot");
			int count = 0;
			if (vals.containsKey(source_string)) {
				count = (Integer)vals.get(source_string);
			}
			++count;
			vals.put(source_string,  count);
		}
		
		ret.put("source-aggregate",  vals);
		return ret;
	}
	
}
