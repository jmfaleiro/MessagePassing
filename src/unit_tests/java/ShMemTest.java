package unit_tests.java;


import static org.junit.Assert.*;
import mp.java.ITimestamp.Comparison;
import mp.java.ShMem;
import mp.java.ShMemObject;
import mp.java.VectorTimestamp;

public class ShMemTest {

	@org.junit.Test
	public void testTimestamps() {
		VectorTimestamp.Init(3, 0);
		int[] start_time = VectorTimestamp.CreateZero();
		int[] temp = new int[3];
		for (int i = 0; i < 3; ++i) {
			temp[i] = 0;
		}
		
		assertEquals(VectorTimestamp.Compare(start_time,  temp),
					 Comparison.EQ);
		
		VectorTimestamp.IncrementLocal(start_time);
		temp[0] = 1;
		assertEquals(VectorTimestamp.Compare(start_time,  temp),
					 Comparison.EQ);
		temp[2] = 1;
		assertEquals(VectorTimestamp.Compare(start_time,  temp), 
					 Comparison.LT);
		for (int i = 0;i  < 3; ++i) {
			temp[i] = 0;
		}
		assertEquals(VectorTimestamp.Compare(start_time,  temp),
					 Comparison.GT);
		temp[1] = 1;
		assertEquals(VectorTimestamp.Compare(start_time,  temp),
					 Comparison.NONE);
		
		for (int i = 0; i < 3; ++i) {
			temp[i] = 4;
		}
		VectorTimestamp.CopyFromTo(temp,  start_time);
		assertEquals(VectorTimestamp.Compare(start_time,  temp),
					 Comparison.EQ);
		
		int[] other = {3,7,3};
		VectorTimestamp.Union(start_time,  other);
		assertEquals(VectorTimestamp.Compare(start_time,  temp),
					 Comparison.GT);
		temp[1] = 7;
		assertEquals(VectorTimestamp.Compare(start_time, temp),
					 Comparison.EQ);
		temp[0] = 5;
		temp[1] = 5;
		temp[2] = 5;
		
		assertEquals(VectorTimestamp.Compare(start_time,  temp),
					 Comparison.NONE);
					 
	}
	
	@org.junit.Test
	public void testPutGet() {
		ShMem.Init(0);
		ShMem.Start();
		int[] temp = {1, 0, 0, 0};
		assertEquals(VectorTimestamp.Compare(ShMemObject.s_now, temp),
					 Comparison.EQ);
		
		ShMemObject first_obj = new ShMemObject();
		first_obj.put("Yale",  "University");
		ShMem.s_state.put("name",  first_obj);
		
		ShMemObject temp_obj = (ShMemObject)ShMem.s_state.get("name");
		assertEquals(first_obj, temp_obj);
		assertEquals(temp_obj.get("Yale").getTextValue().equals("University"), true);
		
		VectorTimestamp.IncrementLocal(ShMemObject.s_now);
		temp[0] = 2;
		assertEquals(VectorTimestamp.Compare(ShMemObject.s_now, temp),
					 Comparison.EQ);
		
		temp[0] = 1;
		temp[1] = 0;
		temp[2] = 0;
		temp[3] = 0;
		first_obj.put("CS",  "Watson");
		int[] yale_timestamp = first_obj.m_timestamps.get("Yale");
		assertEquals(VectorTimestamp.Compare(yale_timestamp,  temp),
					 Comparison.EQ);
		int[] cs_timestamp = first_obj.m_timestamps.get("CS");
		temp[0] = 2;
		assertEquals(VectorTimestamp.Compare(cs_timestamp,  temp),
					 Comparison.EQ);
		int[] name_timestamp = ShMem.s_state.m_timestamps.get("name");
		assertEquals(VectorTimestamp.Compare(name_timestamp,  temp),
					 Comparison.EQ);
		
		ShMemObject.s_now[1] = 2;
		ShMemObject second_obj = new ShMemObject();
		second_obj.put("gah",  "blah");
		first_obj.put("second", second_obj);
		
		temp[0] = 2;
		temp[1] = 2;
		temp[2] = 0;
		temp[3] = 0;
		assertEquals(VectorTimestamp.Compare(name_timestamp,  temp),
					 Comparison.EQ);
		
	}
	
	/*
	@org.junit.Test
	public void test() {
		fail("Not yet implemented");
	}
	*/

}
