package mp;

import org.json.simple.JSONObject;

public class Test {

	public static void main(String[] args) throws InterruptedException, MessageFailure{
		
		Leader s = new Leader(2);
		Node c0 = new Node(2, 0);
		Node c1 = new Node(2, 1);
		
		JSONObject toSend = new JSONObject();
		toSend.put("Hi", "From node 0");
		
		JSONObject ret0 = c0.sendMessage(1,  toSend);
		
		System.out.println(ret0.toJSONString());
		
		JSONObject ret1 = c1.blockingReceive(0);
		
		//JSONObject ret0 = c0.sendMessage(1,  toSend);
		
	}
}
