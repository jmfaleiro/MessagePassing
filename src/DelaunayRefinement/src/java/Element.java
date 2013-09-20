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

Modified by Jose Manuel Faleiro
faleiro.jose.manuel@gmail.com
*/

package DelaunayRefinement.src.java;

import mp.ShMemObject;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.json.simple.JSONObject;

public class Element {

  private static final double MINANGLE = 30.0;

  public static boolean isDead(ShMemObject node) {
	  return node.get("dead").getBooleanValue();
  }
  
  public static void kill(ObjectNode node) {
	  node.put("dead",  true);
  }

  public static ShMemObject CreateElement(JsonNode a, JsonNode b, JsonNode c, Mesh nodes) {
	  
	  ShMemObject ret = new ShMemObject();
	  
	  int index = nodes.getNextIndex();
	  ret.put("index",  index);
	  ret.put("dead",  false);
	  ret.put("dim",  3);
	  
	  ArrayNode coords = Mesh.mapper.createArrayNode();
	  if (JSONTuple.LessThan(b,  a) || JSONTuple.LessThan(c, a)) {
	    	if (JSONTuple.LessThan(b, c)) {
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
	  edges.add(Element.Edge.CreateEdge(coords.get(0), coords.get(1)));
	  edges.add(Element.Edge.CreateEdge(coords.get(1),  coords.get(2)));
	  edges.add(Element.Edge.CreateEdge(coords.get(2),  coords.get(0)));
	  ret.put("edges",  edges);
	  
	  boolean l_bObtuse = false;
	    boolean l_bBad = false;
	    int l_obtuse = -1;
	    for (int i = 0; i < 3; i++) {
	      double angle = getAngle(coords, i);
	      if (angle > 90.1) {
	        l_bObtuse = true;
	        l_obtuse = (i + 1) % 3;
	      } else if (angle < MINANGLE) {
	        l_bBad = true;
	      }
	    }
	    
	    ret.put("bBad",  l_bBad);
	    ret.put("bObtuse",  l_bObtuse);
	    ret.put("obtuse",  l_obtuse);
	    
	    JsonNode center = getCenter(coords.get(0), coords.get(1), coords.get(2));
	    double radius_squared = JSONTuple.DistanceSquared(center,  coords.get(0));
	    ret.put("center",  center);
	    ret.put("radius_squared", radius_squared);
	    
	    ret.put("n0",  -1);
	    ret.put("n1",  -1);
	    ret.put("n2",  -1);
	    
	    nodes.putNode(index, ret);
	    
	    return ret;
  }
  
  /*
  public Element(JsonNode a, JsonNode b, JsonNode c, Mesh nodes) {
	  int index = nodes.getNextIndex();
	 node = CreateElement(index, a, b, c, nodes);
	 nodes.putNode(index,  this);
	 
	 
	 index = nodes.getNextIndex();
	 dead = false;
	  
    dim = 3;
    coords = new JsonNode[3];
    coords[0] = a;
    coords[1] = b;
    coords[2] = c;
    if (JSONTuple.LessThan(b,  a) || JSONTuple.LessThan(c, a)) {
    	if (JSONTuple.LessThan(b, c)) {
    		coords[0] = b;
    		coords[1] = c;
    		coords[2] = a;
    	}
    	else {
    		coords[0] = c;
    		coords[1] = a;
    		coords[2] = b;
    	}
    	
    }
    
    edges = new Element.Edge[3];
    edges[0] = new Element.Edge(coords[0], coords[1]);
    edges[1] = new Element.Edge(coords[1], coords[2]);
    edges[2] = new Element.Edge(coords[2], coords[0]);
    boolean l_bObtuse = false;
    boolean l_bBad = false;
    int l_obtuse = -1;
    for (int i = 0; i < 3; i++) {
      double angle = getAngle(i);
      if (angle > 90.1) {
        l_bObtuse = true;
        l_obtuse = (i + 1) % 3;
      } else if (angle < MINANGLE) {
        l_bBad = true;
      }
    }
    bBad = l_bBad;
    bObtuse = l_bObtuse;
    obtuse = l_obtuse;
    
    center = getCenter(coords[0], coords[1], coords[2]);
    radius_squared = JSONTuple.DistanceSquared(coords[0],  center);
    
    neighbors = new int[3];
    for (int i = 0; i < 3; ++i) {
    	neighbors[i] = -1;
    }
    
    nodes.putNode(index,  this);
    
  }
  */
  
  public static int getObtuseNeighbor(ShMemObject node) throws Exception {
	  if (node.get("bObtuse").getBooleanValue()) {
		  switch (node.get("obtuse").getIntValue()) {
		  case 0:
			  return node.get("n0").getIntValue();
		  case 1:
			  return node.get("n1").getIntValue();
		  case 2:
			  return node.get("n2").getIntValue();
			  default:
				  throw new Exception("Invalid neighbor index!");
		  }
	  }
	  else {
		  throw new Exception("Element is not obtuse!");
	  }
  }
  
  public static int getNeighbor(ShMemObject node, int index) {
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
		
		double len1 = JSONTuple.Distance(coord0, coord1);
		double len2 = JSONTuple.Distance(coord0, coord2);
		
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
	    
	    return JSONTuple.CreateTuple(x_center,  y_center,  z_center);
  }
  
  private static JsonNode getCenter(JsonNode coord0, JsonNode coord1) {
	  double x = (coord0.get(JSONTuple.x_index).getDoubleValue() + coord1.get(JSONTuple.x_index).getDoubleValue()) / 2.0;
	  double y = (coord0.get(JSONTuple.y_index).getDoubleValue() + coord1.get(JSONTuple.y_index).getDoubleValue()) / 2.0;
	  double z = (coord0.get(JSONTuple.z_index).getDoubleValue() + coord1.get(JSONTuple.z_index).getDoubleValue()) / 2.0; 
	  
	  return JSONTuple.CreateTuple(x, y, z);
  }
  
  public static ShMemObject CreateElement(JsonNode a, JsonNode b, Mesh nodes) {
	  ShMemObject ret = new ShMemObject();
	  
	  int index = nodes.getNextIndex();
	  boolean dead = false;
	  ret.put("index",  index);
	  ret.put("dead",  dead);
	  ret.put("dim",  2);
	  
	  ArrayNode coords = Mesh.mapper.createArrayNode();
	  if (JSONTuple.LessThan(b,  a)) {
		  coords.add(b);
		  coords.add(a);
	  }
	  else {
		  coords.add(a);
		  coords.add(b);
	  }
	  ret.put("coords",  coords);
	  
	  ArrayNode edges = Mesh.mapper.createArrayNode();
	  edges.add(Element.Edge.CreateEdge(coords.get(0),  coords.get(1)));
	  edges.add(Element.Edge.CreateEdge(coords.get(1),  coords.get(0)));
	  ret.put("edges", edges);
	  
	  ret.put("bBad",  false);
	  ret.put("bObtuse",  false);
	  ret.put("obtuse",  -1);
	  
	  JsonNode center = getCenter(coords.get(0), coords.get(1));
	  double radius_squared = JSONTuple.DistanceSquared(center,  coords.get(0));
	  ret.put("center",  center);
	  ret.put("radius_squared",  radius_squared);
	  
	  ret.put("n0",  -1);
	  nodes.putNode(index,  ret);
	  return ret;
  }

  /*
  public Element(JsonNode a, JsonNode b, Mesh nodes) {
	  int index = nodes.getNextIndex();
		 node = CreateElement(index, a, b,nodes);
		 nodes.putNode(index,  this);
	  
	  index = nodes.getNextIndex();
	  dead = false;
	  
    dim = 2;
    coords = new JsonNode[2];
    coords[0] = a;
    coords[1] = b;
    if (JSONTuple.LessThan(b, a)) {
    	coords[0] = b;
    	coords[1] = a;
    }
    edges = new Element.Edge[2];
    edges[0] = new Element.Edge(coords[0], coords[1]);
    edges[1] = new Element.Edge(coords[1], coords[0]);
    bBad = false;
    bObtuse = false;
    obtuse = -1;
    center = getCenter(coords[0], coords[1]);
    radius_squared = JSONTuple.DistanceSquared(a,  center);
    
    neighbors = new int[1];
    neighbors[0] = -1;
    
    nodes.putNode(index,  this);
    
  }
  */
  
  public static void resolveNeighbor(ShMemObject node, ShMemObject neighbor) throws Exception {
	  int related_index = isRelated(node, neighbor);
	  int neighbor_index = neighbor.get("index").getIntValue();
	  if (related_index == -1) {
		  throw new Exception("Called with unrelated elements!");
	  }
	  
	  switch (related_index) {
	  case 0:
		  node.put("n0",  neighbor_index);
		  return;
	  case 1:
		  node.put("n1",  neighbor_index);
		  return;
	  case 2:
		  node.put("n2",  neighbor_index);
		  return;
		  default:
			  throw new Exception("Called with unrelated elements!");
	  }
  }

  public void setNeighbor(ShMemObject node, int index, int value) throws Exception {
	  int dim = node.get("dim").getIntValue();
	  if (dim == 2) {
		  if (index == 0) {
			  node.put("n0",  value);
			  return;
		  }
		  else {
			  throw new Exception("blargh!");
		  }
	  }
	  else {
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
				  throw new Exception("blargh!");
		  }
	  }
  }
  
  public static class Edge {
	
	  private JsonNode m_edge;
	  private int m_hashcode;
	  
    public static ArrayNode CreateEdge(JsonNode a, JsonNode b) {
    	ArrayNode ret = Mesh.mapper.createArrayNode();
    	if (JSONTuple.LessThan(a,  b)) {
    		ret.add(a);
    		ret.add(b);
    	}
    	else {
    		ret.add(b);
    		ret.add(a);
    	}
    	int tmphashval = 17;
    	tmphashval = 37*tmphashval + ret.get(0).get(JSONTuple.hash_index).getIntValue();
    	tmphashval = 37*tmphashval + ret.get(1).get(JSONTuple.hash_index).getIntValue();
    	ret.add(tmphashval);
    	return ret;
    }
    
    public Edge(JsonNode edge) {
    	m_edge = edge;
    	m_hashcode = edge.get(2).getIntValue();
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof Element.Edge) {
    		return Element.Edge.Equals(m_edge,  ((Element.Edge) o).m_edge);
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	return m_hashcode;
    }
    
    public JsonNode getPoint(int i) throws Exception{
    	if (i == 0) {
    		return m_edge.get(0);
    	}
    	else if (i == 1) {
    		return m_edge.get(1);
    	}
    	else {
    		throw new Exception("unexpected index");
    	}
    }
    
/*
    public Edge(JsonNode a, JsonNode b) {
    	
      if (JSONTuple.LessThan(a,  b)) {
        p1 = a;
        p2 = b;
      } else {
        p1 = b;
        p2 = a;
      }
      int tmphashval = 17;
      tmphashval = 37 * tmphashval + p1.get(JSONTuple.hash_index).getIntValue();
      tmphashval = 37 * tmphashval + p2.get(JSONTuple.hash_index).getIntValue();
      hashvalue = tmphashval;
    }
    */
    
    public static boolean Equals(JsonNode edge1, JsonNode edge2) {
    	JsonNode p1 = edge1.get(0);
    	JsonNode p2 = edge1.get(1);
    	
    	JsonNode ep1 = edge2.get(0);
    	JsonNode ep2 = edge2.get(1);
    	
    	return JSONTuple.Equals(p1, ep1) && JSONTuple.Equals(p2,  ep2);
    }

    /*
    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Edge)) {
        return false;
      }
      Edge edge = (Edge) obj;
      return JSONTuple.Equals(p1, edge.p1) && JSONTuple.Equals(p2, edge.p2); 
    }
    */
    
    public static int HashCode(JsonNode edge) {
    	return edge.get(2).getIntValue();
    }

/*
    @Override
    public int hashCode() {
      return hashvalue;
    }
*/
    
    public static boolean NotEqual(JsonNode edge1, JsonNode edge2) {
    	return !Equals(edge1, edge2);
    }

    /*
    public boolean notEqual(Edge rhs) {
      return !equals(rhs);
    }
*/
    
    public static boolean LessThan(JsonNode edge1, JsonNode edge2) {
    	JsonNode p1 = edge1.get(0);
    	JsonNode p2 = edge1.get(1);
    	
    	JsonNode ep1 = edge2.get(0);
    	JsonNode ep2 = edge2.get(1);
    	
    	return JSONTuple.LessThan(p1,  ep1) || (JSONTuple.Equals(p1,  ep1) && JSONTuple.LessThan(p2,  ep2));
    }

    /*
    public boolean lessThan(Edge rhs) {
    	return JSONTuple.LessThan(p1,  rhs.p1) || (JSONTuple.Equals(p1,  rhs.p1) && JSONTuple.LessThan(p2,  rhs.p2));
    	
    }
    */
    
    public static boolean GreaterThan(JsonNode edge1, JsonNode edge2) {
    	JsonNode p1 = edge1.get(0);
    	JsonNode p2 = edge1.get(1);
    	
    	JsonNode ep1 = edge2.get(0);
    	JsonNode ep2 = edge2.get(1);
    	
    	return JSONTuple.GreaterThan(p1,  ep1) || (JSONTuple.Equals(p1,  ep1) && JSONTuple.GreaterThan(p2,  ep2));
    }

/*
    public boolean greaterThan(Edge rhs) {
    	return JSONTuple.GreaterThan(p1,  rhs.p1) || (JSONTuple.Equals(p1,  rhs.p1) && JSONTuple.GreaterThan(p2,  rhs.p2));

    }
*/
    
    public static JsonNode GetPoint(JsonNode edge, int i) {
    	if (i == 0) {
    		return edge.get(0);
    	}
    	else if (i == 1) {
    		return edge.get(1);
    	}
    	else {
    		System.exit(-1);
    		return null;
    	}
    }

    /*
    public JsonNode getPoint(int i) {
      if (i == 0) {
        return p1;
      } else if (i == 1) {
        return p2;
      } else {
        System.exit(-1);
        return null;
      }
    }
    


    @Override
    public String toString() {
      return "<" + p1.toString() + ", " + p2.toString() + ">";
    }
    */
  }

  

  public boolean lessThan(ShMemObject node, ShMemObject e) {
	  int dim = node.get("dim").getIntValue();
	  int e_dim = e.get("dim").getIntValue();
    if (dim < e_dim) {
      return false;
    }
    if (dim > e_dim) {
      return true;
    }
    for (int i = 0; i < dim; i++) {
    	JsonNode my_coord = node.get("coords").get(i);
    	JsonNode e_coord = e.get("coords").get(i);
    	if (JSONTuple.LessThan(my_coord, e_coord)) {
        return true;
      } else if (JSONTuple.GreaterThan(my_coord, e_coord)) {
        return false;
      }
    }
    return false;
  }
  
  public static int isRelated(ShMemObject node, ShMemObject e) {
    int edim = e.get("dim").getIntValue();
    int dim = node.get("dim").getIntValue();
    JsonNode my_edge, e_edge0, e_edge1, e_edge2 = null;
    my_edge = getEdge(node, 0);
    e_edge0 = getEdge(e, 0);
    if (Element.Edge.Equals(my_edge,  e_edge0)) {
      return 0;
    }
    e_edge1 = getEdge(e, 1);
    if (Element.Edge.Equals(my_edge,  e_edge1)) {
      return 0;
    }
    if (edim == 3) {
      e_edge2 = getEdge(e, 2);
      if (Element.Edge.Equals(my_edge,  e_edge2)) {
        return 0;
      }
    }
    my_edge = getEdge(node, 1);
    if (Element.Edge.Equals(my_edge,  e_edge0)) { 
      return 1;
    }
    if (Element.Edge.Equals(my_edge,  e_edge1)) {
      return 1;
    }
    if (edim == 3) {
      if (Element.Edge.Equals(my_edge,  e_edge2)) {
        return 1;
      }
    }
    if (dim == 3) {
      my_edge = getEdge(node, 2);
      if (Element.Edge.Equals(my_edge,  e_edge0)) {
        return 2;
      }
      if (Element.Edge.Equals(my_edge,  e_edge1)) {
        return 2;
      }
      if (edim == 3) {
        if (Element.Edge.Equals(my_edge,  e_edge2)) {
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
      ret += coords[i].toString();
      if (i != (dim - 1)) {
        ret += ", ";
      }
    }
    ret += "]";
    return ret;
  }
*/

  public static JsonNode center(ShMemObject node) {
	  return node.get("center");
  }


  public static boolean inCircle(ShMemObject node, JsonNode p) {
	  JsonNode center = center(node);
	  double radius_squared = node.get("radius_squared").getDoubleValue();
	  double ds = JSONTuple.DistanceSquared(center,  p);
    return ds <= radius_squared;
  }


  public static double getAngle(JsonNode coords, int i) {
    int j = i + 1;
    if (j == 3) {
      j = 0;
    }
    int k = j + 1;
    if (k == 3) {
      k = 0;
    }
    JsonNode a = coords.get(i);
    JsonNode b = coords.get(j);
    JsonNode c = coords.get(k);
    return JSONTuple.angle(b, a, c);
  }

  public static int getIndex(ShMemObject node) {
	  return node.get("index").getIntValue();
  }
  
  public static JsonNode getEdge(ShMemObject node, int i) {
	  return node.get("edges").get(i);
  }


  public static JsonNode getPoint(ShMemObject node, int i) {
	  return node.get("coords").get(i);
  }

  
  // should the node be processed?
  public static boolean isBad(ShMemObject node) {
	  return node.get("bBad").getBooleanValue();
  }


  public static int getDim(ShMemObject node) {
	  return node.get("dim").getIntValue();
  }


  public static int numEdges(ShMemObject node) {
	  int dim = getDim(node);
    return dim + dim - 3;
  }


  public static boolean isObtuse(ShMemObject node) {
	  return node.get("bObtuse").getBooleanValue();
  }


  public JsonNode getRelatedEdge(ShMemObject node, ShMemObject e) {
    // Scans all the edges of the two elements and if it finds one that is
    // equal, then sets this as the Edge of the EdgeRelation
    int edim = getDim(e);
    int dim = getDim(node);
    JsonNode my_edge, e_edge0, e_edge1, e_edge2 = null;
    my_edge = getEdge(node, 0);
    e_edge0 = getEdge(e, 0);
    if (Element.Edge.Equals(my_edge,  e_edge0)) {
      return my_edge;
    }
    e_edge1 = getEdge(e, 1);
    if (Element.Edge.Equals(my_edge,  e_edge1)) {
      return my_edge;
    }
    if (edim == 3) {
      e_edge2 = getEdge(e, 2);
      if (Element.Edge.Equals(my_edge,  e_edge2)) {
        return my_edge;
      }
    }
    my_edge = getEdge(node, 1);
    if (Element.Edge.Equals(my_edge,  e_edge0)) {
      return my_edge;
    }
    if (Element.Edge.Equals(my_edge,  e_edge1)) {
      return my_edge;
    }
    if (edim == 3) {
      if (Element.Edge.Equals(my_edge,  e_edge2)) { 
        return my_edge;
      }
    }
    if (dim == 3) {
      my_edge = getEdge(node, 2);
      if (Element.Edge.Equals(my_edge,  e_edge0)) {
        return my_edge;
      }
      if (Element.Edge.Equals(my_edge,  e_edge1)) {
        return my_edge;
      }
      if (edim == 3) {
        if (Element.Edge.Equals(my_edge,  e_edge2)) {
          return my_edge;
        }
      }
    }
    return null;
  }
}
