package mp;

import java.util.*;
import java.net.*;
import java.io.*;

import org.apache.commons.lang3.tuple.*;
import org.codehaus.jackson.JsonNode;
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
	
	public static void InitTest(int node_id) {
		VectorTimestamp.Init(addresses.size(),  node_id);
		VectorTimestamp.CreateDefault();
		VectorTimestamp.s_zero = VectorTimestamp.CreateZero();
		m_last_sync = new HashMap<Integer, int[]>();
		
		for (int i = 0; i < addresses.size(); ++i)
			m_last_sync.put(i,  VectorTimestamp.CreateZero());
		
		ShMemObject.s_now = VectorTimestamp.Copy(VectorTimestamp.s_zero); 
		ShMemObject.cur_node_ = node_id;
		
		s_state = new ShMemObject();
		
		deltas_ = new DeltaStore();
		// acquirer_ = new ShMemAcquirer(node_id, deltas_);
		// releaser_ = new ShMemReleaser(node_id);
	}
	
	public static void Init(int node_id) {
		VectorTimestamp.Init(addresses.size(),  node_id);
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
	public static void Acquire(int from) throws ShMemObject.MergeException {
		int[] since = m_last_sync.get(from);
		
		// - Get the appropriate delta.
		// - Merge it into state_. 
		// - Change now_ appropriately.
		// - Update last_sync_.
		ObjectNode delta = deltas_.Pop(from);
		s_state.merge(delta.get("value"));
		VectorTimestamp.UnionWithSerialized(ShMemObject.s_now,  (ArrayNode)(delta.get("time")));
		VectorTimestamp.UnionWithSerialized(since,  (ArrayNode)delta.get("time"));
		VectorTimestamp.IncrementLocal(ShMemObject.s_now);
	}
	
	private static ArrayNode CopyArray(ArrayNode node) {
		
		// Create an empty ArrayNode to return, iterate through the 
		// values in the ArrayNode.
		ArrayNode ret = mapper.createArrayNode();
		int num_vals = node.size();
		for (int i = 0; i < num_vals; ++i) {
			
			// We only recurse if the value is an ObjectNode or an ArrayNode. 
			JsonNode value = node.get(i);
			if (value instanceof ObjectNode) {
				value = CopyObject((ObjectNode)value);
			}
			else if (value instanceof ArrayNode) {
				value = CopyArray((ArrayNode)value);
			}
			
			// Add the copied value to the array node we wish to return. 
			ret.add(value);
		}
		return ret;
	}
	
	private static ObjectNode CopyObject(ObjectNode node) {
		
		// Create an object node to return and iterate through the key-value
		// pairs of this object. 
		ObjectNode ret = mapper.createObjectNode();
		Iterator<Map.Entry<String, JsonNode>> iter = node.getFields();
		while (iter.hasNext()) {
			Map.Entry<String, JsonNode> kvp = iter.next();
			
			// We only need to recurse if the value is an ObjectNode or an ArrayNode
			// because nodes of simple types are immutable. 
			JsonNode to_add = kvp.getValue();
			if (to_add instanceof ObjectNode) {
				to_add = CopyObject((ObjectNode)kvp.getValue());
			}
			else if(to_add instanceof ArrayNode) {
				to_add = CopyArray((ArrayNode)to_add);
			}
			
			// Add the key-value pair to the object to return.
			ret.put(kvp.getKey(), to_add);
		}
		return ret;
	}
	
	public static ObjectNode AcquirePlain(int from) {
		return deltas_.Pop(from);
	}
	
	// Called by regular message passing applications. 
	public static void ReleasePlain(ObjectNode to_send, int to) {
		
		// Make a deep copy of the object we want to release and put it in 
		// the send queue. 
		ObjectNode deep_copy = CopyObject(to_send);	
		releaser_.Release(to,  deep_copy);
	}
	
	// Called from the application. Release state to another process. 
	// Releasing does not block, happens asynchronously in the background. 
	public static void Release(int to) {
		
		// Get the timestamp of the last release. 
		int[] last_sync = m_last_sync.get(to);
		
		// Extract the appropriate delta from state_ (all modifications to
		// state since the last time we released to the process we're releasing
		// to).
		ObjectNode to_release = mapper.createObjectNode();
		to_release.put("time",  VectorTimestamp.toArrayNode(ShMemObject.s_now));
		ObjectNode cur_delta = null;
		cur_delta = ShMemObject.get_diff_tree(s_state, last_sync);
		assert(cur_delta != null);						// Make sure we have a valid delta. 
		to_release.put("value",  cur_delta);
		VectorTimestamp.CopyFromTo(ShMemObject.s_now,  last_sync);
		releaser_.Release(to,  to_release);				// Release (asynchronously). 
		VectorTimestamp.IncrementLocal(ShMemObject.s_now);								// Increment time.
		
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
    	int node_num = 0;
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
    		
    		int port = -1;
    		
    		try {
    			
    			port = Integer.parseInt(parts[1]);
    		}
    		catch(NumberFormatException e) {
    			e.printStackTrace();
    			System.exit(-1);
    		}
    		
    		if (node_num != -1 && port != -1) {
    			addresses.put(node_num, Pair.of(parts[0],  port));
    		}
    		
    		node_num += 1;
    	}
	}

}