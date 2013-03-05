package mp;

import org.json.simple.JSONObject;

import java.io.*;

public class Test {

	public static void main(String[] args) throws InterruptedException, IOException{
		
		//Leader s = new Leader(2);
		NodeNew c0 = new NodeNew(2, 1);
		//Node c1 = new Node(2, 1);
		
		/*
		JSONObject toSend = new JSONObject();
		toSend.put("Hi", "From node 0");
		
		JSONObject ret0 = c0.sendMessage(1,  toSend);
		
		System.out.println(ret0.toJSONString());
		
		*/
		
		JSONObject ret1 = c0.receiveMessage(0);
		
		System.out.println(ret1.toJSONString());
		
		//JSONObject ret0 = c0.sendMessage(1,  toSend);
		
	}
}
