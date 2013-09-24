package mp;

import java.util.*;
import java.net.*;
import java.io.*;

import org.apache.commons.lang3.tuple.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;


//
// Use this class to manage forks. Automatically keeps track of the 
// reference state, and merges with the parent state. The methods in 
// this class are NOT thread-safe. 
//
public class ShMem {
	
	// Address book populated from disk. 
	public static Map<Integer, Pair<String, Integer>> addresses;
	public static ObjectMapper mapper;

	static {
		Properties prop = new Properties();
		String config_path = "mp.properties";
		addresses = new HashMap<Integer, Pair<String, Integer>>();	
		mapper = new ObjectMapper();
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

	
	public  static ShMemObject s_state;	// Externally visible state.
	private static DeltaStore deltas_;			// Repository of received deltas. 
	private static ShMemReleaser releaser_; 	// Use to release deltas. 
	private static ShMemAcquirer acquirer_;
	
	// We need to keep the last time we released to extract the appropriate
	// deltas from state_ while releasing. 
	private static Map<Integer, int[]> m_last_sync;  
	
	public static void Init(int node_id) {
		VectorTimestamp.s_local_index = node_id;
		VectorTimestamp.s_vector_size = addresses.size();
		VectorTimestamp.CreateDefault();
		VectorTimestamp.s_zero = VectorTimestamp.CreateZero();
		m_last_sync = new HashMap<Integer, int[]>();
		
		for (int i = 0; i < addresses.size(); ++i)
			m_last_sync.put(i,  VectorTimestamp.CreateZero());
		
		ShMemObject.s_now = VectorTimestamp.Copy(VectorTimestamp.s_zero); 
		ShMemObject.cur_node_ = node_id;
		
		s_state = new ShMemObject();
		
		deltas_ = new DeltaStore();
		acquirer_ = new ShMemAcquirer(node_id, deltas_);
		releaser_ = new ShMemReleaser(node_id);
	}
	
	public static void Start() {
		VectorTimestamp.IncrementLocal(ShMemObject.s_now);
	}
	
	// Called from the application. 
	// Acquire state from another process. 
	// XXX: Automatically blocks the calling thread if
	// the state isn't yet available. 
	public static void Acquire(int from) {
		int[] since = m_last_sync.get(from);
		
		// - Get the appropriate delta.
		// - Merge it into state_. 
		// - Change now_ appropriately.
		// - Update last_sync_.
		ObjectNode delta = deltas_.Pop(from);
		s_state.merge(delta, since);
		//VectorTimestamp.Union(ShMemObject.s_now,  s_state.getTime());

		m_last_sync.put(from,  VectorTimestamp.Copy(ShMemObject.s_now));
		VectorTimestamp.IncrementLocal(ShMemObject.s_now);
	}
	
	// Called from the application. Release state to another process. 
	// Releasing does not block, happens asynchronously in the background. 
	public static void Release(int to) {
		
		// Get the timestamp of the last release. 
		int[] last_sync = m_last_sync.get(to);
		
		// Extract the appropriate delta from state_ (all modifications to
		// state since the last time we released to the process we're releasing
		// to).
		ObjectNode cur_delta = null;
		cur_delta = ShMemObject.get_diff_tree(s_state, last_sync);
		assert(cur_delta != null);						// Make sure we have a valid delta. 
		m_last_sync.put(to,  VectorTimestamp.Copy(ShMemObject.s_now));	// Deep copy timestamp.
		VectorTimestamp.IncrementLocal(ShMemObject.s_now);								// Increment time.
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