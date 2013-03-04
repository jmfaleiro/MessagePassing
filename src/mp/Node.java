package mp;

import org.json.simple.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.*;

import java.io.IOException;
import java.util.*;



public class Node{
	
	private int m_totalNodes;
	private int m_nodeId;
	
	private Client client;
	
	private JSONObject toReturn;
	private boolean done;
	
	private static int connectionTimeout = 10000;
	private static int numRetries = 10;
	
	public Node(int totalNodes, int nodeId){
		
		m_totalNodes = totalNodes;
		m_nodeId = nodeId;
		
		client = new Client();
		
		Registration.registerClasses(client.getKryo());
		
		client.addListener(new Listener(){
			
			public void received(Connection connection, Object object){
				
				JSONObject ret;
				if (object instanceof String){
					
					Object blah = JSONValue.parse((String)object);
					ret = (JSONObject)blah;
				}
				else{
					
					ret = new JSONObject();
					ret.put("Error",  "Malformed server response");
				}
				
				receive(ret);
				
				client.stop();
				client.close();
			}
			
		});
	}
	
	private synchronized void receive(JSONObject ret){
		
		done = true;
		toReturn = ret;
		notify();
	}
	
	/*
	public synchronized void dummy(){
		
		try{
			client.start();
			client.connect(5000,  "localhost", 29188);
			client.sendTCP("blah");
			
			
		}
		catch(IOException e){
			
			e.printStackTrace();
		}
		
		while(!done){
			try {
				wait();
			} 
			catch (InterruptedException e) {
				
				e.printStackTrace();
			}
		}
		
		done = false;
		System.out.println("Notification works!");
		System.out.println(retMsg);
		
	}
	*/
	
	private synchronized JSONObject sendRequest(JSONObject obj) throws MessageFailure{
		
		int tryCount = 0;
		String objString = obj.toJSONString();
		while (tryCount++ < numRetries){
			
			try{
				
				client.start();
				client.connect(Node.connectionTimeout, "localhost", 29188);
				client.sendTCP(objString);
				break;
			}
			catch(IOException e){
				
				e.printStackTrace();
			}
		}
		
		if (tryCount > numRetries){
			
			throw new MessageFailure("Exceeded number of retries");
		}
		
		while(!done){
			
			try{
				wait();
			}
			catch(InterruptedException e){ }
		}
		
		done = false;
		return toReturn;
	}
	
	public JSONObject sendMessage(int toNode, JSONObject message) throws MessageFailure{
		
		JSONObject toSend = new JSONObject();
		
		toSend.put("send",  "send");
		toSend.put("receiver", toNode);
		toSend.put("sender",  this.m_nodeId);
		toSend.put("message", message);
		
		return sendRequest(toSend);
	}
	
	public JSONObject blockingReceive (int fromNode) throws MessageFailure {
		
		JSONObject reply = receiveMessage(fromNode);
		
		while (reply.get("message") == null)
			reply = receiveMessage(fromNode);
		
		return reply;
		
	}
	
	public JSONObject receiveMessage(int fromNode) throws MessageFailure{
		
		
		JSONObject request = new JSONObject();
		
		request.put("receive", "receive");
		request.put("sender", fromNode);
		request.put("receiver", m_nodeId);
		
		return sendRequest(request);
	}
	
}
