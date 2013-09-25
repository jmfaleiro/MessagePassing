package mp.java;

public class ShMemFailure extends Exception{

	
	private static final long serialVersionUID = -3502591856642218099L;
	public String msg;
	
	public ShMemFailure(String msg) {
		this.msg = msg;
	}
}
