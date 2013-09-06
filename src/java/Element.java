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

import org.json.simple.JSONObject;

public class Element {
  private final boolean bObtuse;
  private final boolean bBad;

  private final int obtuse;
  private final JSONTuple[] coords;
  private final Element.Edge[] edges;
  private final int dim;

  private final JSONTuple center;
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

  public Element(JSONTuple a, JSONTuple b, JSONTuple c, Mesh nodes) {
	 
	 index = nodes.getNextIndex();
	 dead = false;
	  
    dim = 3;
    coords = new JSONTuple[3];
    coords[0] = a;
    coords[1] = b;
    coords[2] = c;
    if (b.lessThan(a) || c.lessThan(a)) {
      if (b.lessThan(c)) {
        coords[0] = b;
        coords[1] = c;
        coords[2] = a;
      } else {
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
    radius_squared = coords[0].distance_squared(center);
    
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
  
  private JSONTuple getCenter(JSONTuple coord0, 
		  				 	  JSONTuple coord1, 
		  				 	  JSONTuple coord2) {
	  
	  double x1 = (Double)coord1.get(JSONTuple.x_index) - (Double)coord0.get(JSONTuple.x_index);
		double y1 = (Double)coord1.get(JSONTuple.y_index) - (Double)coord0.get(JSONTuple.y_index);
		double z1 = (Double)coord1.get(JSONTuple.z_index) - (Double)coord0.get(JSONTuple.z_index);
		
		double x2 = (Double)coord2.get(JSONTuple.x_index) - (Double)coord0.get(JSONTuple.x_index);
		double y2 = (Double)coord2.get(JSONTuple.y_index) - (Double)coord0.get(JSONTuple.y_index);
		double z2 = (Double)coord2.get(JSONTuple.z_index) - (Double)coord0.get(JSONTuple.z_index);
		
		double len1 = coord0.distance(coord1);
		double len2 = coord0.distance(coord2);
		
		double cosine = (x1*x2 + y1*y2 + z1*z2) / (len1 * len2);
		double sine_sq = 1.0 - cosine * cosine;
	    double plen = len2/len1;
	    double s = plen * cosine;
	    double t = plen * sine_sq;
	    double wp = (plen - cosine) / (2 * t);
	    double wb = 0.5 - (wp * s);
	    
	    double x_center = (Double)coord0.get(JSONTuple.x_index) * (1 - wb - wp);
	    double y_center = (Double)coord0.get(JSONTuple.y_index) * (1 - wb - wp);
	    double z_center = (Double)coord0.get(JSONTuple.z_index) * (1 - wb - wp);
	    
	    x_center += (Double)coord1.get(JSONTuple.x_index) * wb;
	    y_center += (Double)coord1.get(JSONTuple.y_index) * wb;
	    z_center += (Double)coord1.get(JSONTuple.z_index) * wb;
	    
	    x_center += (Double)coord2.get(JSONTuple.x_index) * wp;
	    y_center += (Double)coord2.get(JSONTuple.y_index) * wp;
	    z_center += (Double)coord2.get(JSONTuple.z_index) * wp;
	    
	    return new JSONTuple(x_center, y_center, z_center);
  }
  
  private JSONTuple getCenter(JSONTuple coord0, JSONTuple coord1) {
	  double x = (coord0.get(JSONTuple.x_index) + coord1.get(JSONTuple.x_index)) / 2.0;
	  double y = (coord0.get(JSONTuple.y_index) + coord1.get(JSONTuple.y_index)) / 2.0;
	  double z = (coord0.get(JSONTuple.z_index) + coord1.get(JSONTuple.z_index)) / 2.0; 
	  
	  return new JSONTuple(x, y, z);
  }

  public Element(JSONTuple a, JSONTuple b, Mesh nodes) {
	  index = nodes.getNextIndex();
	  dead = false;
	  
    dim = 2;
    coords = new JSONTuple[2];
    coords[0] = a;
    coords[1] = b;
    if (b.lessThan(a)) {
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
    radius_squared = center.distance_squared(a);
    
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
    private final JSONTuple p1;
    private final JSONTuple p2;
    private final int hashvalue;


    public Edge() {
      p1 = null;
      p2 = null;
      hashvalue = 1;
    }


    public Edge(JSONTuple a, JSONTuple b) {
      if (a.lessThan(b)) {
        p1 = a;
        p2 = b;
      } else {
        p1 = b;
        p2 = a;
      }
      int tmphashval = 17;
      tmphashval = 37 * tmphashval + p1.hashCode();
      tmphashval = 37 * tmphashval + p2.hashCode();
      hashvalue = tmphashval;
    }


    public Edge(Edge rhs) {
      p1 = rhs.p1;
      p2 = rhs.p2;
      hashvalue = rhs.hashvalue;
    }


    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Edge)) {
        return false;
      }
      Edge edge = (Edge) obj;
      return p1.equals(edge.p1) && p2.equals(edge.p2);
    }


    @Override
    public int hashCode() {
      return hashvalue;
    }


    public boolean notEqual(Edge rhs) {
      return !equals(rhs);
    }


    public boolean lessThan(Edge rhs) {
      return p1.lessThan(rhs.p1) || (p1.equals(rhs.p1) && p2.lessThan(rhs.p2));
    }


    public boolean greaterThan(Edge rhs) {
      return p1.greaterThan(rhs.p1) || (p1.equals(rhs.p1) && p2.greaterThan(rhs.p2));
    }


    public JSONTuple getPoint(int i) {
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
      if (coords[i].lessThan(e.coords[i])) {
        return true;
      } else if (coords[i].greaterThan(e.coords[i])) {
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


  public JSONTuple center() {
    return center;
  }


  public boolean inCircle(JSONTuple p) {
    double ds = center.distance_squared(p);
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
    JSONTuple a = coords[i];
    JSONTuple b = coords[j];
    JSONTuple c = coords[k];
    return JSONTuple.angle(b, a, c);
  }

  public int getIndex() {
	  return this.index;
  }
  
  public Element.Edge getEdge(int i) {
    return edges[i];
  }


  public JSONTuple getPoint(int i) {
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
