package mp;


import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;


import org.apache.commons.lang3.tuple.*;

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
	
	@SuppressWarnings("unchecked")
	public void run() {
		
		// The service is not multi-threaded. The service should be stateless, so we 
		// shouldn't really care about any co-ordination except for basic book-keeping
		// data structures. 
		while (true) {
			
			// State associated with a client connection. 
			Socket client_socket = null;
			BufferedReader in = null;
			PrintWriter out = null;
			JSONObject ret = null;
			
			// Wait for a client to connect and initialize the input and output streams.
			try {
				client_socket = server_.accept();
				in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
				out = new PrintWriter(client_socket.getOutputStream(), true);
			}
			// If we see an error, it's probably our fault. Quit immediately. 
			catch(IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			// Try to parse out a JSON argument from the contents of the request. 
			JSONParser parser = new JSONParser();
			
			JSONObject arg = null;
			try {
				arg = (JSONObject)parser.parse(in.readLine());
			}
			// IOException should just quit immediately, debug this..
			catch(IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			// Not our fault, communicate failure to the client. 
			catch(ParseException e) {
				ret = failure_message();
			}
			
			
			if (ret == null) {
				
				// Try to parse out a versioned object - which we encode as a JSONArray -
				// from the client's request. If it succeeds, call "process", otherwise, 
				// communicate failure to the client. 
				try {
					
					// Get the argument and get the fork_id. 
					JSONObject versioned_argument = (JSONObject)parser.parse((String)arg.get("argument"));
					int releaser = ((Long)arg.get("releaser")).intValue();
					received_state_.Push(releaser,  versioned_argument);
				}
				// We return a failure message in case we get "is alive" messages as well.
				// All the client really needs is *some* response from the server, we don't
				// really care about the contents of the message. 
				catch (Exception e) {
					ret = failure_message();
				}	
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private JSONObject failure_message() {
		JSONObject ret = new JSONObject();
		ret.put("failure",  1);
		return ret;
	}
}