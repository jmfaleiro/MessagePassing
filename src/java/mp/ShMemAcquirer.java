package mp;


import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;


import org.apache.commons.lang3.tuple.*;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;


//
// This should be run on the node on which we want to run forked computations. 
// TODO: We currently ship *every* version of state on a fork. We could potentially keep
//       that state around and only send the latest version of state from the parent. 
//
public class ShMemAcquirer implements Runnable {
	
	
	// Server object that listens on a port for client connections. 
	private ServerSocket m_server;
	private Thread m_thread;
	private ObjectMapper m_mapper;
	
	// Put the state received from remote processes here. 
	private DeltaStore m_received_state;
	
	public ShMemAcquirer(int id, DeltaStore received_state) {
    	try {
    		Pair<String, Integer> my_address = ShMem.addresses.get(id);
    		m_server = new ServerSocket(my_address.getRight());
    		m_received_state = received_state;
    		m_mapper = new ObjectMapper();
    		start();
    	}
    	catch (IOException e) {
    		System.out.println("ShMemNode: Couldn't listen on port!!!");
    	    System.exit(-1);
    	}
	}
	
	public void start() {
		m_thread = new Thread(this);
		m_thread.start();
	}
	
	@SuppressWarnings("unchecked")
	public void run() {
		while (true) {
			
			// State associated with a client connection. 
			ObjectNode value = null;
			int sender = -1;
			
			// Wait for a client to connect and initialize the input and output streams.
			try {
				
				// Accept a connection from some client and grab the input stream corresponding to the connection.
				Socket client = m_server.accept();
				InputStream is = client.getInputStream();
				
				// Parse the JSON, get appropriate values of the diff and the client who released. 
				ObjectNode wrapper = (ObjectNode)m_mapper.readTree(is);
				value = (ObjectNode)wrapper.get("argument");
				sender = wrapper.get("releaser").getIntValue();
				
				// Close the input stream and client socket. 
				is.close();
				client.close();
			}
			catch (JsonProcessingException e) {
				System.err.println("Coudln't parse JSON!");
				System.exit(-1);
			}
			// If we see an error, it's probably our fault. Quit immediately. 
			catch(IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			m_received_state.Push(sender,  value);
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject failure_message() {
		JSONObject ret = new JSONObject();
		ret.put("failure",  1);
		return ret;
	}
}