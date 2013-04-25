package mp;

import java.util.*;
import java.net.*;
import java.io.*;

import org.apache.commons.lang3.tuple.*;

import org.json.simple.*;
import org.json.simple.parser.*;

//
// Use this class to manage forks. Automatically keeps track of the 
// reference state, and merges with the parent state. 
//
public class ShMem {
	
	// Address book populated from disk. 
	public static Map<Integer, Pair<String, Integer>> addresses;

	
	public static ShMemObject state;		// Consolidated JSON state used on *all* forks and
											// joins.
	
	private String state_string;
	private long fork_id;
	
	private JSONObject child;			// Received from child after it is done.
	private JSONParser parser;			// Use to parse objects. 
	private ShMemThread thread;			// Does the actual processing in a separate thread.
	private Thread thread_wrapper;		// Wrapper around ShMemThread for Thread library.
										
	
	//
	// Initialize the address book from disk. We expect it to be in a properties file. 
	// 
	static {
		Properties prop = new Properties();
		String config_path = "mp.properties";
		addresses = new HashMap<Integer, Pair<String, Integer>>();
		
		try {
			FileInputStream fi = new FileInputStream(config_path);
			prop.load(fi);
			String input_file = prop.getProperty("addresses");
			ShMem.populate_addresses(input_file);
		}
		catch (Exception e) {
			System.out.println("Couldn't read addresses properly!");
			e.printStackTrace();
			System.exit(-1);
		}
		
		state = new ShMemObject();
	}
	
	//
	// Constructor. We do a deep copy of the original object
	// and put it in the reference field. We also create a ShMemThread
	// to run in the background when fork is invoked. 
	//
	private ShMem(int slave) throws ParseException{
		parser = new JSONParser();
		
		// Deep copy of the parent state. 
		this.state_string = ShMem.state.toJSONString();
		
		// Fork_id to pass to the acquiring process. 
		this.fork_id = ShMemObject.fork_id_cur;
		
		// Increment the fork_id so we can track future modifications to shared state. 
		ShMemObject.fork_id_cur += 1;
		Pair<String, Integer> address = addresses.get(slave);
		if (address == null) {
			System.out.println("Address is invalid!");
			System.exit(-1);
		}
		
		// Create a new ShMemThread which will contact the acquiring process
		// in the background. 
		this.thread = new ShMemThread(this.state_string, 
									  address.getLeft(), 
									  address.getRight());
	}
	
	// 
	// Wrapper for creating a new thread and spawning it off with fork arguments
	// the background. Returns a handle to this object for join-ing. 
	//
	public static ShMem fork(int slave) throws ShMemFailure, ParseException{
		ShMem new_fork = new ShMem(slave);
		new_fork.thread_wrapper = new Thread(new_fork.thread);
		new_fork.thread_wrapper.start();
		return new_fork;
	}
	
	// This class implements the thread interface to communicate with the acquiring
	// process asynchronously. 
	private class ShMemThread implements Runnable {
		
		private String state_string;				// A deep copy of the state. 
		private String machine;						// The acquiring process' machine. 
		private int port;							// The acquiring process' port. 
		
		private Socket conx;						// Connection to talk to the acquring process.
		private BufferedReader in = null;			// Reader to parse input from the acquiring proc.
		private PrintWriter out = null;				// Writer to send data to the acquiring proc. 
		
		
		// Constructor, just initialize the data, machine and port parameters. 
		public ShMemThread(String state_string, String machine, int port){
			this.state_string = state_string;
			this.machine = machine;
			this.port = port;
		}
		
		//
		// Connect to the server on the machine we want to run the forked computation on. 
		//
		private void Connect() {
			
			// Keep re-trying forever.
			// XXX: We should be doing something more sensible here!
			while (true) {
				try {
					conx = new Socket(machine, port);
					in = new BufferedReader(new InputStreamReader(conx.getInputStream()));
					out = new PrintWriter(conx.getOutputStream(), true);
					break;
				}
				catch (IOException e) {
					System.out.println("Couldn't connect to ShMemServer!");
				}
			}
		}
	
		//
		// Do the actual fork. 
		//
		@SuppressWarnings("unchecked")
		private void Fork() {
			
			// To send contains the message we're sending. It wraps the actual "state" as a
			// value in its map. 
			JSONObject to_send = new JSONObject();
			
			// The real "shared state". 
			JSONObject to_send_state = null;
			JSONParser parser = new JSONParser();
			try {
				
				// Convert the serialized copy of shared state to a JSONObject. 
				to_send_state = (JSONObject)parser.parse(state_string);
			}
			catch(ParseException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			// Make sure that we successfully converted serialized state to a JSONObject. 
			assert to_send_state != null;
			
			// Add the state and timestamp to the message we're going to send. 
			to_send.put("argument",  to_send_state);
			to_send.put("fork_id",  fork_id);
			
			// This object is the message we get in response to the fork. 
			// It will contain the appropriate diffs. 
			JSONObject ret = null;
			
			try {
				
				// Send the message (to_send) to the acquiring process. 
				out.println(to_send.toJSONString());
				
				// Wait until the acquiring process responds to us. 
				ret = (JSONObject)parser.parse(in.readLine());
			}
			
			// Sophisticated error handling ;)
			// XXX: Fix this!
			catch(Exception e){
				System.out.println("blah");
			}
			
			// Make sure that we computation forked was successful. 
			if (ret.containsKey("success")) {
				
				// Get the response. 
				child = (JSONObject)ret.get("response");
			}
			else {
				
				System.out.println("Fork failure!");
				System.exit(-1);
			}
			
			// Clean-up the connection state. 
			try {
				out.close();
				in.close();
				conx.close();
			}
			catch(IOException e) {
				
			}
		}

		public void run() {
			
			// First connect to the process we want to fork to, then do the actual
			// fork. 
			Connect();
			Fork();
		}
		
	};
	
	//
	// Recursively check that first and second are *identical* on the same key.
	//
	public static boolean check_equal(final Object first, final Object second) {
		
		if (first.getClass() != second.getClass()) {
			return false;
		}
		
		if (first.getClass() == ShMemObject.class) {
			
			ShMemObject first_obj = (ShMemObject)first;
			ShMemObject second_obj = (ShMemObject) second;
			Set<Object> first_keys = first_obj.keySet();
			Set<Object> second_keys = second_obj.keySet();
			
			if (first_keys.equals(second_keys)) {
				
				for(Object key : first_keys) {
					if (!check_equal(first_obj.get(key), second_obj.get(key))) {
						return false;
					}
				}
			}
			else {
				return false;
			}
		}
		else if (first.getClass() == JSONArray.class) {
			
			JSONArray first_array = (JSONArray)first;
			JSONArray second_array = (JSONArray)second;
			int len_first = first_array.size();
			int len_second = second_array.size();
			
			if (len_first == len_second) {
				
				for (int i = 0; i < len_first; ++i) {
					if (!check_equal(first_array.get(i), second_array.get(i))) {
						return false;
					}
				}
			}
			else {
				return false;
			}
		}
		else if (!first.equals(second)) {
			return false;
		}
		
		return true;
	}
	
	// XXX: This method is not currently used because we treat arrays as leaves, but we
	// probably want to do something more intelligent with arrays. Use it then. 
	private JSONArray merge_arrays(JSONArray first, JSONArray second, JSONArray ref) throws ShMemFailure {
		
		int first_len = first.size();
		int second_len = second.size();
		int ref_len = ref.size();
		
		JSONArray ret = new JSONArray();
		if ((first_len != ref_len) || (second_len != ref_len)) {
			throw new ShMemFailure("Conflicting parent and child!");
		}
		
		for (int i = 0; i < ref_len; ++i) {
			
			if (check_equal(ref.get(i), first.get(i))) {
				ret.add(second.get(i));
			}
			else if (!check_equal(ref.get(i), second.get(i))) {
				throw new ShMemFailure("Conflicting parent and child!");
			}
			else {
				ret.add(first.get(i));
			}
		}
		
		return ret;
	}
	
	//
	// Do a super simple merge. We just check that for every *new* key in the 
	// child. 
	//
	@SuppressWarnings("unchecked")
	public synchronized void join() throws ShMemFailure, ParseException{
		
		try {
			// Wait until the child process responds. 
			thread_wrapper.join();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
			System.out.println("Don't know how to handle this error!");
			System.exit(-1);
		}
		
		// Incorporate the deltas into our state!
		ShMem.state.merge(ShMem.state,  child, fork_id);
	}
	
	//
	// Populate the address book in memory with the addresses from disk. Boring
	// file parsing code. 
	// 
	private static void populate_addresses(String input_file) {
		
		FileReader reader = null;
		try {
			reader = new FileReader(input_file);
		}
		catch(FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
    	BufferedReader file = new BufferedReader(reader);
    	
    	while (true) {
    		
    		String file_line = null;
    		try {
    			file_line = file.readLine();
    		}
    		catch(IOException e) {
    			e.printStackTrace();
    			System.exit(-1);
    		}
    		
    		if (file_line == null) {
    			break;
    		}
    		
    		String parts[] = file_line.split(" ");
    		int node_num = -1;
    		int port = -1;
    		
    		try {
    			
    			node_num = Integer.parseInt(parts[0]); 
    			port = Integer.parseInt(parts[2]);
    		}
    		catch(NumberFormatException e) {
    			e.printStackTrace();
    			System.exit(-1);
    		}
    		
    		if (node_num != -1 && port != -1) {
    			addresses.put(node_num, Pair.of(parts[1],  port));
    		}
    	}
	}
}
