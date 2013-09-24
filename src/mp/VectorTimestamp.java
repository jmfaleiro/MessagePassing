package mp;

import mp.ITimestamp.Comparison;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.IntNode;
import org.json.simple.*;
import org.json.simple.parser.*;

public class VectorTimestamp {

	public static int s_vector_size;
	public static int s_local_index;
	public static int[] s_default;
	public static int[] s_zero;
	
	private static int[] s_scratch_space;
	
	public static void CreateDefault() {
		int[] ret = new int[s_vector_size];
		for (int i = 0; i < s_vector_size; ++i) {
			if (i == s_local_index) {
				ret[i] = 1;
			}
			else {
				ret[i] = 0;
			}
		}
		
		s_default = ret;
	}
	
	public static int[] CreateZero() {
		int[] ret = new int[s_vector_size];
		for (int i = 0; i < s_vector_size; ++i) {
			ret[i] = 0;
		}
		return ret;
	}
	
	public static void CopyFromTo(int[] from, int[] to) {
		for (int i = 0; i < s_vector_size; ++i) {
			to[i] = from[i];
		}
	}
	
	public static void CopyFromSerializedTo(ArrayNode from, int[] to) {
		for (int i = 0; i < s_vector_size; ++i) {
			to[i] = from.get(i).getIntValue();
		}
	}
	
	public static ArrayNode toArrayNode(int[] vector) {
		ArrayNode ret = ShMem.mapper.createArrayNode();
		for (int i = 0; i < s_vector_size; ++i) {
			ret.add(vector[i]);
		}
		return ret;
	}
	
	
	public static int[] CopySerialized(ArrayNode node) {
		int[] ret = new int[s_vector_size];
		for (int i = 0; i < s_vector_size; ++i) {
			ret[i] = node.get(i).getIntValue();
		}
		return ret;
	}
	public static int[] Copy(int[] node) {
		int[] ret = new int[s_vector_size];
		for (int i = 0; i < s_vector_size; ++i) {
			ret[i] = node[i];
		}
		return ret;
	}
	
	public static void Union(int[] result, int[] with) {
		
		for (int i = 0; i < s_vector_size; ++i) {
			if (with[i] > result[i]) {
				result[i] = with[i];
			}
		}
	}
	
	public static void UnionWithSerialized(int[] result, ArrayNode with) {
		to_scratch(with);
		Union(result, s_scratch_space);
	}
	
	/*
	public static void MergeTime(ArrayNode vector1, ArrayNode vector2) {
		for (int i = 0; i < s_vector_size; ++i) {
			int value1 = vector1.get(i).getIntValue();
			int value2 = vector2.get(i).getIntValue();
			vector1.insert(i,  value1 > value2? value1 : value2);
		}
	}
	*/
	
	public static void IncrementLocal(int[] vector) {
		vector[s_local_index] += 1;
	}
	
	private static void to_scratch(ArrayNode serialized_vector) {
		for (int i = 0; i < s_vector_size; ++i) {
			s_scratch_space[i] = serialized_vector.get(i).getIntValue();
		}
	}
	
	public static Comparison CompareWithSerializedTS(int[] vector,
													 ArrayNode serialized_vector) {
		to_scratch(serialized_vector);
		return Compare(vector, s_scratch_space);
	}
	
	public static Comparison Compare(int[] vector1, int[] vector2) {
		boolean less_than = false;
		boolean bigger_than = false;
		
		int len = s_vector_size;
		for (int i = 0; i < len; ++i) {
			bigger_than |= vector1[i] > vector2[i];
			less_than |= vector2[i] > vector1[i];
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
