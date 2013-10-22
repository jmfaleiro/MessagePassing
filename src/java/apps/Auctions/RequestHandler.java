package apps.Auctions;
import org.codehaus.jackson.JsonNode;

import mp.*;


//
// Auction schema: 
// 				\	value
//				\	bid_id
//				\	close_time
//				\ 	finished
// 
public class RequestHandler {
	
	private final ShMemObject auction_state;
	
	public RequestHandler(int node_id) {
		ShMem.Init(node_id);
		ShMem.Start();
		ShMemObject auctions = new ShMemObject();
		ShMem.s_state.put("auctions", auctions);
		auction_state = auctions;
	}
	
	private void HandleAuction(Auction cur_auction) {
		
		// Make sure that the current auction does not exist. 
		String auction_id = cur_auction.m_auction_id;
		
		// For now, silently fail if the auction already exists. 
		if (auction_state.get(auction_id) == null) {
			ShMemObject new_auction = new ShMemObject();
			new_auction.put("close_time",  cur_auction.m_close_time);
			new_auction.put("finished", false);
			auction_state.put(auction_id,  new_auction);
		}
	}
	
	private void HandleBid(Bid cur_bid) {
		String auction_id = cur_bid.m_auction_id;
		JsonNode cur_auction = null;
		
		// For now, just silently fail if the auction does not exist. 
		if ((cur_auction = auction_state.get(auction_id)) != null) {
			ShMemObject auction_obj = (ShMemObject)cur_auction;
			
			// First check if the auction is already done. 
			if (!auction_obj.get("finished").getBooleanValue()) {
				
				// If the bid time is past the auction validity time, mark the auction as 
				// finished. 
				if (cur_bid.
						m_bid_time.
						compareTo(auction_obj.get("close_time").getTextValue()) > 0) {
					auction_obj.put("finished", true);
				}
				else if (cur_bid.m_value > auction_obj.get("value").getDoubleValue()) {
					
					// This bid is higher than the previous highest bid. 
					auction_obj.put("value",  cur_bid.m_value);
					auction_obj.put("bid_id", cur_bid.m_bid_id);
				}
			}
		}
	}
	
	public void HandleRequest(Request req) {
		
		// Handle the request based on its type. 
		switch (req.Type()) {
		case BID:
			HandleBid((Bid)req);
			break;
			
		case NEW_AUCTION:
			HandleAuction((Auction)req);
			break;
			default:
		}
	}
}
