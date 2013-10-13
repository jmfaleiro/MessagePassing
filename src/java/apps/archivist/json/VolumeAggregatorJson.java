package apps.archivist.json;

import java.util.*;
import java.text.*;

import org.apache.commons.lang3.time.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.*;

import mp.*;

public class VolumeAggregatorJson {
		
	private static int round_type = Calendar.MINUTE;
	private static SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy", Locale.ENGLISH);
	
	
	public static void process() {
		
		// Create two object nodes, one to keep track of volume counts,
		// the other to send newly computed counts to the master every
		// epoch. 
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode vals = mapper.createObjectNode();
		ObjectNode to_send = mapper.createObjectNode();
		HashSet<String> epoch_changes = new HashSet<String>();
		
		while (true) {
			ObjectNode new_tweet_wrapper = ShMem.AcquirePlain(0);
			JsonNode tweets = new_tweet_wrapper.get("tweets");
			int num_tweets = tweets.size();
			
			for (int i = 0; i < num_tweets; ++i) {
				JsonNode tweet = tweets.get(i);
				String date_string = tweet.get("created_at").getTextValue();
				Date cur_date = null;
				
				try {
					Date temp = format.parse(date_string);
					cur_date = DateUtils.round(temp, round_type);
				}
				catch (Exception e) {
					continue;
				}
				
				date_string = cur_date.toString();
				JsonNode count_wrapper = vals.get(date_string);
				epoch_changes.add(date_string);
				if (count_wrapper != null) {
					vals.put(date_string, count_wrapper.getIntValue()+1);
				}
				else {
					vals.put(date_string, 1);
				}
			}
			
			for (String date_string : epoch_changes) {
				to_send.put(date_string,  vals.get(date_string));
			}
			ShMem.ReleasePlain(to_send, 0);
			to_send.removeAll();
			epoch_changes.clear();
		}
	}
	

	public static void main(String [] args) {
		
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		VolumeAggregatorJson.process();
	}
}
