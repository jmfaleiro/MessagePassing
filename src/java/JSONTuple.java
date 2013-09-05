/*
 * Created by Jose Faleiro at Yale University
 * faleiro.jose.manuel@gmail.com
 */

package DelaunayRefinement.src.java;

import org.json.simple.JSONArray;

public class JSONTuple {
	
	public static final int x_index = 0;
	public static final int y_index = 1;
	public static final int z_index = 2;
	public static final int hash_index = 3;
	
	private final JSONArray m_tuple;
	
	public JSONTuple(double a, double b, double c) {
		m_tuple = new JSONArray();
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
		m_tuple = new JSONArray();
		
		double x_other = (Double)other.m_tuple.get(x_index);
		double y_other = (Double)other.m_tuple.get(y_index);
		double z_other = (Double)other.m_tuple.get(z_index);
		int hash_other = (Integer)other.m_tuple.get(hash_index);
		
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
		return (Integer)this.m_tuple.get(hash_index);
	}
	
	public boolean notEquals(JSONTuple rhs) {
	    return !equals(rhs);
	}
	
	public boolean lessThan(JSONTuple rhs) {
		JSONArray rhs_obj = rhs.m_tuple;
		return JSONTuple.LessThan(this.m_tuple, rhs_obj);
	}
	
	public boolean greaterThan(JSONTuple rhs) {
		JSONArray rhs_obj = rhs.m_tuple;
		return JSONTuple.GreaterThan(this.m_tuple,  rhs_obj);
	}
	
	public JSONTuple add(JSONTuple rhs) {
		double x1 = (Double)rhs.m_tuple.get(x_index);
		double y1 = (Double)rhs.m_tuple.get(y_index);
		double z1 = (Double)rhs.m_tuple.get(z_index);
		
		x1 += (Double)m_tuple.get(x_index);
		y1 += (Double)m_tuple.get(y_index);
		z1 += (Double)m_tuple.get(z_index);
		
		return new JSONTuple(x1, y1, z1);
	}
	
	public JSONTuple subtract(JSONTuple rhs) {
		double x1 = (Double)m_tuple.get(x_index);
		double y1 = (Double)m_tuple.get(y_index);
		double z1 = (Double)m_tuple.get(z_index);
		
		x1 -= (Double)rhs.m_tuple.get(x_index);
		y1 -= (Double)rhs.m_tuple.get(y_index);
		z1 -= (Double)rhs.m_tuple.get(z_index);
		
		return new JSONTuple(x1, y1, z1);
	}
	
	public double dotp(JSONTuple rhs) {
		double x1 = (Double)rhs.m_tuple.get(x_index);
		double y1 = (Double)rhs.m_tuple.get(y_index);
		double z1 = (Double)rhs.m_tuple.get(z_index);
		
		double x2 = (Double)m_tuple.get(x_index);
		double y2 = (Double)m_tuple.get(y_index);
		double z2 = (Double)m_tuple.get(z_index);
		
		return x1*x2 + y1*y2 + z1*z2;
	}
	
	public JSONTuple scale(double value) {
		double x1 = (Double)m_tuple.get(x_index);
		double y1 = (Double)m_tuple.get(y_index);
		double z1 = (Double)m_tuple.get(z_index);
		
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
	
	@Override
	public String toString() {
		return new String("(" + (Double)m_tuple.get(x_index) + ", " + (Double)m_tuple.get(y_index) + ", " + (Double)m_tuple.get(z_index) + ")");
	}
	
	public static int cmp(JSONTuple a, JSONTuple b) {
		return a.cmp(b);
	}
	
	public static double distance(JSONTuple a, JSONTuple b) {
		return a.distance(b);
	}
	
	public static double angle(JSONTuple a, JSONTuple b, JSONTuple c) {
		return b.angle(a, c);
	}
	
	public static JSONArray CreateTuple(double a, double b, double c){
		
		JSONArray new_coord = new JSONArray();
		
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
	
	public static double Angle(JSONArray vertex, JSONArray end1, JSONArray end2) {
		double x_diff1 = (Double)end1.get(0) - (Double)vertex.get(0);
		double y_diff1 = (Double)end1.get(1) - (Double)vertex.get(1);
		double z_diff1 = (Double)end1.get(2) - (Double)vertex.get(2);
		
		double x_diff2 = (Double)end2.get(0) - (Double)vertex.get(0);
		double y_diff2 = (Double)end2.get(1) - (Double)vertex.get(1);
		double z_diff2 = (Double)end2.get(2) - (Double)vertex.get(2);
		
		double diff_dotp = x_diff1 * x_diff2 + y_diff1 * y_diff2 + z_diff1 * z_diff2;
		double d = diff_dotp / (Math.sqrt(DistanceSquared(vertex, end1) * DistanceSquared(vertex, end2)));
		return (180 / Math.PI) * Math.acos(d);
	}
	
	public static double Distance(JSONArray first, JSONArray second) {
		return Math.sqrt(DistanceSquared(first, second));
	}
	
	public static double DistanceSquared(JSONArray first, JSONArray second) {
		double x_diff = (Double)first.get(0) - (Double)second.get(0);
		double y_diff = (Double)first.get(1) - (Double)second.get(1);
		double z_diff = (Double)first.get(2) - (Double)second.get(2);
		
		return x_diff * x_diff + y_diff * y_diff + z_diff * z_diff;
	}
	
	private static double dotp(JSONArray first, JSONArray second) {
		return (((Double)first.get(0) * (Double)second.get(0)) +
				((Double)first.get(1) * (Double)second.get(1)) +
				((Double)first.get(2) * (Double)second.get(2)));
	}
	
	public static boolean GreaterThan(JSONArray coord1, JSONArray coord2) {
		double x1 = (Double)coord1.get(0);
		double y1 = (Double)coord1.get(1);
		double z1 = (Double)coord1.get(2);
		
		double x2 = (Double)coord2.get(0);
		double y2 = (Double)coord2.get(1);
		double z2 = (Double)coord2.get(2);
		
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
	
	public static boolean LessThan(JSONArray coord1, JSONArray coord2) {
		double x1 = (Double)coord1.get(0);
		double y1 = (Double)coord1.get(1);
		double z1 = (Double)coord1.get(2);
		
		double x2 = (Double)coord2.get(0);
		double y2 = (Double)coord2.get(1);
		double z2 = (Double)coord2.get(2);
		
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

	private static boolean Equals(JSONArray coord1, JSONArray coord2) {
		double x1 = (Double)coord1.get(0);
		double y1 = (Double)coord1.get(1);
		double z1 = (Double)coord1.get(2);
		
		double x2 = (Double)coord2.get(0);
		double y2 = (Double)coord2.get(1);
		double z2 = (Double)coord2.get(2);
		
		return (x1 == x2) && (y1 == y2) && (z1 == z2);
	}
	
	public double get(int index) {
		return (Double)m_tuple.get(index);
	}
}
