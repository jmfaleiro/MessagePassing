package mp;

import java.io.IOException;
import java.util.*;

import com.esotericsoftware.kryonet.*;

import org.json.simple.*;

public class Leader{

	private JSONArray sharedState[][];
	private int numNodes;
	
	
	private class WaitPair {
		
		public int sender;
		public int receiver;
	};
	
	
	
	Server server;
	
	public Leader(int n) {
		
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
			
			public void received(Connection connection, Object object_buffer) {
				
				System.out.println("Got a new request!");
				
				JSONObject ret = new JSONObject();
				boolean sendRet = true;
				if (object_buffer instanceof byte[]){
					
					String objectString = null;
					try {
						objectString = new String((byte[])object_buffer, "UTF-8");
					}
					catch(Exception e) {
						
						ret.put("error",  "Malformed send request");
					}
					
					Object toConvert = JSONValue.parse((String)objectString);
					JSONObject object = (JSONObject)toConvert;
					
					if (((JSONObject) object).containsKey("send")){
						
						try{
							long sender_long = (Long)((JSONObject) object).get("sender");
							long receiver_long = (Long)((JSONObject) object).get("receiver");
							
							int sender = (int)sender_long;
							int receiver = (int)receiver_long;
							
							
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
							long sender_long = (Long)((JSONObject) object).get("sender");
							long receiver_long = (Long)((JSONObject) object).get("receiver");
							
							int sender = (int)sender_long;
							int receiver = (int)receiver_long;
							
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
				
				connection.sendTCP(ret.toJSONString().getBytes());
			
			}
			
		});
		
	
		server.start();
		try{
			
			server.bind(44555);
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
