package archivist;

import java.util.*;
import java.text.*;

import org.json.simple.*;

import org.apache.commons.lang3.time.*;

import mp.*;

public class VolumeAggregator implements IProcess{

		
		
	private static int round_type = Calendar.MINUTE;
	private static SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH);
	
	@SuppressWarnings("unchecked")
	@Override
	public void process() {
		
		// We expect that the caller will give us only new tweets. 
		JSONArray tweets = (JSONArray)ShMem.state.get("tweets");
		ShMemObject vals = (ShMemObject)ShMem.state.get("volume-aggregate");
		
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
				count = (java.lang.Integer)vals.get(date_string);
			}
			
			// Increment count and add it to the dictionary.
			++count;
			try {
				vals.put(date_string,  count);
			}
			catch (Exception e) {
				System.exit(-1);
			}
		}
	}

	public static void main(String [] args) {
		
	
		IProcess vol_agg_proc = new VolumeAggregator();
		ShMemServer s = new ShMemServer(vol_agg_proc, 4);
		s.start();
	}
}
