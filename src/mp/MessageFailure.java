package mp;

public class MessageFailure extends Exception{

	public String msg;
	
	MessageFailure(String exceptionMsg){
		
		msg = exceptionMsg;
	}
}
