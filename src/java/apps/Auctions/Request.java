package apps.Auctions;

public interface Request {

	public enum RequestType {
		BID, NEW_AUCTION,
	}
	
	public RequestType Type();
}
