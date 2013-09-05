/*
 * Created by Jose Faleiro at Yale University
 * faleiro.jose.manuel@gmail.com
 */

package DelaunayRefinement.src.java;

import org.json.simple.JSONObject;

public class JSONTuple {
	
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

	public static boolean Equals(JSONObject coord1, JSONObject coord2) {
		double x1 = (Double)coord1.get("x");
		double y1 = (Double)coord1.get("y");
		double z1 = (Double)coord1.get("z");
		
		double x2 = (Double)coord2.get("x");
		double y2 = (Double)coord2.get("y");
		double z2 = (Double)coord2.get("z");
		
		return (x1 == x2) && (y1 == y2) && (z1 == z2);
	}
}
