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
import org.json.simple.JSONObject;

public class Element {
  private final boolean bObtuse;
  private final boolean bBad;

  private final int obtuse;
  private final JsonNode[] coords;
  private final Element.Edge[] edges;
  private final int dim;

  private final JsonNode center;
  private final double radius_squared;
  
  private final int index;
  private boolean dead;
  
  private final int[] neighbors;

  private static final double MINANGLE = 30.0;

  public boolean isDead() {
	  return dead;
  }
  
  public void kill() {
	  dead = true;
  }

  public Element(JsonNode a, JsonNode b, JsonNode c, Mesh nodes) {
	 
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
  
  public int getObtuseNeighbor() throws Exception {
	  if (bObtuse) {
		  return neighbors[obtuse];
	  }
	  else {
		  throw new Exception("Element is not obtuse!");
	  }
  }
  
  public int getNeighbor(int index) {
	  return neighbors[index];
  }
  
  public int[] getNeighbors() {
	  return neighbors;
  }
  
  private int grabIndex(JSONObject nodes) {
	  int current_index = (Integer)nodes.get("current");
	  nodes.put("current",  current_index+1);
	  return current_index;
  }
  
  private JsonNode getCenter(JsonNode coord0, 
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
  
  private JsonNode getCenter(JsonNode coord0, JsonNode coord1) {
	  double x = (coord0.get(JSONTuple.x_index).getDoubleValue() + coord1.get(JSONTuple.x_index).getDoubleValue()) / 2.0;
	  double y = (coord0.get(JSONTuple.y_index).getDoubleValue() + coord1.get(JSONTuple.y_index).getDoubleValue()) / 2.0;
	  double z = (coord0.get(JSONTuple.z_index).getDoubleValue() + coord1.get(JSONTuple.z_index).getDoubleValue()) / 2.0; 
	  
	  return JSONTuple.CreateTuple(x, y, z);
  }

  public Element(JsonNode a, JsonNode b, Mesh nodes) {
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
  
  public void resolveNeighbor(Element neighbor) throws Exception {
	  int related_index = isRelated(neighbor);
	  if (related_index == -1) {
		  throw new Exception("Called with unrelated elements!");
	  }
	  
	  neighbors[related_index] = neighbor.index;
  }

  public void setNeighbor(int index, int value) {
	  neighbors[index] = value;
  }
  
  public static class Edge {
    private final JsonNode p1;
    private final JsonNode p2;
    private final int hashvalue;


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

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Edge)) {
        return false;
      }
      Edge edge = (Edge) obj;
      return JSONTuple.Equals(p1, edge.p1) && JSONTuple.Equals(p2, edge.p2); 
    }


    @Override
    public int hashCode() {
      return hashvalue;
    }


    public boolean notEqual(Edge rhs) {
      return !equals(rhs);
    }


    public boolean lessThan(Edge rhs) {
    	return JSONTuple.LessThan(p1,  rhs.p1) || (JSONTuple.Equals(p1,  rhs.p1) && JSONTuple.LessThan(p2,  rhs.p2));
    	
    }


    public boolean greaterThan(Edge rhs) {
    	return JSONTuple.GreaterThan(p1,  rhs.p1) || (JSONTuple.Equals(p1,  rhs.p1) && JSONTuple.GreaterThan(p2,  rhs.p2));

    }


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
  }

  

  public boolean lessThan(Element e) {
    if (dim < e.getDim()) {
      return false;
    }
    if (dim > e.getDim()) {
      return true;
    }
    for (int i = 0; i < dim; i++) {
    	if (JSONTuple.LessThan(coords[i],  e.coords[i])) {
        return true;
      } else if (JSONTuple.GreaterThan(coords[i],  e.coords[i])) {
        return false;
      }
    }
    return false;
  }
  
  public int isRelated(Element e) {
    int edim = e.getDim();
    Element.Edge my_edge, e_edge0, e_edge1, e_edge2 = null;
    my_edge = edges[0];
    e_edge0 = e.edges[0];
    if (my_edge.equals(e_edge0)) {
      return 0;
    }
    e_edge1 = e.edges[1];
    if (my_edge.equals(e_edge1)) {
      return 0;
    }
    if (edim == 3) {
      e_edge2 = e.edges[2];
      if (my_edge.equals(e_edge2)) {
        return 0;
      }
    }
    my_edge = edges[1];
    if (my_edge.equals(e_edge0)) {
      return 1;
    }
    if (my_edge.equals(e_edge1)) {
      return 1;
    }
    if (edim == 3) {
      if (my_edge.equals(e_edge2)) {
        return 1;
      }
    }
    if (dim == 3) {
      my_edge = edges[2];
      if (my_edge.equals(e_edge0)) {
        return 2;
      }
      if (my_edge.equals(e_edge1)) {
        return 2;
      }
      if (edim == 3) {
        if (my_edge.equals(e_edge2)) {
          return 2;
        }
      }
    }
    return -1;
  }


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


  public JsonNode center() {
    return center;
  }


  public boolean inCircle(JsonNode p) {
	  double ds = JSONTuple.DistanceSquared(center,  p);
    return ds <= radius_squared;
  }


  public double getAngle(int i) {
    int j = i + 1;
    if (j == dim) {
      j = 0;
    }
    int k = j + 1;
    if (k == dim) {
      k = 0;
    }
    JsonNode a = coords[i];
    JsonNode b = coords[j];
    JsonNode c = coords[k];
    return JSONTuple.angle(b, a, c);
  }

  public int getIndex() {
	  return this.index;
  }
  
  public Element.Edge getEdge(int i) {
    return edges[i];
  }


  public JsonNode getPoint(int i) {
    return coords[i];
  }

  
  // should the node be processed?
  public boolean isBad() {
    return bBad;
  }


  public int getDim() {
    return dim;
  }


  public int numEdges() {
    return dim + dim - 3;
  }


  public boolean isObtuse() {
    return bObtuse;
  }


  public Edge getRelatedEdge(Element e) {
    // Scans all the edges of the two elements and if it finds one that is
    // equal, then sets this as the Edge of the EdgeRelation
    int edim = e.getDim();
    Element.Edge my_edge, e_edge0, e_edge1, e_edge2 = null;
    my_edge = edges[0];
    e_edge0 = e.edges[0];
    if (my_edge.equals(e_edge0)) {
      return my_edge;
    }
    e_edge1 = e.edges[1];
    if (my_edge.equals(e_edge1)) {
      return my_edge;
    }
    if (edim == 3) {
      e_edge2 = e.edges[2];
      if (my_edge.equals(e_edge2)) {
        return my_edge;
      }
    }
    my_edge = edges[1];
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
      my_edge = edges[2];
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
