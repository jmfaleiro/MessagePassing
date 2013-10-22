// Author: Jose Faleiro (faleiro.jose.manuel@gmail.com)

package apps.Auctions;

public interface Request {

	public enum RequestType {
		BID, NEW_AUCTION,
	}
	
	public RequestType Type();
}
