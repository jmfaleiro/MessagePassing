package archivist;

import java.util.*;
import java.text.*;

import org.json.simple.*;

import org.apache.commons.lang3.time.*;

public class VolumeAggregator extends Aggregator{

		
		
		private static int round_type = Calendar.HOUR;
		private static SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH);
		
		public VolumeAggregator (int node_id) {
			
			super(node_id);
		}
		
		
		public JSONObject Aggregate(JSONArray tweets, JSONObject vals) {
			
			// Iterate through the new tweets and update the data in the dictionary
			for (Object obj : tweets) {
				
				// Extract the date from the tweet
				JSONObject tweet = (JSONObject)obj;
				String date_string = (String)tweet.get("created_at");
				Date cur_date = null;
				
				try {
					
					// Get the date and round it down to what we want.
					Date temp = format.parse(date_string);
					cur_date = DateUtils.round(temp, round_type);
				}
				catch (Exception e) {
					
				}
				
				date_string = cur_date.toString();
				int count = 0;
				
				// If the date is already in the dictionary, then get its value.
				if (vals.containsKey(date_string)) {
					count = (java.lang.Integer)vals.get("date_string");
				}
				
				// Increment count and add it to the dictionary.
				++count;
				vals.put(date_string,  count);
			}
			
			return vals;
		}
	
}
