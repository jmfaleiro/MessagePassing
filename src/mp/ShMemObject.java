package mp;

import mp.Diff.DiffType;

import org.json.simple.*;
import java.util.*;

import org.apache.commons.lang3.tuple.*;

/*
 * Users should use ShMemObject where the intended to use JSONObjects. A user may still
 * user JSONObjects but they will not be interpreted in any way. If both the acquiring and
 * releasing processes make changes to unrelated parts of a JSONObject, the updates will be
 * marked conflicting. 
 */
public class ShMemObject extends JSONObject {
	
	// Points to the parent of this object in the recursive ShMemObject hierarchy. 
	private ShMemObject parent;
	
	// The key the parent uses to refer to this object. We need the key to update timestamp
	// information in the parent recursively. 
	private String parent_key;
	
	// The current value of "time". All puts to an ShMemObject will take this value. The value
	// is changed whenever we fork. 
	public static long fork_id_cur;
	
	// Initialize an empty ShMemObject. 
	public ShMemObject() {
		super();
		this.parent = null;
		this.parent_key = "";
	}
	
	// This method is used to parse out an ShMemObject after it has been serialized to
	// a JSONObject. Used when we transmit diffs to the acquirer. 
	public static ShMemObject json2shmem(JSONObject root) throws ShMemFailure {
		ShMemObject ret = new ShMemObject();
		
		// 1. Go through the keys and wrap them into JSONObjects. 
		// 2. Add each wrapped value into the ShMemObject's map. 
		// Do the above recursively.. 
		for (Object k : root.keySet()) {
				
			// Get the key.
			String key = (String)k;
			
			// Get the wrapped value. 
			JSONObject wrapper_value = (JSONObject)root.get(k);
			Object value = wrapper_value.get("value");
			
			// Put the value into a wrapper (to_add in this case). 
			JSONObject to_add = new JSONObject();
			to_add.put("shmem_timestamp",  fork_id_cur);
			if (value.getClass() == JSONObject.class) {
				
				// Recursively do this on this object's children. 
				ShMemObject child = json2shmem((JSONObject)value);
				value = child;
			}
			
			// Finally, put the value into the current ShMemObject. 
			ret.put(key, value);
		}
		return ret;
	}
	
	
	//
	// Recursively select the timestamps from the object for merging.
	//
	public static JSONObject get_diff_tree(ShMemObject obj, long from_ts) {
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
			long timestamp = (Long)val.get("shmem_timestamp");
			if (timestamp > from_ts) {
				JSONObject to_add = new JSONObject();
				to_add.put("shmem_timestamp",  timestamp);
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
	
	// This method interposes on JSONObject's put method. 
	// Records the diff to send to the joiner. 
	//
	// Invariant: If we're "PUT"-ting an ShMemObject, then the difflist
	//			  in the entire tree is *empty*. 
	//
	public Object put(String key, Object value) throws ShMemFailure{
		
		// We wrapp the value in the "to_add" JSONObject.
		JSONObject to_add = new JSONObject();
		
		// Add the timestamp to the wrapper. 
		to_add.put("shmem_timestamp",  fork_id_cur);
		
		// If the value is an ShMemObject class, update its parent and parent_key information.
		// The parent is the current object (this) and the key is the 'key' argument. 
		if (value.getClass() == ShMemObject.class) {
			ShMemObject sh_mem_value = (ShMemObject)value;
			sh_mem_value.parent = this;
			sh_mem_value.parent_key = key;
				
			/*
			// The object is currently empty.
			if (sh_mem_value.keySet().size() != 0) {
				
				// Make sure that there is *some* key 
				boolean consistent = false;
				for (Object k : sh_mem_value.keySet()) {
					
					String inner_key = (String)k;
					long timestamp = (Long)(((JSONObject)sh_mem_value.get(k)).get("shmem_timestamp"));
					if (timestamp == fork_id_cur) {
						consistent = true;
						break;
					}
				}
			}
			*/
		}
		
		// Recursively update this ShMemObject's parents' timestamps. 
		update_difftree(this);
		
		// Finally, put the actual value into the wrapper and insert the wrapper into
		// 'this'. 
		to_add.put("value", value);
		return super.put(key, to_add);
	}
	
	// 
	// We need a new "get" method because values are wrapped inside a timestamped JSONObject. 
	// We remove the raw value from its wrapper and return the raw value. 
	// 
	public Object get(String key)  {
		
		JSONObject wrapper = (JSONObject)super.get(key);
		return wrapper.get("value");
	}
	
	// 
	// We can't just remove a key from the map because the remove is still an UPDATE. 
	// We insert a tombstone as the leaf (null), which will be interpreted by the merge
	// process appropriately. Merge understands when to garbage collect tombstones. 
	//
	// XXX: This method looks buggy!
	public Object remove(String key) throws ShMemFailure {
		Object ret = super.remove(key);
		
		// Update diff meta data
		update_difftree(this);
		JSONObject to_add = new JSONObject();
		to_add.put("shmem_timestamp",  fork_id_cur);
		to_add.put("value",  null);
		
		return ret;
	}
	
	// Returns the timestamped wrapper. 
	private Object get_wrapper(String key) {
		return super.get(key);
	}
	
	// Recursively update the given ShMemObject's (cur) ancestor's timestamps
	// to reflect a new update. 
	private static void update_difftree(ShMemObject cur) throws ShMemFailure{
		while (cur.parent != null) {
			String key = cur.parent_key;
			
			// Get cur's wrapper in its parent. 
			JSONObject val = (JSONObject)cur.parent.get_wrapper(key);
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
	
	//
	// This method takes the serialized "release" deltas and merges them into the acquirer's 
	// ShMemObject. orig_timestamp is the timestamp at which the acquirer forked the releasing
	// process. 
	//
	public void merge(ShMemObject acquire, JSONObject release, long orig_timestamp) 
		throws ShMemFailure {
		
		// Go through all the objects in the release set. 
		for (Object k : release.keySet()) {
			String key = (String)k;
			
			// Get the wrapper value. 
			JSONObject release_cur = (JSONObject)release.get(k);
			
			// The acquirer also contains the current key. 
			if (acquire.containsKey(k)) {
				
				// Pull the acquirer's timestamp from its wrapper. 
				JSONObject acquire_cur = (JSONObject)acquire.get(k);
				Object acquire_value = acquire_cur.get("value");
				long acquire_timestamp = (Long)acquire_cur.get("shmem_timestamp");
				
				// Check if the releasing object contains a tombstone, if it does, then
				// the timestamps should not conflict. 
				if (release_cur.get("value") == null) {
					
					// The timestamps conflict: the acquiring process has modified/removed 
					// this subtree, while the releasing process has removed it altogether. 
					if (acquire_timestamp > orig_timestamp) {
						throw new ShMemFailure("Merge failed!");
					}
					
					// We're safe, the acquiring process hasn't touched this subtree, we can 
					// safely remove it. The remove method will add a tombstone to the acquiring
					// process' ShMemObject. 
					else {
						acquire.remove(key);
					}
				}
				
				// If we've reached this point, the value is NOT A TOMBSTONE. 
				Object release_value = release_cur.get("value");
				
				// If the acquiring process hasn't modified the timestamp since the original 
				// timestamp, we will always be safe. 
				if (acquire_timestamp <= orig_timestamp) {
					
					// If the releasing process contains a leaf here, just put the leaf value
					// into the acquiring process. 
					if (release_value.getClass() != JSONObject.class) {
						acquire.put(key,  release_value);
					}
					else {
						// 	XXX: Bug here. We should be adding leaves here, not modifying the keys which
						// have not been modified by both the acuiring process and releasing process. 
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
				// but this is just a serialized version of a ShMemObject). Recursively
				// try to merge them. 
				else {
					merge((ShMemObject)acquire_value, (JSONObject)release_value, orig_timestamp);
				}
			}
			
			// The acquiring process does not contain a key here (that is, no tombstone and 
			// no subtree). 
			else {
				
				// If the acquirer does not contain anything, we're fine, we just do nothing. 
				// This condition garbage collects tombstones automatically. 
				if (release_cur.get("value") != null) {
					Object release_value = release_cur.get("value");
					
					// If the releasing process contains an ShMemObject here, first convert from JSON
					// to ShMemObject and then we can just "put" it
					// into the acquiring process. 
					if (release_value.getClass() == JSONObject.class) {
						ShMemObject mod_child = json2shmem((JSONObject)release_value);
						acquire.put(key, mod_child);
					}
					
					// If it's a leaf, just put it into the acquiring process. 
					else {
						acquire.put(key, release_value);
					}
				}
			}
		}
	}
}
