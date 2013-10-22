// Author: Jose Faleiro (faleiro.jose.manuel@gmail.com)
//

package apps.Auctions;

public class Auction implements Request {

	public final String m_auction_id;
	public final String m_close_time;
	
	public Auction(String auction_id, String close_time) {
		m_auction_id = auction_id;
		m_close_time = close_time;
	}
	
	public RequestType Type() {
		return RequestType.NEW_AUCTION;
	}
}
