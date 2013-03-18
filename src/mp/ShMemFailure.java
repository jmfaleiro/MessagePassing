package mp;

public class ShMemFailure extends Exception{

	public String msg;
	
	public ShMemFailure(String msg) {
		this.msg = msg;
	}
}
