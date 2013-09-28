package mp.java;


import mp.java.ITimestamp.Comparison;

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.*;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import java.math.BigDecimal;
import java.util.*;

import org.apache.commons.lang3.tuple.*;

/*
 * Users should use ShMemObject where the intended to use JSONObjects. A user may still
 * user JSONObjects but they will not be interpreted in any way. If both the acquiring and
 * releasing processes make changes to unrelated parts of a JSONObject, the updates will be
 * marked conflicting. 
 */
public class ShMemObject extends ObjectNode {
	
	
	private static JsonNodeFactory s_factory;
	
	// Points to the parent of this object in the recursive ShMemObject hierarchy. 
	private ShMemObject parent;
	
	// The key the parent uses to refer to this object. We need the key to update timestamp
	// information in the parent recursively. 
	private String parent_key;
	
	// The current value of "time". All puts to an ShMemObject will take this value. The value
	// is changed whenever we fork. 
	public static int[] s_now;
	
	// Index of the current node in the timestamps. 
	public static int cur_node_;
	
	// Initialize an empty ShMemObject. 
	public ShMemObject() {
		super(JsonNodeFactory.instance);
		this.parent = null;
		this.parent_key = "";
		m_key_map = new HashMap<String, ListNode>();
		m_sorted_keys = new InternalLinkedList();
	}
	
	public HashMap<String, ListNode> m_key_map;
	public InternalLinkedList m_sorted_keys;
	
	public class ListNode {
		
		public final String m_key;
		public final int[] m_timestamp;
		
		public ListNode m_prev;
		public ListNode m_next;
		
		public ListNode(String key, int[] timestamp) {
			m_key = key;
			m_timestamp = timestamp;
			m_prev = null;
			m_next = null;
		}
	}
	
	protected class InternalLinkedList {
		
		public ListNode m_head;
		public ListNode m_tail;
		
		public InternalLinkedList() {
			m_head = null;
			m_tail = null;
		}
		
		public void InsertFront(ListNode cur) {
			if (m_head == null) {
				m_head = cur;
				m_tail = cur;
			}
			else {
				cur.m_prev = null;
				cur.m_next = m_head;
				m_head.m_prev = cur;
				m_head = cur;
			}
		}
		
		public void InsertLast(ListNode cur) {
			if (m_head == null) {
				m_head = cur;
				m_tail = cur;
			}
			else {
				cur.m_prev = m_tail;
				m_tail.m_next = cur;
				cur.m_next = null;
				m_tail = cur;
			}
		}
		
		public void MoveFront(ListNode cur) {
			
			// If prev is null, the node is already at the front of the list. 
			if (cur.m_prev != null) {
				cur.m_prev.m_next = cur.m_next;
				if (cur.m_next != null) {
					cur.m_next.m_prev = cur.m_prev;
				}
			}
		}
		
		public void Remove(ListNode cur) {
			ListNode prev = cur.m_prev;
			ListNode next = cur.m_next;
			
			// We're trying to remove a head node. 
			if (prev == null) {
				m_head = next;
			}
			else {
				prev.m_next = next;
			}
			
			// We're trying to remove a tail node. 
			if (next == null) {
				m_tail = prev;
			}
			else {
				next.m_prev = prev;
			}
			
			cur.m_prev = null;
			cur.m_next = null;
		}
	}
	
	// This method is used to parse out an ShMemObject after it has been serialized to
	// a JSONObject. Used when we transmit diffs to the acquirer. 
	
	/*
	public static ShMemObject json2shmem(JSONObject root) throws ShMemFailure {
		ShMemObject ret = new ShMemObject();
		
		// 1. Go through the keys and wrap them into JSONObjects. 
		// 2. Add each wrapped value into the ShMemObject's map. 
		// Do the above recursively.. 
		for (Object k : root.keySet()) {
				
			// Get the key.
			String key = (String)k;
			
			// Get the wrapped value and its timestamp.  
			JSONObject wrapper_value = (JSONObject)root.get(k);
			Object value = wrapper_value.get("value");
			if (value.getClass() == JSONObject.class) {
				
				// Recursively do this on this object's children. 
				ShMemObject child = json2shmem((JSONObject)value);
				value = child;
			}
			
			ITimestamp child_ts = VectorTimestamp.Deserialize((String)wrapper_value.get("shmem_timestamp"),
															  cur_node_);
			
			// Temporarily switch timestamps so that the inserted values have the right
			// timestamp. 
			// 
			// The child's timestamp shouldn't be subsumed by now_.
			assert(child_ts.Compare(now_) != Comparison.LT);	
			ret.insert(key,  value,  child_ts);
		}
		return ret;
	}
	
	*/
	
	private static void fixTime(ShMemObject cur, int[] time) {
		while (cur.parent != null) {
			String key = cur.parent_key;
			int[] cur_timestamp = cur.parent.m_key_map.get(key).m_timestamp;
			Comparison comp = VectorTimestamp.Compare(cur_timestamp,  time);
			if (comp == Comparison.LT) {
				VectorTimestamp.Union(cur_timestamp,  time);
				
				ListNode cur_node = cur.parent.m_key_map.get(cur.parent_key);
				cur.parent.m_sorted_keys.MoveFront(cur_node);
				
				cur = cur.parent;
			}
			else {
				break;
			}
		}
	}
	
	private ArrayNode getNodeTimestamp(String key) {
		return (ArrayNode)super.get(key).get("shmem_timestamp");
	}
	
	
	
	public static ObjectNode get_diff_tree(ShMemObject obj, int[] ts) {
		ObjectNode ret = ShMem.mapper.createObjectNode();
		
		// The keys are sorted in timestamp order, so we can stop iterating
		// so long as ts is less than the current node's timestamp. 
		for (ListNode cur = obj.m_sorted_keys.m_head; 
			 cur != null && VectorTimestamp.Compare(ts, cur.m_timestamp) == Comparison.LT;
			 cur = cur.m_next) {
			
			// Create a wrapper which will contain the actual value and its
			// corresponding timestamp. The value is contained in value_tree
			// which is recursively derived if the value is an object node. 
			ObjectNode wrapper = ShMem.mapper.createObjectNode();
			JsonNode value = obj.get(cur.m_key);
			ArrayNode serialized_time = VectorTimestamp.toArrayNode(cur.m_timestamp);
			JsonNode value_tree;
			
			// If the value is an object node, recursively derive its
			// serialization. 
			if (value.isObject()) {
				value_tree = get_diff_tree((ShMemObject)value, ts);
			}
			else {
				value_tree = value;
			}
			
			// Finally, put value_tree and serialized_time into a wrapper which
			// gets put into ret.
			wrapper.put("value",  value_tree);
			wrapper.put("shmem_timestamp",  serialized_time);
			ret.put(cur.m_key,  wrapper);
		}
		
		return ret;
	}
	
	/*
	//
	// Recursively select the timestamps from the object for merging.
	//
	public static JSONObject get_diff_tree(ShMemObject obj, ITimestamp from_ts) throws ShMemFailure{
		JSONObject ret = new JSONObject();
		
		// Get the set of keys from this object. 
		for (Object k : obj.keySet()) {
			assert k.getClass() == String.class;
			
			// All values are encapsulated in a JSONObject to record timestamp
			// information, get the wrapper. 
			JSONObject val = (JSONObject)obj.get(k);
			
			// We only bother adding the subtree to diffs if it has been updated since the
			// reference timestamp. 
			// INVARIANT: The root's timestamp is the maximum timestamp of any of its leaves. 
			// Hence, if the root's timestamp is less than or equal to from_ts, none of the internal
			// nodes can ever have a timestamp greater than from_ts. 
			ITimestamp timestamp = (ITimestamp)val.get("shmem_timestamp");
			if (timestamp.Compare(from_ts) == Comparison.GE) {
			
				JSONObject to_add = new JSONObject();
				to_add.put("shmem_timestamp",  timestamp.Serialize());
				Object value = val.get("value");
				
				// If the value is an ShMemObject, recursively add the right diffs. 
				if (value.getClass() == ShMemObject.class) {
					to_add.put("value",  get_diff_tree((ShMemObject)value, from_ts)); 
				}
				
				// This is a leaf, add the diff. 
				else {
					to_add.put("value",  value);
				}
				ret.put(k,  to_add);
			}
		}
		return ret;
	}
	*/
	/*
	public ITimestamp MergeTime() throws ShMemFailure {
		ITimestamp ret = now_.Copy();
		for (Object k : this.keySet()) {
			ITimestamp temp = (ITimestamp)((JSONObject)super.get(k)).get("shmem_timestamp");
			ret.Union(temp);
		}
		return ret;
	}
	*/
	
	private ObjectNode getWrapper(String key) {
		return (ObjectNode)super.get(key);
	}
	
	private void put_common(String fieldname, int[] timestamp) {
		ListNode cur_node;
		try {
			cur_node = m_key_map.get(fieldname);
		}
		catch (Exception e) {
			cur_node = null;
		}
		if (cur_node == null) {
			
			// Allocate a list node for the new key. 
			cur_node = new ListNode(fieldname, timestamp);
			m_key_map.put(fieldname,  cur_node);
			m_sorted_keys.InsertFront(cur_node);
		}
		else {	// There already exists a list node and timestamp. 
			
			VectorTimestamp.CopyFromTo(timestamp, cur_node.m_timestamp);
			m_sorted_keys.MoveFront(cur_node);
		}
	}
	
	@Override
	public void put(String fieldname, BigDecimal v) {
		int[] new_timestamp = VectorTimestamp.Copy(s_now);
		put_common(fieldname, new_timestamp);
		super.put(fieldname,  v);
		fixTime(this, s_now);
	}
	
	@Override
	public void put(String fieldname, boolean v) {
		int[] new_timestamp = VectorTimestamp.Copy(s_now);
		put_common(fieldname, new_timestamp);
		super.put(fieldname,  v);
		fixTime(this, s_now);
	}
	
	@Override
	public void put(String fieldname, byte[] v) {
		int[] new_timestamp = VectorTimestamp.Copy(s_now);
		put_common(fieldname, new_timestamp);
		super.put(fieldname, v);
		fixTime(this, s_now);
	}
	
	@Override
	public void put(String fieldname, double v) {
		int[] new_timestamp = VectorTimestamp.Copy(s_now);
		put_common(fieldname, new_timestamp);
		super.put(fieldname,  v);
		fixTime(this, s_now);
	}
	
	@Override
	public void put(String fieldname, float v) {
		int[] new_timestamp = VectorTimestamp.Copy(s_now);
		put_common(fieldname, new_timestamp);
		super.put(fieldname,  v);
		fixTime(this, s_now);
	}
	
	@Override
	public void put(String fieldname, int v) {
		int[] new_timestamp = VectorTimestamp.Copy(s_now);
		put_common(fieldname, new_timestamp);
		super.put(fieldname,  v);
		fixTime(this, s_now);
	}
	
	@Override
	public JsonNode put(String fieldname, JsonNode v) {
		int[] new_timestamp = VectorTimestamp.Copy(s_now);
		put_common(fieldname, new_timestamp);
		super.put(fieldname,  v);
		fixTime(this, s_now);
		return null;
	}
	
	@Override
	public void put(String fieldname, long v) {
		int[] new_timestamp = VectorTimestamp.Copy(s_now);
		put_common(fieldname, new_timestamp);
		super.put(fieldname,  v);
		fixTime(this, s_now);
	}
	
	@Override
	public void put(String fieldname, String v) {
		int[] new_timestamp = VectorTimestamp.Copy(s_now);
		put_common(fieldname, new_timestamp);
		super.put(fieldname,  v);
		fixTime(this, s_now);
	}
	
	public void put(String fieldname, ShMemObject v) {
		v.parent = this;
		v.parent_key = fieldname;
		
		int[] new_timestamp = VectorTimestamp.Copy(s_now);
		put_common(fieldname, new_timestamp);
		super.put(fieldname,  v);
		fixTime(this, s_now);
	}
	
	@Override
	public JsonNode remove(String fieldname) {
		int[] new_timestamp = VectorTimestamp.Copy(s_now);
		put_common(fieldname, new_timestamp);
		JsonNode ret = super.remove(fieldname);
		fixTime(this, s_now);
		return ret;
	}
	
	/*
	private Object insert(String key, Object value, ITimestamp ts) throws ShMemFailure {
		
		// We wrap the value in the "to_add" JSONObject.
		JSONObject to_add = new JSONObject();
		
		// Add the timestamp to the wrapper. 
		to_add.put("shmem_timestamp",  ts);
		
		// If the value is an ShMemObject class, update its parent and parent_key information.
		// The parent is the current object (this) and the key is the 'key' argument. 
		if (value.getClass() == ShMemObject.class) {
			ShMemObject sh_mem_value = (ShMemObject)value;
			sh_mem_value.parent = this;
			sh_mem_value.parent_key = key;
		}
		
		// Recursively update this ShMemObject's parents' timestamps. 
		update_difftree(this, ts);
		
		// Finally, put the actual value into the wrapper and insert the wrapper into
		// 'this'. 
		to_add.put("value", value);
		return super.put(key, to_add);
	}
	
	private Object delete(String key, ITimestamp ts) throws ShMemFailure {
		
		Object ret = super.remove(key);
		
		// Update diff meta data
		JSONObject to_add = new JSONObject();
		to_add.put("shmem_timestamp",  ts);
		update_difftree(this, ts);
		return ret;
	}
	
	*/
	
	
	// 
	// We can't just remove a key from the map because the remove is still an UPDATE. 
	// We insert a tombstone as the leaf (null), which will be interpreted by the merge
	// process appropriately. Merge understands when to garbage collect tombstones. 
	//
	// XXX: This method looks buggy!
	
	/*
	public Object remove(Object key) {
		Object ret = super.remove(key);
		
		// Update diff meta data
		update_difftree(this, now_);
		JSONObject to_add = new JSONObject();
		to_add.put("shmem_timestamp",  now_);
		return ret;
	}
	*/
	
	/*
	
	
	private void put_force(ShMemObject other) throws ShMemFailure {
		for (Object k : other.keySet()) {
			Object other_value = other.get((String)k);
			if (this.containsKey(k)) {
				Object this_value = this.get((String)k);
				if ((other_value.getClass() != ShMemObject.class) || 
					(this_value.getClass() != ShMemObject.class)) {
					ITimestamp saved = now_;
					now_ = (ITimestamp)((JSONObject)other.get(k)).get("shmem_timestamp");
					this.put((String)k, other_value);
					now_ = saved;
				}
				else {
					((ShMemObject)this_value).put_force((ShMemObject)other_value);
				}
			}
			else {
				ITimestamp saved = now_;
				now_ = (ITimestamp)((JSONObject)other.get(k)).get("shmem_timestamp");
				this.put((String)k, other_value);
				now_ = saved;
			}
		}
	}
	*/
	
	public void InsertAt(String fieldname, JsonNode node, int[] time) {
		VectorTimestamp.Union(s_now,  time);
		put_common(fieldname, time);
		super.put(fieldname,  node);
		fixTime(this, time);
	}
	
	private static ShMemObject DeserializeObjectNode(ObjectNode obj) {
		ShMemObject ret = new ShMemObject();
		Iterator<Map.Entry<String,JsonNode>> fields = obj.getFields();
		
		// Every field is "new" so we have to create new objects to keep
		// track of timestamp information. 
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> cur = fields.next();
			String cur_key = cur.getKey();
			
			// Serialized objects will always have a timestamp and value
			// wrapped into a larger ObjectNode
			JsonNode wrapped_value = cur.getValue();
			JsonNode real_value = wrapped_value.get("value");
			ArrayNode serialized_timestamp = 
					(ArrayNode)wrapped_value.get("shmem_timestamp");
			
			// Create a timestamp object for the new node and a list node
			// to keep track of it. 
			// XXX: Does the serialization preserve the order in which
			int[] new_timestamp = 
					VectorTimestamp.CopySerialized(serialized_timestamp);
			ListNode new_list_node = ret.new ListNode(cur_key, new_timestamp);
			ret.m_key_map.put(cur_key,  new_list_node);
			ret.m_sorted_keys.InsertLast(new_list_node);
			
			if (real_value.isObject()) {
				ShMemObject to_insert = DeserializeObjectNode((ObjectNode)real_value);
				ret.InsertAt(cur_key, 
							 to_insert, 
							 new_timestamp);
				to_insert.parent = ret;
				to_insert.parent_key = cur_key;
				
			}
			else {
				ret.InsertAt(cur_key, real_value, new_timestamp);
			}
		}
		return ret;
	}
	
	public class MergeException extends Exception {
		
		private String m_message;
		
		public MergeException(String value) {
			m_message = value;
		}
		
		@Override
		public String toString() {
			return m_message;
		}
	}
	
	public void merge(JsonNode release) throws MergeException {
		Iterator<Map.Entry<String,JsonNode>> fields = release.getFields();
		
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> cur = fields.next();
			String key = cur.getKey();
			JsonNode wrapped_value = cur.getValue();
			ArrayNode other_timestamp = (ArrayNode)wrapped_value.get("shmem_timestamp");
			JsonNode other_value = wrapped_value.get("value");
			
			ListNode my_list_node = m_key_map.get(key);
			
			if (my_list_node != null) {
				
				int[] my_timestamp = my_list_node.m_timestamp;
				
				// We need to compare timestamps, so use ObjectNode's get. 
				JsonNode my_value = super.get(key);
				
				Comparison comp = 
						VectorTimestamp.CompareWithSerializedTS(my_timestamp,  
																other_timestamp);
				
				// Both have written to the same node.
				if (comp == Comparison.NONE) {
					if (other_value.isObject() && my_value.isObject()) {
						((ShMemObject)my_value).merge(other_value);
					}
					else {
						// We've just detected a write-write conflict. 
						throw new MergeException("Merge exception!");
					}
				}
				else if (comp == Comparison.LT) {
					
					// Just take the other guy's timestamp. 
					VectorTimestamp.CopyFromSerializedTo(other_timestamp, my_timestamp); 
							 
					// Take the other guy's changes because we're subsumed. 
					if (other_value.isObject()) {
						ShMemObject deserialized_value = DeserializeObjectNode((ObjectNode)other_value);
						this.InsertAt(key,  deserialized_value,  my_timestamp);
						deserialized_value.parent = this;
						deserialized_value.parent_key = key;
					}
					else {
						this.InsertAt(key,  other_value,  my_timestamp);
					}
					
					// Move this key's list node to the front. 
					ListNode new_list_node = m_key_map.get(key);
					m_sorted_keys.MoveFront(new_list_node);
				}
				else {
					// Two cases: Either we are greater, in which case we can keep our changes. 
					// or we're equal, in which case it's also safe to keep our changes. 
					continue;
				}
			}
			else {	// This means that we don't contain the key. 
				
				// We need to create  new timestamp because it doesn't yet 
				// exist. 
				int[] new_timestamp = 
						VectorTimestamp.CopySerialized(other_timestamp);
				
				if (other_value.isObject()) {
					ShMemObject deserialized_value = DeserializeObjectNode((ObjectNode)other_value);
					this.InsertAt(key, deserialized_value, new_timestamp);
					deserialized_value.parent = this;
					deserialized_value.parent_key = key;
				}
				else {
					this.InsertAt(key, other_value, new_timestamp);
				}
			}
		}
	}
				
	
	//
	// This method takes the serialized "release" deltas and merges them into the acquirer's 
	// ShMemObject. orig_timestamp is the timestamp at which the acquirer forked the releasing
	// process. 
	//
	/*
	public void merge(JSONObject release, ITimestamp orig_timestamp) 
		throws ShMemFailure {
		
		// Go through all the objects in the release set. 
		for (Object k : release.keySet()) {
			String key = (String)k;
			
			// Get the wrapper value. 
			JSONObject release_cur = (JSONObject)release.get(k);
			
			// The acquirer also contains the current key. 
			if (this.containsKey(k)) {
				
				// Pull the acquirer's timestamp from its wrapper. 
				JSONObject acquire_cur = (JSONObject)this.get(k);
				Object acquire_value = acquire_cur.get("value");
				ITimestamp acquire_timestamp = (ITimestamp)acquire_cur.get("shmem_timestamp");
				
				// Check if the releasing object contains a tombstone, if it does, then
				// the timestamps should not conflict. 
				if (!release_cur.containsKey("value")) {
					
					// The timestamps conflict: the acquiring process has modified/removed 
					// this subtree, while the releasing process has removed it altogether. 
					if (orig_timestamp.Compare(acquire_timestamp) == Comparison.LT) {
						throw new ShMemFailure("Merge failed!");
					}
					
					// We're safe, the acquiring process hasn't touched this subtree, we can 
					// safely remove it. The remove method will add a tombstone to the acquiring
					// process' ShMemObject. 
					else {
						ITimestamp released_ts = 
								VectorTimestamp.Deserialize((String)release_cur.get("shmem_timestamp"),
															cur_node_);
						this.delete(key, released_ts);
					}
				}
				
				// If we've reached this point, the value is NOT A TOMBSTONE. 
				Object release_value = release_cur.get("value");
				
				// If the acquiring process hasn't modified the timestamp since the original 
				// timestamp, we will always be safe. 
				if (orig_timestamp.Compare(acquire_timestamp) == Comparison.GE) {
					
					// If the releasing process contains a leaf here, just put the leaf value
					// into the acquiring process. 
					if (release_value.getClass() != JSONObject.class) {
						ITimestamp ts = (ITimestamp)VectorTimestamp.
								Deserialize((String)release_cur.get("shmem_timestamp"), cur_node_);
						insert(key, release_value, ts);
					}
					else {
						// 	XXX: Bug. We should be adding leaves here, not modifying the keys which
						// have not been modified by both the acuiring process and releasing process. 
						ShMemObject mod_child = json2shmem((JSONObject)release_value);
						if (acquire_value.getClass() == ShMemObject.class) {
							((ShMemObject)acquire_value).put_force(mod_child);
						}
						else {
							this.put(key,  mod_child);
						}
					}
				}
				
				// One of them is a leaf and both have been modified. 
				// This is a write-write conflict. 
				else if (acquire_value.getClass() != ShMemObject.class ||
						 release_value.getClass() != JSONObject.class) {
					throw new ShMemFailure("Merge failed!");
				}
				
				// They are both ShMemObjects (technically the releaser is a JSONObject,
				// but this is just a serialized version of a ShMemObject). Recursively
				// try to merge them. 
				else {
					((ShMemObject)acquire_value).merge((JSONObject)release_value, orig_timestamp);
				}
			}
			
			// The acquiring process does not contain a key here (that is, no tombstone and 
			// no subtree). 
			else {
				
				// If the acquirer does not contain anything, we're fine, we just do nothing. 
				// This condition garbage collects tombstones automatically. 
				if (release_cur.get("value") != null) {
					Object release_value = release_cur.get("value");
					ITimestamp ts = (ITimestamp)VectorTimestamp.
							Deserialize((String)release_cur.get("shmem_timestamp"), cur_node_);
					
					// If the releasing process contains an ShMemObject here, first convert from JSON
					// to ShMemObject and then we can just "put" it
					// into the acquiring process. 
					if (release_value.getClass() == JSONObject.class) {
						ShMemObject mod_child = json2shmem((JSONObject)release_value);
						insert(key, mod_child, ts);
					}
					else {
						insert(key, release_value, ts);
					}
				}
			}
		}
	}
	*/
}
