/*
Lonestar Benchmark Suite for irregular applications that exhibit 
amorphous data-parallelism.

Center for Grid and Distributed Computing
The University of Texas at Austin

Copyright (C) 2007, 2008, 2009 The University of Texas at Austin

Licensed under the Eclipse Public License, Version 1.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.eclipse.org/legal/epl-v10.html

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

File: Element.java 
*/

package DelaunayRefinement.src.java;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.*;

public class Element {
  
  private static final double MINANGLE = 30.0;

  public static boolean isDead(ObjectNode node) {
	  return node.get("dead").getBooleanValue();
  }
  
  public static void kill(ObjectNode node) {
	  node.put("dead",  true);
  }

  public static ObjectNode CreateNewElement(JsonNode a, JsonNode b, JsonNode c, Mesh nodes) {
	  
	  ObjectNode ret = Mesh.mapper.createObjectNode();
	  
	  ArrayNode coords = Mesh.mapper.createArrayNode();
	  
	  int index = nodes.getNextIndex();
	  ret.put("index",  index);
	  ret.put("dim",  3);
	  
	  if (JSONTuple.LessThan(b, a) || JSONTuple.LessThan(c, a)) {
		  if (JSONTuple.LessThan(b,  c)) {
			  coords.add(b);
			  coords.add(c);
			  coords.add(a);
		  }
		  else {
			  coords.add(c);
			  coords.add(a);
			  coords.add(b);
		  }
	  }
	  else {
		  coords.add(a);
		  coords.add(b);
		  coords.add(c);
	  }
	  
	  ret.put("coords",  coords);
	  
	  ArrayNode edges = Mesh.mapper.createArrayNode();
	  
	  edges.add(Edge.CreateEdge(coords.get(0), coords.get(1)));
	  edges.add(Edge.CreateEdge(coords.get(1),  coords.get(2)));
	  edges.add(Edge.CreateEdge(coords.get(2),  coords.get(0)));
	  
	  ret.put("edges", edges);
	  
	  boolean l_bObtuse = false;
	    boolean l_bBad = false;
	    int l_obtuse = -1;
	    for (int i = 0; i < 3; i++) {
	      double angle = getAngle(coords, 3, i);
	      if (angle > 90.1) {
	        l_bObtuse = true;
	        l_obtuse = (i + 1) % 3;
	      } else if (angle < MINANGLE) {
	        l_bBad = true;
	      }
	    }
	    boolean bBad = l_bBad;
	    ret.put("bBad",  l_bBad);
	    boolean bObtuse = l_bObtuse;
	    ret.put("bObtuse",  l_bObtuse);
	    ret.put("obtuse",  l_obtuse);
	    
	    JsonNode center = getCenter(coords.get(0), coords.get(1), coords.get(2));
	    ret.put("center",  center);
	    
	    double radius_squared = JSONTuple.DistanceSquared(center,  coords.get(0));
	    ret.put("radius_squared",  radius_squared);
	    ret.put("n0",  -1);
	    ret.put("n1", -1);
	    ret.put("n2",  -1);
	    ret.put("dead",  false);
	    nodes.putNode(index,  ret);
	    return ret;
  }
  
  
  public static int getObtuseNeighbor(ObjectNode node) throws Exception {
	  if (node.get("bObtuse").getBooleanValue()) {
		  switch (node.get("obtuse").getIntValue()) {
		  case 0:
			  return node.get("n0").getIntValue();
		  case 1:
			  return node.get("n1").getIntValue();
		  case 2:
			  return node.get("n2").getIntValue();
			  default:
				  throw new Exception("Unexpected obtuse index!");
		  }
	  }
	  else {
		  throw new Exception("Element is not obtuse!");
	  }
  }
  
  public static int getNeighbor(ObjectNode node, int index) {
	  switch (index) {
	  case 0:
		  return node.get("n0").getIntValue();
	  case 1:
		  return node.get("n1").getIntValue();
	  case 2:
		  return node.get("n2").getIntValue();
		  default:
			  return -1;
	  }
  }
  
  private static JsonNode getCenter(JsonNode coord0, 
		  				 	  		JsonNode coord1, 
		  				 	  		JsonNode coord2) {
	  
	  	double x1 = coord1.get(JSONTuple.x_index).getDoubleValue() - coord0.get(JSONTuple.x_index).getDoubleValue();
		double y1 = coord1.get(JSONTuple.y_index).getDoubleValue() - coord0.get(JSONTuple.y_index).getDoubleValue();
		double z1 = coord1.get(JSONTuple.z_index).getDoubleValue() - coord0.get(JSONTuple.z_index).getDoubleValue();
		
		double x2 = coord2.get(JSONTuple.x_index).getDoubleValue() - coord0.get(JSONTuple.x_index).getDoubleValue();
		double y2 = coord2.get(JSONTuple.y_index).getDoubleValue() - coord0.get(JSONTuple.y_index).getDoubleValue();
		double z2 = coord2.get(JSONTuple.z_index).getDoubleValue() - coord0.get(JSONTuple.z_index).getDoubleValue();
		
		double len1 = JSONTuple.Distance(coord0,  coord1);
		double len2 = JSONTuple.Distance(coord0,  coord2);
		
		double cosine = (x1*x2 + y1*y2 + z1*z2) / (len1 * len2);
		double sine_sq = 1.0 - cosine * cosine;
	    double plen = len2/len1;
	    double s = plen * cosine;
	    double t = plen * sine_sq;
	    double wp = (plen - cosine) / (2 * t);
	    double wb = 0.5 - (wp * s);
	    
	    double x_center = coord0.get(JSONTuple.x_index).getDoubleValue() * (1 - wb - wp);
	    double y_center = coord0.get(JSONTuple.y_index).getDoubleValue() * (1 - wb - wp);
	    double z_center = coord0.get(JSONTuple.z_index).getDoubleValue() * (1 - wb - wp);
	    
	    x_center += coord1.get(JSONTuple.x_index).getDoubleValue() * wb;
	    y_center += coord1.get(JSONTuple.y_index).getDoubleValue() * wb;
	    z_center += coord1.get(JSONTuple.z_index).getDoubleValue() * wb;
	    
	    x_center += coord2.get(JSONTuple.x_index).getDoubleValue() * wp;
	    y_center += coord2.get(JSONTuple.y_index).getDoubleValue() * wp;
	    z_center += coord2.get(JSONTuple.z_index).getDoubleValue() * wp;
	    
	    return JSONTuple.CreateTuple(x_center, y_center, z_center);
  }
  
  private static JsonNode getCenter(JsonNode coord0, JsonNode coord1) {
	  double x = (coord0.get(JSONTuple.x_index).getDoubleValue() + coord1.get(JSONTuple.x_index).getDoubleValue()) / 2.0;
	  double y = (coord0.get(JSONTuple.y_index).getDoubleValue() + coord1.get(JSONTuple.y_index).getDoubleValue()) / 2.0;
	  double z = (coord0.get(JSONTuple.z_index).getDoubleValue() + coord1.get(JSONTuple.z_index).getDoubleValue()) / 2.0; 
	  
	  return JSONTuple.CreateTuple(x, y, z);
  }

  public static ObjectNode CreateNewElement(JsonNode a, JsonNode b, Mesh nodes) {
	  
	  ObjectNode ret = Mesh.mapper.createObjectNode();
	  
	  int index = nodes.getNextIndex();
	  ret.put("index",  index);
	  ret.put("dim",  2);
	  ret.put("dead",  false);
	  
	  ArrayNode coords = Mesh.mapper.createArrayNode();
	  
	  
	  if (JSONTuple.LessThan(b, a)) {
		  coords.add(b);
		  coords.add(a);
	  }
	  else {
		  coords.add(a);
		  coords.add(b);
	  }
	  
	  ret.put("coords",  coords);
	  
	  ArrayNode edges = Mesh.mapper.createArrayNode();
	  edges.add(Element.Edge.CreateEdge(coords.get(0), coords.get(1)));
	  edges.add(Element.Edge.CreateEdge(coords.get(1), coords.get(0)));
	  
	  ret.put("edges",  edges);
	  ret.put("bBad", false);
	  ret.put("bObtuse", false);
	  ret.put("obtuse", -1);
	  
	  JsonNode center = getCenter(a, b);
	  double radius_squared = JSONTuple.DistanceSquared(center,  a);
	  ret.put("center",  center);
	  ret.put("radius_squared", radius_squared);
	  
	  ret.put("n0", -1);
	  
	  nodes.putNode(index,  ret);
	  return ret;
  }
  
  public static void resolveNeighbor(ObjectNode from, ObjectNode neighbor) throws Exception {
	  int related_index = isRelated(from, neighbor);
	  int neighbor_index = neighbor.get("index").getIntValue();
	  switch (related_index) {
	  case 0:
		  from.put("n0",  neighbor_index);
		  return;
	  case 1:
		  from.put("n1",  neighbor_index);
		  return;
	  case 2:
		  from.put("n2", neighbor_index);
		  return;
		  
		  // XXX: Not sure if I should silently fail here!
		  default:
			  throw new Exception("got an invalid neighbot index!");
	  }
  }

  public static void setNeighbor(ObjectNode node, int index, int value) throws Exception {
	  
	  
	  switch (index) {
	  case 0:
		  node.put("n0",  value);
		  return;
	  case 1:
		  node.put("n1",  value);
		  return;
	  case 2:
		  node.put("n2",  value);
		  return;
		  default:
			  throw new Exception("Got an invalid neighbor index!");
	  }
  }
  
  public static class Edge {
    
    public static JsonNode CreateEdge(JsonNode a, JsonNode b) {
    	ArrayNode ret = Mesh.mapper.createArrayNode();
    	if (JSONTuple.LessThan(a, b)) {
    		ret.add(a);
    		ret.add(b);
    	}
    	else {
    		ret.add(b);
    		ret.add(a);
    	}
    	int tmphashval = 17;
    	tmphashval = 37 * tmphashval + ret.get(0).get(JSONTuple.hash_index).getIntValue();
        tmphashval = 37 * tmphashval + ret.get(1).get(JSONTuple.hash_index).getIntValue();
        ret.add(tmphashval);
        return ret;
    }
    
    public static boolean Equals(JsonNode edge1, JsonNode edge2) {
    	return (JSONTuple.Equals(edge1.get(0), edge2.get(0)) &&
    			JSONTuple.Equals(edge1.get(1),  edge2.get(1)));
    }

    public static int HashCode(JsonNode edge) {
      return edge.get(2).getIntValue();
    }

    public static boolean NotEquals(JsonNode edge1, JsonNode edge2) {
    	return !Equals(edge1, edge2);
    }


    public static boolean LessThan(JsonNode edge1, JsonNode edge2) {
    	return JSONTuple.LessThan(edge1.get(0), edge2.get(0)) ||
    		   (JSONTuple.Equals(edge1.get(0),  edge2.get(0)) && JSONTuple.LessThan(edge1.get(1), edge2.get(1)));

    }


    public static boolean GreaterThan(JsonNode edge1, JsonNode edge2) {
      return JSONTuple.GreaterThan(edge1.get(0),  edge2.get(0)) ||
    		 (JSONTuple.Equals(edge1.get(0),  edge2.get(0)) && JSONTuple.GreaterThan(edge1.get(1),  edge2.get(1)));
     
    }


    public static JsonNode GetPoint(JsonNode edge, int i) {
    	return edge.get(i);
      
    }
  }

  
/*
  public boolean lessThan(Element e) {
    if (dim < e.getDim()) {
      return false;
    }
    if (dim > e.getDim()) {
      return true;
    }
    for (int i = 0; i < dim; i++) {
      if (coords[i].lessThan(e.coords[i])) {
        return true;
      } else if (coords[i].greaterThan(e.coords[i])) {
        return false;
      }
    }
    return false;
  }
  */
  
  public static int isRelated(JsonNode from, JsonNode e) {
	JsonNode my_edges = from.get("edges");
	JsonNode e_edges = e.get("edges");
    int edim = e.get("dim").getIntValue();
    int dim = from.get("dim").getIntValue();
    
    JsonNode my_edge, e_edge0, e_edge1, e_edge2 = null;
    my_edge = my_edges.get(0);
    e_edge0 = e_edges.get(0);
    if (Element.Edge.Equals(my_edge, e_edge0)) {
    	return 0;
    }
    e_edge1 = e_edges.get(1);
    if (my_edge.equals(e_edge1)) {
    	return 0;
    }
    
    if (edim == 3) {
      e_edge2 = e_edges.get(2);
      if (Element.Edge.Equals(my_edge, e_edge2)) {
        return 0;
      }
    }
    my_edge = my_edges.get(1);
    if (Element.Edge.Equals(my_edge,  e_edge0)) {
      return 1;
    }
    if (Element.Edge.Equals(my_edge, e_edge1)) {
      return 1;
    }
    if (edim == 3) {
      if (Element.Edge.Equals(my_edge,  e_edge2)) {
        return 1;
      }
    }
    if (dim == 3) {
      my_edge = my_edges.get(2);
      if (Element.Edge.Equals(my_edge, e_edge0)) {
        return 2;
      }
      if (Element.Edge.Equals(my_edge,  e_edge1)) {
        return 2;
      }
      if (edim == 3) {
        if (Element.Edge.Equals(my_edge, e_edge2)) {
          return 2;
        }
      }
    }
    return -1;
  }


 /*
  
  @Override
  public String toString() {
    String ret = "[";
    for (int i = 0; i < dim; i++) {
      ret +=  coords[i].toString();
      if (i != (dim - 1)) {
        ret += ", ";
      }
    }
    ret += "]";
    return ret;
  }
  */


  public static JsonNode center(ObjectNode node) {
    return node.get("center");
  }


  public static boolean inCircle(ObjectNode node, JsonNode p) {
	double ds = JSONTuple.Distance(node.get("center"), p);
	double radius_squared = node.get("radius_squared").getDoubleValue();
    return ds <= radius_squared;
  }


  public static double getAngle(JsonNode coords, int dim, int i) {
    int j = i + 1;
    if (j == dim) {
      j = 0;
    }
    int k = j + 1;
    if (k == dim) {
      k = 0;
    }
    JsonNode a = coords.get(i);
    JsonNode b = coords.get(j);
    JsonNode c = coords.get(k);
    return JSONTuple.angle(b, a, c);
  }

  public static int getIndex(ObjectNode node) {
	  return node.get("index").getIntValue();
  }
  
  public static JsonNode getEdge(ObjectNode node, int i) {
    return node.get("edges").get(i);
  }


  public static JsonNode getPoint(ObjectNode node, int i) {
    return node.get("coords").get(i);
  }

  
  // should the node be processed?
  public static boolean isBad(ObjectNode node) {
    return node.get("bBad").getBooleanValue();
  }


  public static int getDim(ObjectNode node) {
    return node.get("dim").getIntValue();
  }


  public static int numEdges(ObjectNode node) {
	int dim = getDim(node);
    return 2*dim - 3;
  }


  public static boolean isObtuse(ObjectNode node) {
	  return node.get("bObtuse").getBooleanValue();
  }


  public static JsonNode getRelatedEdge(ObjectNode from, ObjectNode e) {
    // Scans all the edges of the two elements and if it finds one that is
    // equal, then sets this as the Edge of the EdgeRelation
    int edim = getDim(e);
    int dim = getDim(from);
    JsonNode my_edge, e_edge0, e_edge1, e_edge2 = null;
    
    my_edge = getEdge(from, 0);
    e_edge0 = getEdge(e, 0);
    if (Element.Edge.Equals(my_edge,  e_edge0)) {
      return my_edge;
    }
    e_edge1 = getEdge(e, 1);
    if (my_edge.equals(e_edge1)) {
      return my_edge;
    }
    if (edim == 3) {
      e_edge2 = getEdge(e, 2);
      if (my_edge.equals(e_edge2)) {
        return my_edge;
      }
    }
    my_edge = getEdge(e, 1);
    if (my_edge.equals(e_edge0)) {
      return my_edge;
    }
    if (my_edge.equals(e_edge1)) {
      return my_edge;
    }
    if (edim == 3) {
      if (my_edge.equals(e_edge2)) {
        return my_edge;
      }
    }
    if (dim == 3) {
      my_edge = getEdge(e, 2);
      if (my_edge.equals(e_edge0)) {
        return my_edge;
      }
      if (my_edge.equals(e_edge1)) {
        return my_edge;
      }
      if (edim == 3) {
        if (my_edge.equals(e_edge2)) {
          return my_edge;
        }
      }
    }
    return null;
  }
}
