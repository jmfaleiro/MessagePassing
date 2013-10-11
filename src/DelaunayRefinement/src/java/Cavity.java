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

File: Cavity.java 
*/

package DelaunayRefinement.src.java;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;


public class Cavity {
  protected JsonNode center;
  protected int centerNode;
  protected ObjectNode centerElement;
  protected int dim;
  protected final Queue<Integer> frontier;
  protected final Subgraph pre; // the cavity itself
  protected final Subgraph post; // what the new elements should look like
  private final Mesh graph;
  protected final HashMap<Element.Edge, Integer> connections;
  
  protected final HashMap<Element.Edge, Integer> unresolved_edges;


  // the edge-relations that connect the boundary to the cavity

  public Cavity(Mesh mesh) {
    center = null;
    frontier = new LinkedList<Integer>();
    pre = new Subgraph();
    post = new Subgraph();
    graph = mesh;
    connections = new HashMap<Element.Edge, Integer>();
    unresolved_edges = new HashMap<Element.Edge, Integer>();
  }


  public Subgraph getPre() {
    return pre;
  }


  public Subgraph getPost() {
    return post;
  }


  public void triggerAbort() {
  }


  public void triggerBorderConflict() {
  }


  public void initialize(int node) throws Exception {
    pre.reset();
    post.reset();
    connections.clear();
    frontier.clear();
    
    centerNode = node;
    centerElement = graph.getNodeData(centerNode);
    while (graph.containsNode(centerNode) && Element.isObtuse(centerElement)) {
      centerNode = getOpposite(centerNode);
      centerElement = graph.getNodeData(centerNode);
      if (centerNode == -1) {
        System.exit(-1);
      }
    }
    center = Element.center(centerElement);
    dim = Element.getDim(centerElement);
    pre.addNode(centerNode, graph);
    frontier.add(centerNode);
  }


  // find the edge that is opposite the obtuse angle of the element
  private int getOpposite(int node) throws Exception {
    ObjectNode element = graph.getNodeData(node);
    return Element.getObtuseNeighbor(element);
  }


  public boolean isMember(int node) {
    ObjectNode element = graph.getNodeData(node);
    return Element.inCircle(element, center);
  }

  public class EdgeWrapper {
	  private final Element.Edge m_edge;
	  private final int node0;
	  private final int node1;
	  
	  public Element.Edge getEdge() {
		  return m_edge;
	  }
	  
	  public int getNode(int index) throws Exception {
		  switch (index) {
		  case 0: 
			  return node0;
		  case 1: 
			  return node1;
		  default:
			  throw new Exception("Got an unexpected index!");
		  }
	  }
	  
	  public EdgeWrapper(Element.Edge edge, int first, int second) {
		  m_edge = edge;
		  node0 = first;
		  node1 = second;
	  }
	  
	  @Override
	  public boolean equals(Object o) {
		  if (o instanceof EdgeWrapper) {
			  return m_edge.equals(((EdgeWrapper) o).m_edge);
			  
		  }
		  return false;
	  }
	  
	  @Override
	  public int hashCode() {
		  return m_edge.hashCode();
	  }
  }
  

  public void build() throws Exception {
    while (frontier.size() != 0) {
      int curr = frontier.poll();
      ObjectNode elem = graph.getNodeData(curr);
      int num_neighbors = 2*Element.getDim(elem) - 3;
      for (int i = 0; i < num_neighbors; ++i) {
    	int next = Element.getNeighbor(elem, i);
	    ObjectNode nextElement = graph.getNodeData(next);
        Element.Edge edge = new Element.Edge(Element.getEdge(elem, i));
        // Cavity.EdgeWrapper edge_wrapper = new Cavity.EdgeWrapper(edge, curr, next);
        int next_dim = Element.getDim(nextElement);
        if ((!(dim == 2 && next_dim == 2 && next != centerNode)) && isMember(next)) {
          // isMember says next is part of the cavity, and we're not the second
          // segment encroaching on this cavity
          if ((next_dim == 2) && (dim != 2)) { // is segment, and we
                                                           // are encroaching
            initialize(next);
            build();
            return;
          } else {
            if (!pre.existsNode(next)) {
              pre.addNode(next, graph);
              frontier.add(next);
            }
          }
        } else { // not a member
        	if (!connections.containsKey(edge)) {
        		connections.put(edge,  next);
        	}
        }    	  
      }
      

    }
  }


  public void resolveEdges(ObjectNode elem) throws Exception {
	  int num = Element.numEdges(elem);
	  for (int i = 0; i < num; ++i) {
		  Element.Edge edge = new Element.Edge(Element.getEdge(elem, i));
		  if (unresolved_edges.containsKey(edge)) {
			  int neighbor_index = unresolved_edges.get(edge);
			  unresolved_edges.remove(edge);
			  
			  ObjectNode neighbor = graph.getNodeData(neighbor_index);
			  Element.resolveNeighbor(neighbor,  elem);
			  Element.resolveNeighbor(elem,  neighbor);
		  }
		  else {
			  unresolved_edges.put(edge,  Element.getIndex(elem));
		  }
	  }
  }
  
  @SuppressWarnings("unchecked")
  public void update() throws Exception {
	  
	  
	  
    if (Element.getDim(centerElement) == 2) { // we built around a segment
      ObjectNode ele1 = Element.CreateElement(center, Element.getPoint(centerElement, 0), graph);
      post.addNode(Element.getIndex(ele1), graph);
      ObjectNode ele2 = Element.CreateElement(center, Element.getPoint(centerElement, 1), graph);
      post.addNode(Element.getIndex(ele2), graph);
      unresolved_edges.put(new Element.Edge(Element.getEdge(ele1, 0)),  Element.getIndex(ele1));
      unresolved_edges.put(new Element.Edge(Element.getEdge(ele2, 0)),  Element.getIndex(ele2));
    }
    // for (Edge conn : new HashSet<Edge>(connections)) {
    for (Element.Edge edge : connections.keySet()) {
      
      ObjectNode new_element =  Element.CreateElement(center, edge.getPoint(0), edge.getPoint(1), graph);
      
      unresolved_edges.put(edge, connections.get(edge));
      resolveEdges(new_element);
      
      
      post.addNode(Element.getIndex(new_element), graph);
    }
  }
}
