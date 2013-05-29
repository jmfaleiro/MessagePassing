package mp;

import org.json.simple.*;
import org.json.simple.parser.*;

public class VectorTimestamp implements ITimestamp {

	private long[] time_;
	private int vector_size_;
	private int local_index_;
		
	public ITimestamp Copy() {
		VectorTimestamp ret = new VectorTimestamp();
		ret.vector_size_ = vector_size_;
		ret.local_index_ = local_index_;
		ret.time_ = time_.clone();
		return ret;
	}
	
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
	
	public Comparison Compare(ITimestamp other) throws ShMemFailure {
		
		// Do some error handling. 
		if (other.getClass() != VectorTimestamp.class) {
			throw new ShMemFailure("Got a non-vector timestamp to compare with");
		}
		
		VectorTimestamp other_time = (VectorTimestamp)other;
		if (other_time.vector_size_ != vector_size_) {
			throw new ShMemFailure("Vector sizes are not the same!");
		}
		
		boolean less_than = false;
		boolean bigger_equal = false;
		
		for (int i = 0; i < vector_size_; ++i) {
			if (time_[i] >= other_time.time_[i]) {
				bigger_equal = true;
			}
			else {
				less_than = true;
			}
		}
		
		if (less_than && bigger_equal) {
			return Comparison.NONE;
		}
		else if (bigger_equal) {
			return Comparison.GE;
		}
		else {
			return Comparison.LT;
		}
	}
	
	public void Union(ITimestamp other) throws ShMemFailure {
		if (other.getClass() != VectorTimestamp.class) {
			throw new ShMemFailure("Other timestamp is not of type VectorTimestamp!");
		}
		VectorTimestamp other_vect = (VectorTimestamp)other;
		if (other_vect.vector_size_ != vector_size_) {
			throw new ShMemFailure("Vector sizes don't match!");
		}
		for (int i = 0; i < vector_size_; ++i) {
			time_[i] = time_[i] >= other_vect.time_[i] ? time_[i] : other_vect.time_[i];
		}
	}

	public void LocalIncrement() {
		++time_[local_index_];
	}

}
