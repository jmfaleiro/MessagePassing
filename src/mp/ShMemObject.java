package mp;

import mp.Diff.DiffType;

import org.json.simple.*;
import java.util.*;

import org.apache.commons.lang3.tuple.*;

// TODO: 1) *Enforce* the rule that an object can have just a single reference
public class ShMemObject extends JSONObject {
	
	protected ShMemObject parent;
	protected String parent_key;
	
	public static long fork_id_cur;
	
	public ShMemObject() {
		super();
		parent = null;
		parent_key = "";
	}
	
	public static ShMemObject json2shmem(JSONObject root) throws ShMemFailure {
		
		ShMemObject ret = new ShMemObject();
		for (Object k : root.keySet()) {
			
			String key = (String)k;
			JSONObject wrapper_value = (JSONObject)root.get(k);
			
			Object value = wrapper_value.get("value");
			
			if (value.getClass() == JSONObject.class) {
				
				ShMemObject child = json2shmem((JSONObject)value);
				child.parent = ret;
				child.parent_key = key;
				value = child;
			}
			
			ret.put(key, value);
		}
		return ret;
	}
	
	
	//
	// Recursively select the timestamps from the object for merging.
	//
	public static JSONObject get_diff_tree(ShMemObject obj, long from_ts) {
		
		JSONObject ret = new JSONObject();
		List<Pair<String, JSONObject>> children = new ArrayList<Pair<String, JSONObject>>();
		
		for (Object k : obj.keySet()) {
			
			assert k.getClass() == String.class;
			
			// All values are encapsulated in a JSONObject to record timestamp
			// information. 
			JSONObject val = (JSONObject)obj.get(k);
			
			// We only bother adding the subtree to diffs if it has been updated since the
			// reference timestamp.
			long timestamp = (Long)obj.get("shmem_timestamp");
			if (timestamp > from_ts) {
				
				JSONObject to_add = new JSONObject();
				to_add.put("shmem_timestamp",  timestamp);
				
				Object value = obj.get("value");
				
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
	
	// This method interposes on JSONObject's put method. 
	// Records the diff to send to the joiner. 
	//
	// Invariant: If we're "PUT"-ting an ShMemObject, then the difflist
	//			  in the entire tree is *empty*. 
	//
	public Object put(String key, Object value) throws ShMemFailure{
		
		JSONObject to_add = new JSONObject();
		to_add.put("shmem_timestamp",  fork_id_cur);
		
		if (value.getClass() == ShMemObject.class) {
			
			ShMemObject sh_mem_value = (ShMemObject)value;
			long timestamp = (Long)sh_mem_value.get("shmem_timestamp");
			if (timestamp != fork_id_cur) {
				throw new ShMemFailure("Inconsistent timestamps!");
			}
			
			sh_mem_value.parent = this;
			sh_mem_value.parent_key = key;
		}
		
		update_difftree(this);
		return super.put(key, value);
	}
	
	public Object get(String key)  {
		
		JSONObject wrapper = (JSONObject)super.get(key);
		return wrapper.get("value");
	}
	
	public Object remove(String key) throws ShMemFailure {
		Object ret = super.remove(key);
		
		// Update diff meta data
		update_difftree(this);
		JSONObject to_add = new JSONObject();
		to_add.put("shmem_timestamp",  fork_id_cur);
		to_add.put("value",  null);
		
		return ret;
	}
	
	private static void update_difftree(ShMemObject cur) throws ShMemFailure{
		
		
		while (cur.parent != null) {
			
			String key = cur.parent_key;
			JSONObject val = (JSONObject)cur.parent.get(key);
			long cur_timestamp = (Long)val.get("shmem_timestamp");
			
			// First check if we really need to update the timestamp.
			// Invariant: The ancestor's timestamp >= current timestamp, 
			// if it's already equal, then we don't care. 
			assert cur_timestamp <= fork_id_cur;
			
			if (cur_timestamp != fork_id_cur) {
				val.put("shmem_timestamp",  fork_id_cur);
				cur = cur.parent;
			}
			
			// We don't have to do anything in this case, the ancestors will already
			// contain the required timestamp. 
			else {
				break;
			}
		}
		
	}
	
	
	private void fix_timestamps(JSONObject obj, long ts) {
		
		obj.remove("shmem_timestamp");
		List<Pair<String, JSONObject>> fixed_children = new ArrayList<Pair<String, JSONObject>>();
		
		for (Object k : obj.keySet()) {
			fix_timestamps((JSONObject)obj.get(k), ts);
		}
		
		obj.put("shmem_timestamp",  ts);
	}
	
	public void merge(ShMemObject acquire, JSONObject release, long orig_timestamp) 
		throws ShMemFailure {
		
		for (Object k : release.keySet()) {
			
			String key = (String)k;
			JSONObject release_cur = (JSONObject)release.get(k);
			
			if (acquire.containsKey(k)) {
				
				JSONObject acquire_cur = (JSONObject)acquire.get(k);
				Object acquire_value = acquire_cur.get("value");
				
				long acquire_timestamp = (Long)acquire_cur.get("shmem_timestamp");
				
			    // Get the case where the releaser removes an element out of the
				// way first. 
				if (release_cur.get("value") == null) {
					if (acquire_timestamp > orig_timestamp) {
						throw new ShMemFailure("Merge failed!");
					}
					else {
						acquire.remove(key);
					}
				}
				
				Object release_value = release_cur.get("value");
				
				// We're safe no matter what in this case. 
				if (acquire_timestamp <= orig_timestamp) {
					
					// It's a leaf
					if (release_value.getClass() != JSONObject.class) {
						acquire.put(key,  release_value);
					}
					else {
						ShMemObject mod_child = json2shmem((JSONObject)release_value);
						acquire.put(key,  mod_child);
					}
				}
				// One of them is a leaf and both have been modified. 
				// This is a write-write conflict. 
				else if (acquire_value.getClass() != ShMemObject.class ||
						 release_value.getClass() != JSONObject.class) {
					throw new ShMemFailure("Merge failed!");
				}
				// They are both ShMemObjects (technically the releaser is a JSONObject,
				// but this is just a serialized version of a ShMemObject). 
				else {
					merge((ShMemObject)acquire_value, (JSONObject)release_value, orig_timestamp);
				}
			}
			else {
				
				// Discard the tombstone, we don't need it anymore. 
				if (release_cur.get("value") != null) {
					Object release_value = release_cur.get("value");
					
					if (release_value.getClass() == JSONObject.class) {
						ShMemObject mod_child = json2shmem((JSONObject)release_value);
						acquire.put(key, mod_child);
					}
				}
			}
		}
	}
	
	public JSONObject merge(JSONObject parent_obj, JSONObject child_obj, long orig_timestamp) 
			throws ShMemFailure {
	
		JSONObject ret;
		
		// These timestamps are the max timestamp of any leaf in the tree.
		long child_max_timestamp = (Long)child_obj.get("shmem_timestamp");
		long parent_max_timestamp = (Long)parent_obj.get("shmem_timestamp");
		
		// The parent hasn't touched this tree since forking.
		if (parent_max_timestamp == orig_timestamp) {
			fix_timestamps(child_obj, orig_timestamp);
			ret = child_obj;
		}
		
		// The child hasn't touched the tree
		else if (child_max_timestamp == orig_timestamp) { 
			ret = parent_obj;
		}
		
		// One is a leaf that is, has modified this key in particular, while the other
		// has modified some subtree. This should be a write-write race. 
		else if ((parent_obj.keySet().size() == 1) || (child_obj.keySet().size() == 1)) {
			throw new ShMemFailure("Merge failure!");
		}
		
		// Try to merge the trees recursively on every child node.
		else {
			
			List<Pair<Object, JSONObject>> merges = new ArrayList<Pair<Object, JSONObject>>();
			Set<String> done_keys = new HashSet<String>();
			
			for (Object parent_key : parent_obj.keySet()) {
				
				assert parent_key.getClass() == String.class;
				
				// The child contains the key as well, we have to recursively check both.
				if (child_obj.containsKey(parent_key)) {
					
					JSONObject parent_next = (JSONObject)parent_obj.get(parent_key);
					JSONObject child_next = (JSONObject)child_obj.get(parent_key);
					
					// Merge the subtrees and put it in a queue to insert into the parent
					// later.
					JSONObject merge_next = merge(parent_next, child_next, orig_timestamp);
					Pair<Object, JSONObject> to_add = Pair.of(parent_key,  merge_next);
					merges.add(to_add);
				}
				
				done_keys.add((String)parent_key);
			}
			
			// Insert all the merged subtrees from above. 
			for (Pair<Object, JSONObject> kvp : merges) {
				parent_obj.put(kvp.getLeft(), kvp.getRight());
			}
			
			for (Object child_key : child_obj.keySet()) {
				
				assert child_key.getClass() == String.class;
				
				// Insert only new keys
				if (!done_keys.contains((String) child_key)) {
					
					JSONObject cur_child_obj = (JSONObject)child_obj.get(child_key);
					
					// Fix the timestamps in the child so they reflect the current timestamp. 
					// The child has its own notion of time with respect to the parent. 
					fix_timestamps(cur_child_obj, orig_timestamp);
					parent_obj.put(child_key,  cur_child_obj);
				}
			}
			ret = parent_obj;
		}
		return ret;
	}
	
	
	
	//
	// Recursively walk up the tree and append the path of the object in the tree
	//
	public static String getPath(ShMemObject obj) {
		
		String ret;
		if (obj.parent != null) {
			ret = "";
		}
		else {
			ret = getPath(obj.parent);
			// We want patterns of the form key1:key2:...:keyn
			if (ret == "") {
				ret = obj.parent_key;
			}
			else {
				ret += ":" + obj.parent_key;
			}
		}
		return ret;
	}
}
