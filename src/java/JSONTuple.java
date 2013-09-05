/*
 * Created by Jose Faleiro at Yale University
 * faleiro.jose.manuel@gmail.com
 */

package DelaunayRefinement.src.java;

import org.json.simple.JSONObject;

public class JSONTuple {
	
	private final JSONObject m_tuple;
	
	public JSONTuple(double a, double b, double c) {
		m_tuple = new JSONObject();
		m_tuple.put("x", a);
		m_tuple.put("y", b);
		m_tuple.put("z", c);
		
		int tmphashvalue = 17;
	    long tmp = Double.doubleToLongBits(a);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    tmp = Double.doubleToLongBits(b);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    tmp = Double.doubleToLongBits(c);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    
	    m_tuple.put("hashvalue",  tmphashvalue);
	}
	
	public JSONTuple(JSONTuple other) {
		m_tuple = new JSONObject();
		
		double x_other = (Double)other.m_tuple.get("x");
		double y_other = (Double)other.m_tuple.get("y");
		double z_other = (Double)other.m_tuple.get("z");
		int hash_other = (Integer)other.m_tuple.get("hashvalue");
		
		m_tuple.put("x",  x_other);
		m_tuple.put("y", y_other);
		m_tuple.put("z",  z_other);
		m_tuple.put("hashvalue", hash_other);
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
		return (Integer)this.m_tuple.get("hashcode");
	}
	
	public boolean notEquals(JSONTuple rhs) {
	    return !equals(rhs);
	}
	
	public boolean lessThan(JSONTuple rhs) {
		JSONObject rhs_obj = rhs.m_tuple;
		return JSONTuple.LessThan(this.m_tuple, rhs_obj);
	}
	
	public boolean greaterThan(JSONTuple rhs) {
		JSONObject rhs_obj = rhs.m_tuple;
		return JSONTuple.GreaterThan(this.m_tuple,  rhs_obj);
	}
	
	public JSONTuple add(JSONTuple rhs) {
		double x1 = (Double)rhs.m_tuple.get("x");
		double y1 = (Double)rhs.m_tuple.get("y");
		double z1 = (Double)rhs.m_tuple.get("z");
		
		x1 += (Double)m_tuple.get("x");
		y1 += (Double)m_tuple.get("y");
		z1 += (Double)m_tuple.get("z");
		
		return new JSONTuple(x1, y1, z1);
	}
	
	public JSONTuple subtract(JSONTuple rhs) {
		double x1 = (Double)m_tuple.get("x");
		double y1 = (Double)m_tuple.get("y");
		double z1 = (Double)m_tuple.get("z");
		
		x1 -= (Double)rhs.m_tuple.get("x");
		y1 -= (Double)rhs.m_tuple.get("y");
		z1 -= (Double)rhs.m_tuple.get("z");
		
		return new JSONTuple(x1, y1, z1);
	}
	
	public double dotp(JSONTuple rhs) {
		double x1 = (Double)rhs.m_tuple.get("x");
		double y1 = (Double)rhs.m_tuple.get("y");
		double z1 = (Double)rhs.m_tuple.get("z");
		
		double x2 = (Double)m_tuple.get("x");
		double y2 = (Double)m_tuple.get("y");
		double z2 = (Double)m_tuple.get("z");
		
		return x1*x2 + y1*y2 + z1*z2;
	}
	
	public JSONTuple scale(double value) {
		double x1 = (Double)m_tuple.get("x");
		double y1 = (Double)m_tuple.get("y");
		double z1 = (Double)m_tuple.get("z");
		
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
		return Math.sqrt(distance(rhs));
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
		return new String("(" + (Double)m_tuple.get("x") + ", " + (Double)m_tuple.get("y") + ", " + (Double)m_tuple.get("z") + ")");
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
	
	public static JSONObject CreateTuple(double a, double b, double c){
		
		JSONObject new_coord = new JSONObject();
		
		// Add the coordinates to the JSONObject. 
		new_coord.put("x", a);
		new_coord.put("y", b);
		new_coord.put("z", c);
		
		// Compute hash value. 
		int tmphashvalue = 17;
	    long tmp = Double.doubleToLongBits(a);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    tmp = Double.doubleToLongBits(b);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    tmp = Double.doubleToLongBits(c);
	    tmphashvalue = 37 * tmphashvalue + (int) (tmp ^ (tmp >>> 32));
	    new_coord.put("hashvalue", tmphashvalue);
	    
	    return new_coord;
	}
	
	public static double Angle(JSONObject vertex, JSONObject end1, JSONObject end2) {
		double x_diff1 = (Double)end1.get("x") - (Double)vertex.get("x");
		double y_diff1 = (Double)end1.get("y") - (Double)vertex.get("y");
		double z_diff1 = (Double)end1.get("z") - (Double)vertex.get("z");
		
		double x_diff2 = (Double)end2.get("x") - (Double)vertex.get("x");
		double y_diff2 = (Double)end2.get("y") - (Double)vertex.get("y");
		double z_diff2 = (Double)end2.get("z") - (Double)vertex.get("z");
		
		double diff_dotp = x_diff1 * x_diff2 + y_diff1 * y_diff2 + z_diff1 * z_diff2;
		double d = diff_dotp / (Math.sqrt(DistanceSquared(vertex, end1) * DistanceSquared(vertex, end2)));
		return (180 / Math.PI) * Math.acos(d);
	}
	
	public static double Distance(JSONObject first, JSONObject second) {
		return Math.sqrt(DistanceSquared(first, second));
	}
	
	public static double DistanceSquared(JSONObject first, JSONObject second) {
		double x_diff = (Double)first.get("x") - (Double)second.get("x");
		double y_diff = (Double)first.get("y") - (Double)second.get("y");
		double z_diff = (Double)first.get("z") - (Double)second.get("z");
		
		return x_diff * x_diff + y_diff * y_diff + z_diff * z_diff;
	}
	
	private static double dotp(JSONObject first, JSONObject second) {
		return (((Double)first.get("x") * (Double)second.get("x")) +
				((Double)first.get("y") * (Double)second.get("y")) +
				((Double)first.get("z") * (Double)second.get("z")));
	}
	
	public static boolean GreaterThan(JSONObject coord1, JSONObject coord2) {
		double x1 = (Double)coord1.get("x");
		double y1 = (Double)coord1.get("y");
		double z1 = (Double)coord1.get("z");
		
		double x2 = (Double)coord2.get("x");
		double y2 = (Double)coord2.get("y");
		double z2 = (Double)coord2.get("z");
		
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
	
	public static boolean LessThan(JSONObject coord1, JSONObject coord2) {
		double x1 = (Double)coord1.get("x");
		double y1 = (Double)coord1.get("y");
		double z1 = (Double)coord1.get("z");
		
		double x2 = (Double)coord2.get("x");
		double y2 = (Double)coord2.get("y");
		double z2 = (Double)coord2.get("z");
		
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

	private static boolean Equals(JSONObject coord1, JSONObject coord2) {
		double x1 = (Double)coord1.get("x");
		double y1 = (Double)coord1.get("y");
		double z1 = (Double)coord1.get("z");
		
		double x2 = (Double)coord2.get("x");
		double y2 = (Double)coord2.get("y");
		double z2 = (Double)coord2.get("z");
		
		return (x1 == x2) && (y1 == y2) && (z1 == z2);
	}
}
