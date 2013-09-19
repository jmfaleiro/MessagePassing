package mp;

import mp.ITimestamp.Comparison;

import org.codehaus.jackson.node.ArrayNode;
import org.json.simple.*;
import org.json.simple.parser.*;

public class VectorTimestamp {

	public static int s_vector_size;
	public static int s_local_index;
	public static ArrayNode s_default;
	public static ArrayNode s_zero;
	
	public static void CreateDefault() {
		ArrayNode ret = ShMem.mapper.createArrayNode();
		for (int i = 0; i < s_vector_size; ++i) {
			if (i == s_local_index) {
				ret.add(1);
			}
			else {
				ret.add(0);
			}
		}
		
		s_default = ret;
	}
	
	public static ArrayNode CreateZero() {
		ArrayNode ret = ShMem.mapper.createArrayNode();
		for (int i = 0; i < s_vector_size; ++i) {
			ret.add(0);
		}
		return ret;
	}
	
	public static void CopyFromTo(ArrayNode from, ArrayNode to) {
		for (int i = 0; i < s_vector_size; ++i) {
			to.insert(i, from.get(i).getIntValue());
		}
	}
	
	public static ArrayNode Copy(ArrayNode node) {
		ArrayNode ret = ShMem.mapper.createArrayNode();
		for (int i = 0; i < s_vector_size; ++i) {
			ret.add(node.get(i));
		}
		return ret;
	}
	
	public static void Union(ArrayNode result, ArrayNode with) {
		int len = result.size();
		for (int i = 0; i < len; ++i) {
			int result_value = result.get(i).getIntValue();
			int with_value = with.get(i).getIntValue();
			
			result.insert(i, result_value > with_value?result_value : with_value);
		}
	}
	
	public static void MergeTime(ArrayNode vector1, ArrayNode vector2) {
		for (int i = 0; i < s_vector_size; ++i) {
			int value1 = vector1.get(i).getIntValue();
			int value2 = vector2.get(i).getIntValue();
			vector1.insert(i,  value1 > value2? value1 : value2);
		}
	}
	
	public static void IncrementLocal(ArrayNode vector) {
		vector.insert(s_local_index,  vector.get(s_local_index).getIntValue()+1);
	}
	
	public static Comparison Compare(ArrayNode vector1, ArrayNode vector2) {
		boolean less_than = false;
		boolean bigger_than = false;
		
		int len = s_vector_size;
		for (int i = 0; i < len; ++i) {
			int value1 = vector1.get(i).getIntValue();
			int value2 = vector2.get(i).getIntValue();
			bigger_than |= value1 > value2;
			less_than |= value2 > value1;
		}
		
		if (less_than && bigger_than) {
			return Comparison.NONE;
		}
		else if (less_than) {
			return Comparison.LT;
		}
		else if (bigger_than) {
			return Comparison.GT;
		}
		else {
			return Comparison.EQ;
		}
	}
	
	private long[] time_;
	private int vector_size_;
	private int local_index_;
	
	
	public String Serialize() {
		String ret = "";
		for (int i = 0; i < vector_size_; ++i) {
			ret += time_[i];
			ret += ',';
		}
		return ret.substring(0, ret.length()-1);
	}

	public static VectorTimestamp Deserialize(String s, int local_index) {
		VectorTimestamp ret = new VectorTimestamp();
		ret.local_index_ = local_index;
		String[] parts = s.split(",");
		ret.vector_size_ = parts.length;
		ret.time_ = new long[ret.vector_size_];
		for (int i = 0; i < parts.length; ++i) {
			ret.time_[i] = Long.parseLong(parts[i]);
		}
		return ret;
	}
	
	public static VectorTimestamp Default(int size, int index) {
		VectorTimestamp ret = new VectorTimestamp();
		ret.local_index_ = index;
		ret.vector_size_ = size;
		ret.time_ = new long[size];
		for (int i = 0; i < size; ++i) 
			ret.time_[i] = 0;
		return ret;
	}
}
