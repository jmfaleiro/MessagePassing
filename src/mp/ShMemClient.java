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
public class ShMemClient {
	
	public static Map<Integer, Pair<String, Integer>> addresses;

	private JSONArray reference;
	private JSONObject child;
	private int slave;
	private JSONParser parser;
	private ShMemThread thread;
	private Thread thread_wrapper;
	
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
			ShMemClient.populate_addresses(input_file);
		}
		catch (Exception e) {
			System.out.println("Couldn't read addresses properly!");
			e.printStackTrace();
			System.exit(-1);
		}
		
	
	}
	
	//
	// Constructor. We do a deep copy of the original object
	// and put it in the reference field. We also create a ShMemThread
	// to run in the background when fork is invoked. 
	//
	public ShMemClient(JSONArray state, int slave) throws ParseException{
		
		parser = new JSONParser();
		this.reference = (JSONArray) parser.parse(state.toJSONString());
		this.slave = slave;
		Pair<String, Integer> address = addresses.get(slave);
		
		if (address == null) {
			System.out.println("Address is invalid!");
			System.exit(-1);
		}
		
		this.thread = new ShMemThread(reference, address.getLeft(), address.getRight());
	}
	
	// 
	// Wrapper for creating a new thread and spawning it off with fork arguments
	// the background.
	//
	public void fork() throws ShMemFailure{
		
		this.thread_wrapper = new Thread(thread);
		this.thread_wrapper.start();
	}
	
	private class ShMemThread implements Runnable {
		
		private JSONArray args;
		private String machine;
		private int port;
		
		private Socket conx;
		private BufferedReader in = null;
		private PrintWriter out = null;
		
		public ShMemThread(JSONArray args, String machine, int port){
			
			this.args = args;
			this.machine = machine;
			this.port = port;
		}
		
		//
		// Connect to the server on the machine we want to run the forked computation on. 
		//
		private void Connect() {
			
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
		private void Fork() {
			JSONObject to_send = new JSONObject();
			to_send.put("argument", args);
			
			
			JSONParser parser = new JSONParser();
			JSONObject ret = null;
			
			try {
				out.println(to_send.toJSONString());
				ret = (JSONObject)parser.parse(in.readLine());
			}
			catch(Exception e){
				
				System.out.println("blah");
			}
			
			if (ret.containsKey("success")) {
				child = (JSONObject)ret.get("response");
			}
			else {
				
				System.out.println("Fork failure!");
				System.exit(-1);
			}
			
			try {
				out.close();
				in.close();
				conx.close();
			}
			catch(IOException e) {
				
			}
		}

		public void run() {

			Connect();
			Fork();
		}
		
	};
	
	//
	// Do a super simple merge. We just check that for every *new* key in the 
	// child. 
	//
	public synchronized JSONArray merge(JSONArray parent) throws ShMemFailure{
		
		try {
			thread_wrapper.join();
		}
		catch(InterruptedException e) {
			e.printStackTrace();
			System.out.println("Don't know how to handle this error!");
			System.exit(-1);
		}
		
		
		int parent_len = parent.size();
		
		if (parent_len > 0) {
			JSONObject parent_state = (JSONObject)parent.get(parent_len-1);
			
			// First check that the key cannot possibly conflict with anything in the parent. 
			for (Object key : child.keySet()) {
				
				if (parent_state.containsKey(key)) {
					throw new ShMemFailure("Trying to merge a key that already exists!");
				}
			}
			
			// Put all the keys in the child into the parent. 
			parent_state.putAll(child);
		}
		return parent;
	}
	
	//
	// Populate the address book in memory with the addresses from disk. 
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
