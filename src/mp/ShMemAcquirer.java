package mp;


import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;


import org.apache.commons.lang3.tuple.*;
import org.codehaus.jackson.JsonParseException;
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
	private ServerSocket server_;
	private Thread thread_;
	
	// Put the state received from remote processes here. 
	private DeltaStore received_state_;
	
	public ShMemAcquirer(int id, DeltaStore received_state) {
    	try {
    		Pair<String, Integer> my_address = ShMem.addresses.get(id);
    		server_ = new ServerSocket(my_address.getRight());
    		received_state_ = received_state;
    		start();
    	}
    	catch (IOException e) {
    		System.out.println("ShMemNode: Couldn't listen on port!!!");
    	    System.exit(-1);
    	}
	}
	
	public void start() {
		thread_ = new Thread(this);
		thread_.start();
	}
	
	private class LongConnection implements Runnable {
		
		private BufferedReader m_in;
		private PrintWriter m_out;
		private ObjectMapper m_mapper;
		
		public LongConnection(Socket conx) {
			
			try {
				m_in = new BufferedReader(new InputStreamReader(conx.getInputStream()));
				m_out = new PrintWriter(conx.getOutputStream(), true);
			}
			catch (Exception e) {
				e.printStackTrace(System.err);
				System.exit(-1);
			}
			m_mapper = new ObjectMapper();
		}
		
		public void run() {
			while (true) {
				ObjectNode received_delta = null;
				try {
					String temp = m_in.readLine();
					received_delta = m_mapper.readValue(temp,  ObjectNode.class);
					received_state_.Push(received_delta.get("releaser").getIntValue(),
							 (ObjectNode)m_mapper.readValue(received_delta.get("argument").getTextValue(), ObjectNode.class));
				} 
				catch (Exception e) {
					e.printStackTrace(System.err);
					System.exit(-1);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void run() {
		while (true) {
			
			// State associated with a client connection. 
			Socket client_socket = null;
			
			// Wait for a client to connect and initialize the input and output streams.
			try {
				client_socket = server_.accept();
			}
			// If we see an error, it's probably our fault. Quit immediately. 
			catch(IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			Thread conx_thread = new Thread(new LongConnection(client_socket));
			conx_thread.start();
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject failure_message() {
		JSONObject ret = new JSONObject();
		ret.put("failure",  1);
		return ret;
	}
}