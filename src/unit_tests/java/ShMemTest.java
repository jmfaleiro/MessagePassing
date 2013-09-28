package unit_tests.java;


import static org.junit.Assert.*;

import java.util.Iterator;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

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
	
	private void standardInit() {
		ShMem.InitTest(0);
		ShMem.Start();
		
		ShMemObject first_obj = new ShMemObject();
		first_obj.put("Yale",  "University");
		ShMem.s_state.put("name", first_obj);
		
		VectorTimestamp.IncrementLocal(ShMemObject.s_now);
		first_obj.put("CS",  "Watson");
		
		ShMemObject.s_now[0] = 2;
		ShMemObject.s_now[1] = 2;
		
		ShMemObject second_obj = new ShMemObject();
		second_obj.put("gah",  "blah");
		first_obj.put("second",  second_obj);
	}
	
	private void checkGah(JsonNode diff) {
		JsonNode name_wrapper = diff.get("name");
		ArrayNode name_timestamp = (ArrayNode)name_wrapper.get("shmem_timestamp");
		int[] temp = {2,2,0,0};
		assertEquals(VectorTimestamp.CompareWithSerializedTS(temp,  name_timestamp),
					 Comparison.EQ);
		JsonNode second = name_wrapper.get("value");
		JsonNode second_wrapper = second.get("second");
		ArrayNode second_timestamp = (ArrayNode)second_wrapper.get("shmem_timestamp");
		assertEquals(VectorTimestamp.CompareWithSerializedTS(temp, second_timestamp),
					 Comparison.EQ);
		JsonNode gah = second_wrapper.get("value");
		JsonNode gah_wrapper = gah.get("gah");
		ArrayNode gah_timestamp = (ArrayNode)gah_wrapper.get("shmem_timestamp");
		assertEquals(VectorTimestamp.CompareWithSerializedTS(temp,  gah_timestamp), 
					 Comparison.EQ);
		String blah_string = gah_wrapper.get("value").getTextValue();
		assertEquals(blah_string.equals("blah"), true);
	}
	
	private void checkWatson(JsonNode diff) {
		JsonNode name_wrapper = diff.get("name");
		JsonNode cs_wrapper = name_wrapper.get("value").get("CS");
		ArrayNode cs_timestamp = (ArrayNode)cs_wrapper.get("shmem_timestamp");
		int[] temp = {2,0,0,0};
		assertEquals(VectorTimestamp.CompareWithSerializedTS(temp,  cs_timestamp),
					 Comparison.EQ);
		assertEquals(cs_wrapper.get("value").getTextValue().equals("Watson"), true);
	}
	
	private void checkYale(JsonNode diff) {
		JsonNode name_wrapper = diff.get("name");
		JsonNode yale_wrapper = name_wrapper.get("value").get("Yale");
		ArrayNode yale_timestamp = (ArrayNode)yale_wrapper.get("shmem_timestamp");
		int[] temp = {1,0,0,0};
		assertEquals(VectorTimestamp.CompareWithSerializedTS(temp,  yale_timestamp),	
					 Comparison.EQ);
		String yale_value = yale_wrapper.get("value").getTextValue();
		assertEquals(yale_value.equals("University"), true);
	}
	
	@org.junit.After
	public void tearDown() {
		
	}
	
	@org.junit.Test
	public void testDiff() {
		standardInit();
		
		int[] temp = {2,3,0,0};
		JsonNode diff = ShMemObject.get_diff_tree(ShMem.s_state, temp);
		int key_count = 0;
		
		Iterator<String> key_iter =  diff.getFieldNames();
		while (key_iter.hasNext()) {
			key_count += 1;
		}
		assertEquals(key_count, 0);
		
		temp[1] = 1;
		diff = ShMemObject.get_diff_tree(ShMem.s_state, temp);
		checkGah(diff);
		
		temp[0] = 1;
		temp[1] = 0;
		diff = ShMemObject.get_diff_tree(ShMem.s_state,  temp);
		checkGah(diff);
		checkWatson(diff);
		
		temp[0] = 0;
		temp[1] = 0;
		diff = ShMemObject.get_diff_tree(ShMem.s_state, temp);
		checkGah(diff);
		checkWatson(diff);
		checkYale(diff);
	}
	
	@org.junit.Test
	public void testPutGet() {
		ShMem.InitTest(0);
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
		int[] yale_timestamp = first_obj.m_key_map.get("Yale").m_timestamp;
		assertEquals(VectorTimestamp.Compare(yale_timestamp,  temp),
					 Comparison.EQ);
		int[] cs_timestamp = first_obj.m_key_map.get("CS").m_timestamp; 
		temp[0] = 2;
		assertEquals(VectorTimestamp.Compare(cs_timestamp,  temp),
					 Comparison.EQ);
		int[] name_timestamp = ShMem.s_state.m_key_map.get("name").m_timestamp;
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
	
	private ArrayNode createSerializedTimestamp(int[] vector,
												ObjectMapper mapper) {
		ArrayNode ret = mapper.createArrayNode();
		for (int val : vector) {
			ret.add(val);
		}
		return ret;
	}
	
	@org.junit.Test
	public void testMerge() {
		standardInit();
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode to_merge = mapper.createObjectNode();
		
		int[] vector = {0,0,1,0};
		ArrayNode timestamp1 = createSerializedTimestamp(vector, 
														 mapper);
		ArrayNode timestamp2 = createSerializedTimestamp(vector,
														 mapper);
		
		ObjectNode yale_value = mapper.createObjectNode();
		yale_value.put("shmem_timestamp",  timestamp2);
		yale_value.put("value", "College");
		
		ObjectNode yale_node = mapper.createObjectNode();
		yale_node.put("Yale", yale_value);
		
		ObjectNode name_value = mapper.createObjectNode();
		name_value.put("shmem_timestamp", timestamp1);
		name_value.put("value", yale_node);
		
		ObjectNode name_object = mapper.createObjectNode();
		name_object.put("name", name_value);
		
		// The merge should fail. We're expecting a MergeException. 
		Throwable expected_exception = null;
		try {
			ShMem.s_state.merge(name_object);
		}
		catch (Exception ex) {
			expected_exception = ex;
		}
		assertTrue(expected_exception instanceof ShMemObject.MergeException);
		
		// Now try merging something that doesn't conflict. 
		expected_exception = null;
		standardInit();
		ObjectNode abc_value = mapper.createObjectNode();
		abc_value.put("value",  "xyz");
		abc_value.put("shmem_timestamp", timestamp1);
		ObjectNode abc_node = mapper.createObjectNode();
		abc_node.put("abc",  abc_value);
		
		// The merge should go through fine. 
		try {
			ShMem.s_state.merge(abc_node);
		}
		catch (Exception ex) {
			expected_exception = ex;
		}
		assertTrue(expected_exception == null);
		String xyz_string = ShMem.s_state.get("abc").getTextValue();
		assertTrue(xyz_string.equals("xyz"));
		
		standardInit();
		int[] new_timestamp = {2,2,1,0};
		timestamp1 = createSerializedTimestamp(new_timestamp, mapper);
		timestamp2 = createSerializedTimestamp(new_timestamp, mapper);
		
		yale_value.put("shmem_timestamp",  timestamp2);
		name_value.put("shmem_timestamp", timestamp1);
		expected_exception = null;
		try {
			ShMem.s_state.merge(name_object);
		}
		catch (Exception e) {
			expected_exception = e;
		}
		
		assertTrue(expected_exception == null);
		assertEquals(VectorTimestamp.Compare(ShMemObject.s_now, new_timestamp),
					Comparison.EQ);
		String college_string = ShMem.s_state.get("name").get("Yale").getTextValue();
		assertTrue(college_string.equals("College"));
		int[] yale_timestamp = ((ShMemObject)ShMem.s_state.get("name")).m_key_map.get("Yale").m_timestamp;
		assertEquals(VectorTimestamp.Compare(yale_timestamp,  new_timestamp), Comparison.EQ);
		int[] name_timestamp = ShMem.s_state.m_key_map.get("name").m_timestamp;
		assertEquals(VectorTimestamp.Compare(name_timestamp, new_timestamp), Comparison.EQ);
	}
	

}
