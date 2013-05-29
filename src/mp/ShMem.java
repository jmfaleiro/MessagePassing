package mp;

import java.util.*;
import java.net.*;
import java.io.*;

import org.apache.commons.lang3.tuple.*;

import org.json.simple.*;
import org.json.simple.parser.*;

//
// Use this class to manage forks. Automatically keeps track of the 
// reference state, and merges with the parent state. The methods in 
// this class are NOT thread-safe. 
//
public class ShMem {
	
	// Address book populated from disk. 
	public static Map<Integer, Pair<String, Integer>> addresses;

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
	}

	
	public  static ShMemObject state_;	// Externally visible state.
	private static DeltaStore deltas_;			// Repository of received deltas. 
	private static ShMemReleaser releaser_; 	// Use to release deltas. 
	private static ShMemAcquirer acquirer_;
	
	// We need to keep the last time we released to extract the appropriate
	// deltas from state_ while releasing. 
	private static Map<Integer, ITimestamp> last_sync_;  
	
	public static void Init(int node_id) {
		last_sync_ = new HashMap<Integer, ITimestamp>();
		ITimestamp default_time = VectorTimestamp.Default(addresses.size(),  node_id);
		for (int i = 0; i < addresses.size(); ++i)
			last_sync_.put(i,  default_time);
		
		ShMemObject.now_ = default_time.Copy();
		ShMemObject.now_.LocalIncrement();
		ShMemObject.cur_node_ = node_id;
		
		state_ = new ShMemObject();
		deltas_ = new DeltaStore();
		acquirer_ = new ShMemAcquirer(node_id, deltas_);
		releaser_ = new ShMemReleaser(node_id);
	}
	
	// Called from the application. 
	// Acquire state from another process. 
	// XXX: Automatically blocks the calling thread if
	// the state isn't yet available. 
	public static void Acquire(int from) throws ShMemFailure {
		ITimestamp since = last_sync_.get(from);
		
		// - Get the appropriate delta.
		// - Merge it into state_. 
		// - Change now_ appropriately.
		// - Update last_sync_.
		JSONObject delta = deltas_.Pop(from);
		state_.merge(delta, since);
		ShMemObject.now_ = state_.MergeTime();
		last_sync_.put(from,  ShMemObject.now_.Copy());
		ShMemObject.now_.LocalIncrement();
	}
	
	// Called from the application. Release state to another process. 
	// Releasing does not block, happens asynchronously in the background. 
	public static void Release(int to) {
		
		// Get the timestamp of the last release. 
		ITimestamp last_sync = last_sync_.get(to);
		
		// Extract the appropriate delta from state_ (all modifications to
		// state since the last time we released to the process we're releasing
		// to).
		JSONObject cur_delta = null;
		try {
			cur_delta = ShMemObject.get_diff_tree(state_, last_sync);
		}
		catch (ShMemFailure e) {
			System.out.println(e.msg);
			System.exit(-1);
		}
		assert(cur_delta != null);						// Make sure we have a valid delta. 
		last_sync_.put(to,  ShMemObject.now_.Copy());	// Deep copy timestamp.
		ShMemObject.now_.LocalIncrement();				// Increment time.
		releaser_.Release(to,  cur_delta);				// Release (asynchronously). 
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