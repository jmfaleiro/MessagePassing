package mp;

import mp.Diff.DiffType;

import org.json.simple.*;
import java.util.*;


public class ShMemObject extends JSONObject {
	
	protected ShMemObject parent_;
	protected String parent_key_;
	
	public Object put(Object key, Object value) {
		
		if (value.getClass() == ShMemObject.class) {
			((ShMemObject)value).parent_ = this;
			assert key.getClass() == String.class;
			((ShMemObject)value).parent_key_ = (String)key;
		}
		return super.put(key,  value);
	}
	
	//
	// Recursively check whether subtrees are equal, if they are, then 
	//
	public List<Diff> DiffTrees(ShMemObject ref, ShMemObject cur) {
		
		List<Diff> ret = new ArrayList<Diff>();
		
		Set<Object> ref_keys = ref.keySet();
		Set<Object> cur_keys = cur.keySet();
		Set<Object> done_keys = new HashSet();
		
		for (Object key : ref_keys) {
			
			if (cur_keys.contains(key)) {
				
				Object ref_value = ref.get(key);
				Object cur_value = cur.get(key);
				
				List<String> prefix = getPath(cur);
				prefix.add((String)key);
				
				
				if (ref_value.getClass() != cur_value.getClass()) {
					ret.add(new Diff(DiffType.UPDATE, prefix, cur_value));
				}
				else if (ref_value.getClass() == ShMemObject.class){
					List<Diff> diffs = DiffTrees((ShMemObject)ref_value,
												 (ShMemObject)cur_value);
					
					ret.addAll(diffs);
				}
				else if (ref_value.getClass() == JSONArray.class) {
					ret.add(new Diff(DiffType.UPDATE, prefix, cur_value));
				}
				else {
					if (!ref_value.equals(cur_value)) {
						ret.add(new Diff(DiffType.UPDATE, prefix, cur_value));
					}
				}
			}
			else {
				
				List<String> prefix = getPath(cur);
				prefix.add((String)key);
				
				ret.add(new Diff(DiffType.DELETE, prefix, null));
			}
			done_keys.add(key);
		}
		
		for (Object key : cur_keys) {
			
			if (!done_keys.contains(key)) {
				
				List<String> prefix = getPath(cur);
				prefix.add((String)key);
				ret.add(new Diff(DiffType.ADD, prefix, null));
			}
		}
		
		return ret;
	}
	
	//
	// Recursively walk up the tree and append the path of the object in the tree
	//
	public static List<String> getPath(ShMemObject obj) {
		
		List<String> ret;
		if (obj.parent_ != null) {
			ret = new ArrayList<String>();
			return ret;
		}
		else {
			ret = getPath(obj.parent_);
			ret.add(obj.parent_key_);
			return ret;
		}
	}
}
