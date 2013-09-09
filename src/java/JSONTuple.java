/*
 * Created by Jose Faleiro at Yale University
 * faleiro.jose.manuel@gmail.com
 */

package DelaunayRefinement.src.java;


import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

public class JSONTuple {
	
	public static final int x_index = 0;
	public static final int y_index = 1;
	public static final int z_index = 2;
	public static final int hash_index = 3;
	
	private final ArrayNode m_tuple;
	
	public JSONTuple(double a, double b, double c) {
		m_tuple = Mesh.mapper.createArrayNode();
		m_tuple.add(a);
		m_tuple.add(b);
		m_tuple.add(c);
		
		int tmphashvalue = 17;
	    long tmp = Double.doubleToLongBits(a);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    tmp = Double.doubleToLongBits(b);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    tmp = Double.doubleToLongBits(c);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    
	    m_tuple.add(tmphashvalue);
	}
	
	public JSONTuple(JSONTuple other) {
		m_tuple = Mesh.mapper.createArrayNode();
		
		double x_other = other.m_tuple.get(x_index).getDoubleValue();
		double y_other = (Double)other.m_tuple.get(y_index).getDoubleValue();
		double z_other = (Double)other.m_tuple.get(z_index).getDoubleValue();
		int hash_other = (Integer)other.m_tuple.get(hash_index).getIntValue();
		
		m_tuple.add(x_other);
		m_tuple.add(y_other);
		m_tuple.add(z_other);
		m_tuple.add(hash_other);
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof JSONTuple)) {
			return false;
		}
		JSONTuple t = (JSONTuple)other;
		return Equals(this.m_tuple, t.m_tuple);
	}
	
	@Override
	public int hashCode() {
		return this.m_tuple.get(hash_index).getIntValue();
	}
	
	public boolean notEquals(JSONTuple rhs) {
	    return !equals(rhs);
	}
	
	public boolean lessThan(JSONTuple rhs) {
		ArrayNode rhs_obj = rhs.m_tuple;
		return JSONTuple.LessThan(this.m_tuple, rhs_obj);
	}
	
	public boolean greaterThan(JSONTuple rhs) {
		ArrayNode rhs_obj = rhs.m_tuple;
		return JSONTuple.GreaterThan(this.m_tuple,  rhs_obj);
	}
	
	public JSONTuple add(JSONTuple rhs) {
		double x1 = rhs.m_tuple.get(x_index).getDoubleValue();
		double y1 = rhs.m_tuple.get(y_index).getDoubleValue();
		double z1 = rhs.m_tuple.get(z_index).getDoubleValue();
		
		x1 += m_tuple.get(x_index).getDoubleValue();
		y1 += m_tuple.get(y_index).getDoubleValue();
		z1 += m_tuple.get(z_index).getDoubleValue();
		
		return new JSONTuple(x1, y1, z1);
	}
	
	public JSONTuple subtract(JSONTuple rhs) {
		double x1 = m_tuple.get(x_index).getDoubleValue();
		double y1 = m_tuple.get(y_index).getDoubleValue();
		double z1 = m_tuple.get(z_index).getDoubleValue();
		
		x1 -= rhs.m_tuple.get(x_index).getDoubleValue();
		y1 -= rhs.m_tuple.get(y_index).getDoubleValue();
		z1 -= rhs.m_tuple.get(z_index).getDoubleValue();
		
		return new JSONTuple(x1, y1, z1);
	}
	
	public double dotp(JSONTuple rhs) {
		double x1 = rhs.m_tuple.get(x_index).getDoubleValue();
		double y1 = rhs.m_tuple.get(y_index).getDoubleValue();
		double z1 = rhs.m_tuple.get(z_index).getDoubleValue();
		
		double x2 = m_tuple.get(x_index).getDoubleValue();
		double y2 = m_tuple.get(y_index).getDoubleValue();
		double z2 = m_tuple.get(z_index).getDoubleValue();
		
		return x1*x2 + y1*y2 + z1*z2;
	}
	
	public JSONTuple scale(double value) {
		double x1 = m_tuple.get(x_index).getDoubleValue();
		double y1 = m_tuple.get(y_index).getDoubleValue();
		double z1 = m_tuple.get(z_index).getDoubleValue();
		
		return new JSONTuple(value*x1, value*y1, value*z1);
	}
	
	public int cmp(JSONTuple rhs) {
		if (equals(rhs)) {
			return 0;
		}
		else if (greaterThan(rhs)) {
			return 1;
		}
		else {
			return -1;
		}
	}
	
	public double distance(JSONTuple rhs) {
		return Math.sqrt(distance_squared(rhs));
	}
	
	public double distance_squared(JSONTuple rhs) {
	    return JSONTuple.DistanceSquared(this.m_tuple,  rhs.m_tuple);
	}
	
	// angle between a, the current tuple, and c
	public double angle(JSONTuple a, JSONTuple c) {
	    return Angle(this.m_tuple, a.m_tuple, c.m_tuple);
	}
	
	/*
	public static String ToString(JsonNode tuple) {
		double x = tuple.get(x_index).getDoubleValue();
		double y = tuple.get(y_index).getDoubleValue();
		double z = tuple.get(z_index).getDoubleValue();
		
		return new String("(" + x + ", " + y + ", " + z + ")");
	}
	*/
	
	public static int cmp(JSONTuple a, JSONTuple b) {
		return a.cmp(b);
	}
	
	public static double distance(JSONTuple a, JSONTuple b) {
		return a.distance(b);
	}
	
	public static double angle(JsonNode a, JsonNode b, JsonNode c) {
		return Angle(b, a, c);
	}
	
	public static ArrayNode CreateTuple(double a, double b, double c){
		
		ArrayNode new_coord = Mesh.mapper.createArrayNode();
		
		// Add the coordinates to the JSONObject. 
		new_coord.add(a);
		new_coord.add(b);
		new_coord.add(c);
		
		// Compute hash value. 
		int tmphashvalue = 17;
	    long tmp = Double.doubleToLongBits(a);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    tmp = Double.doubleToLongBits(b);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    tmp = Double.doubleToLongBits(c);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    new_coord.add(tmphashvalue);
	    
	    return new_coord;
	}
	
	public static double Angle(JsonNode vertex, JsonNode end1, JsonNode end2) {
		double x_diff1 = end1.get(0).getDoubleValue() - vertex.get(0).getDoubleValue();
		double y_diff1 = end1.get(1).getDoubleValue() - vertex.get(1).getDoubleValue();
		double z_diff1 = end1.get(2).getDoubleValue() - vertex.get(2).getDoubleValue();
		
		double x_diff2 = end2.get(0).getDoubleValue() - vertex.get(0).getDoubleValue();
		double y_diff2 = end2.get(1).getDoubleValue() - vertex.get(1).getDoubleValue();
		double z_diff2 = end2.get(2).getDoubleValue() - vertex.get(2).getDoubleValue();
		
		double diff_dotp = x_diff1 * x_diff2 + y_diff1 * y_diff2 + z_diff1 * z_diff2;
		double d = diff_dotp / (Math.sqrt(DistanceSquared(vertex, end1) * DistanceSquared(vertex, end2)));
		return (180 / Math.PI) * Math.acos(d);
	}
	
	public static double Distance(JsonNode first, JsonNode second) {
		return Math.sqrt(DistanceSquared(first, second));
	}
	
	public static double DistanceSquared(JsonNode first, JsonNode second) {
		double x_diff = first.get(0).getDoubleValue() - second.get(0).getDoubleValue();
		double y_diff = first.get(1).getDoubleValue() - second.get(1).getDoubleValue();
		double z_diff = first.get(2).getDoubleValue() - second.get(2).getDoubleValue();
		
		return x_diff * x_diff + y_diff * y_diff + z_diff * z_diff;
	}
	
	private static double dotp(ArrayNode first, ArrayNode second) {
		return ((first.get(0).getDoubleValue() * second.get(0).getDoubleValue()) +
				(first.get(1).getDoubleValue() * second.get(1).getDoubleValue()) +
				(first.get(2).getDoubleValue() * second.get(2).getDoubleValue()));
	}
	
	public static boolean GreaterThan(JsonNode coord1, JsonNode coord2) {
		double x1 = coord1.get(0).getDoubleValue();
		double y1 = coord1.get(1).getDoubleValue();
		double z1 = coord1.get(2).getDoubleValue();
		
		double x2 = coord2.get(0).getDoubleValue();
		double y2 = coord2.get(1).getDoubleValue();
		double z2 = coord2.get(2).getDoubleValue();
		
		if (x1 > x2) {
			return true;
		}
		if (x2 > x1) {
			return false;
		}
		if (y1 > y2) {
			return true;
		}
		if (y2 > y1) {
			return false;
		}
		if (z1 > z2) {
			return true;
		}
		if (z2 > z1) {
			return false;
		}
		
		// they're equal
		return false;	
	}
	
	public static boolean LessThan(JsonNode coord1, JsonNode coord2) {
		double x1 = coord1.get(0).getDoubleValue();
		double y1 = coord1.get(1).getDoubleValue();
		double z1 = coord1.get(2).getDoubleValue();
		
		double x2 = coord2.get(0).getDoubleValue();
		double y2 = coord2.get(1).getDoubleValue();
		double z2 = coord2.get(2).getDoubleValue();
		
		if (x1 < x2) {
			return true;
		}
		if (x2 < x1) {
			return false;
		}
		if (y1 < y2) {
			return true;
		}
		if (y2 < y1) {
			return false;
		}
		if (z1 < z2) {
			return true;
		}
		if (z2 < z1) {
			return false;
		}
		
		// they're equal
		return false;	
	}

	public static boolean Equals(JsonNode coord1, JsonNode coord2) {
		double x1 = coord1.get(0).getDoubleValue();
		double y1 = coord1.get(1).getDoubleValue();
		double z1 = coord1.get(2).getDoubleValue();
		
		double x2 = coord2.get(0).getDoubleValue();
		double y2 = coord2.get(1).getDoubleValue();
		double z2 = coord2.get(2).getDoubleValue();
		
		return (x1 == x2) && (y1 == y2) && (z1 == z2);
	}
	
	public double get(int index) {
		return m_tuple.get(index).getDoubleValue();
	}
}
