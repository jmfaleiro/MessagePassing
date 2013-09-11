package mp;

public interface ITimestamp {

	public enum Comparison {
		LT,
		GE,
		NONE,
	}	
	
	String Serialize();
	Comparison Compare(ITimestamp other);
	void LocalIncrement();
	ITimestamp Copy();
	void Union(ITimestamp other);
}
