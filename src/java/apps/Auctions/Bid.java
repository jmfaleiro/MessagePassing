package apps.Auctions;

public class Bid implements Request {

	// Fields relevant to a bid. This is the only stuff we share with
	// others. 
	public final long m_bid_id;
	public final String m_auction_id;
	public final double m_value;
	public final String m_bid_time;
	
	public Bid(long id, double value, String auction_id, String time) {
		m_bid_id = id;
		m_value = value;
		m_bid_time = time;
		m_auction_id = auction_id;
	}
	
	public RequestType Type() {
		return RequestType.BID;
	}
}
