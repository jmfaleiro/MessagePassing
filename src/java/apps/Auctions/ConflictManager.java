// Author: Jose Faleiro (faleiro.jose.manuel@gmail.com)
// 
package apps.Auctions;

import org.codehaus.jackson.JsonNode;
import mp.*;

public class ConflictManager {

	private void BidConflict(ShMemObject my_bid, JsonNode other_bid) {
		
		// Compare the values of the two bids. 
		double my_value = my_bid.get("value").getDoubleValue();
		double other_value = other_bid.get("value").getDoubleValue();
		
		// If the other value is larger than ours, than that bid should be the
		// largest seen yet. 
		if (other_value > my_value) {
			my_bid.put("value",  other_value);
			my_bid.put("bid_id", other_bid.get("bid_id").getLongValue());
		}
	}
}
