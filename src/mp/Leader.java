package mp;

import java.io.IOException;
import java.util.*;

import com.esotericsoftware.kryonet.*;

import org.json.simple.*;

public class Leader{

	private JSONArray sharedState[][];
	private int numNodes;
	
	
	private class WaitPair{
		
		public int sender;
		public int receiver;
	};
	
	
	
	Server server;
	
	public Leader(int n){
		
		numNodes = n;
		sharedState = new JSONArray[n][];
		
		for(int i = 0; i < n; ++i){
			
			sharedState[i] = new JSONArray[n];
			for(int j = 0; j < sharedState[i].length; ++j){
				sharedState[i][j] = new JSONArray();
			}
		}
		
		server = new Server();
		Registration.registerClasses(server.getKryo());
		server.addListener(new Listener(){
			
			public void received(Connection connection, Object object) {
				
				System.out.println("Got a new request!");
				
				
				
				JSONObject ret = new JSONObject();
				boolean sendRet = true;
				if (object instanceof JSONObject){
					
					
					if (((JSONObject) object).containsKey("send")){
						
						try{
							int sender = (Integer)((JSONObject) object).get("sender");
							int receiver = (Integer)((JSONObject) object).get("receiver");
							
							JSONObject message = (JSONObject)((JSONObject)object).get("message");
							sendMessage(sender, receiver, message);
							ret.put("message", "Success!");
						}
						catch(Exception e){
							
							ret.put("error", "Malformed send request");
						}
						
						
					}
					
					else if(((JSONObject) object).containsKey("receive")) {
						
						try{
							int sender = (Integer)((JSONObject) object).get("sender");
							int receiver = (Integer)((JSONObject) object).get("receiver");
							
							JSONObject toReturn = receiveMessage(sender, receiver);
							
							ret.put("message", toReturn); 
						}
						catch(Exception e){
							
							ret.put("error", "Malformed receive request");
						}
					}
					else{
						ret.put("Error", "Couldn't understand request");
					}
				}
				else{
					ret.put("Error", "Couldn't understand request");
				}
				
				connection.sendTCP(ret);
			
			}
			
		});
		
	
		server.start();
		try{
			
			server.bind(29188);
		}
		
		catch(IOException e){
			
			System.out.println("Binding server failed!");
			e.printStackTrace();
		}
	}
	
	public void killServer(){
		
		server.stop();
		server.close();
	}
	
	private void sendMessage(int sender, int receiver, JSONObject message){
		
		// Drop the message if from and to are not within range.
		// TODO: have a log of errors.
		if(!(sender >= 0 && sender < numNodes) || 
			!(receiver >= 0 && receiver < numNodes)){
			
			return;
		}
		
		sharedState[receiver][sender].add(message);
	}
	
	private JSONObject receiveMessage(int sender, int receiver){
		
		
		JSONArray buf = sharedState[receiver][sender];
		JSONObject ret;
		try{
			ret = (JSONObject)buf.remove(0);
		}
		catch(IndexOutOfBoundsException e){
			ret = null;
		}
		
		return ret;
	}
	
	
}
