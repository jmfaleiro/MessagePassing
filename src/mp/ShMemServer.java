package mp;


import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


import org.apache.commons.lang3.tuple.*;

//
// This should be run on the node on which we want to run forked computations. 
// TODO: We currently ship *every* version of state on a fork. We could potentially keep
//       that state around and only send the latest version of state from the parent. 
//
public class ShMemServer implements Runnable {
	
	// Server object that listens on a port for client connections. 
	private ServerSocket server;
	// Code to run on a fork request. 
	private IProcess my_process; 
	
	private Thread thread;
	private Semaphore initialized;
	
	public ShMemServer(IProcess process, int id) {
		
		my_process = process;
		initialized = new Semaphore(1);
		
		try {
			initialized.acquire();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
    	
    	try {
    		
    		Pair<String, Integer> my_address = ShMemClient.addresses.get(id);
    		server = new ServerSocket(my_address.getRight());
    	}
    	catch (IOException e) {
    		System.out.println("ShMemNode: Couldn't listen on port!!!");
    	    System.exit(-1);
    	}
	}
	
	public void start() {
		
		thread = new Thread(this);
		thread.start();
		try {
			initialized.acquire();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
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
				initialized.release();
				client_socket = server.accept();
				
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
					JSONArray versioned_argument = (JSONArray)arg.get("argument");
					JSONObject versioned_reply = my_process.process(versioned_argument);
					
					ret = new JSONObject();
					ret.put("success",  1);
					ret.put("response",  versioned_reply);
				}
				// We return a failure message in case we get "is alive" messages as well.
				// All the client really needs is *some* response from the server, we don't
				// really care about the contents of the message. 
				catch (Exception e) {
					ret = failure_message();
				}	
			}
			
			try {
				// Give the result back to the client.
				out.println(ret.toJSONString());
				
				// Do some cleanup. 
				in.close();
				out.close();
				client_socket.close();
				client_socket = null;
			}
			catch (IOException e) {
				
			}
		}
	
	}
	
	private JSONObject failure_message() {
		JSONObject ret = new JSONObject();
		ret.put("failure",  1);
		return ret;
	}
}