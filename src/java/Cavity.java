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


import objects.graph.Edge;
import objects.graph.EdgeGraph;
import objects.graph.Node;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import org.codehaus.jackson.JsonNode;


public class Cavity {
  protected JsonNode center;
  protected int centerNode;
  protected Element centerElement;
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
    while (graph.containsNode(centerNode) && centerElement.isObtuse()) {
      centerNode = getOpposite(centerNode);
      centerElement = graph.getNodeData(centerNode);
      if (centerNode == -1) {
        System.exit(-1);
      }
    }
    center = centerElement.center();
    dim = centerElement.getDim();
    pre.addNode(centerNode, graph);
    frontier.add(centerNode);
  }


  // find the edge that is opposite the obtuse angle of the element
  private int getOpposite(int node) throws Exception {
    Element element = graph.getNodeData(node);
    return element.getObtuseNeighbor();
  }


  public boolean isMember(int node) {
    Element element = graph.getNodeData(node);
    return element.inCircle(center);
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
      Element elem = graph.getNodeData(curr);
      int num_neighbors = 2*elem.getDim() - 3;
      for (int i = 0; i < num_neighbors; ++i) {
    	int next = elem.getNeighbor(i);
	    Element nextElement = graph.getNodeData(next);
        Element.Edge edge = elem.getEdge(i);
        // Cavity.EdgeWrapper edge_wrapper = new Cavity.EdgeWrapper(edge, curr, next);
        
        if ((!(dim == 2 && nextElement.getDim() == 2 && next != centerNode)) && isMember(next)) {
          // isMember says next is part of the cavity, and we're not the second
          // segment encroaching on this cavity
          if ((nextElement.getDim() == 2) && (dim != 2)) { // is segment, and we
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


  public void resolveEdges(Element elem) throws Exception {
	  int num = elem.numEdges();
	  for (int i = 0; i < num; ++i) {
		  Element.Edge edge = elem.getEdge(i);
		  if (unresolved_edges.containsKey(edge)) {
			  int neighbor_index = unresolved_edges.get(edge);
			  unresolved_edges.remove(edge);
			  
			  Element neighbor = graph.getNodeData(neighbor_index);
			  neighbor.resolveNeighbor(elem);
			  elem.resolveNeighbor(neighbor);
		  }
		  else {
			  unresolved_edges.put(edge,  elem.getIndex());
		  }
	  }
  }
  
  @SuppressWarnings("unchecked")
  public void update() throws Exception {
	  
	  
	  LinkedList<Element> new_elems = new LinkedList<Element>();
    if (centerElement.getDim() == 2) { // we built around a segment
      Element ele1 = new Element(center, centerElement.getPoint(0), graph);
      post.addNode(ele1.getIndex(), graph);
      Element ele2 = new Element(center, centerElement.getPoint(1), graph);
      post.addNode(ele2.getIndex(), graph);
      unresolved_edges.put(ele1.getEdge(0),  ele1.getIndex());
      unresolved_edges.put(ele2.getEdge(0),  ele2.getIndex());
    }
    // for (Edge conn : new HashSet<Edge>(connections)) {
    for (Element.Edge edge : connections.keySet()) {
      
      Element new_element = new Element(center, edge.getPoint(0), edge.getPoint(1), graph);
      new_elems.add(new_element);
      unresolved_edges.put(edge, connections.get(edge));
      resolveEdges(new_element);
      
      /*
      unresolved_edges.put(edge, connections.get(edge));
      
      graph.tryResolveEdges(new_element,  unresolved_edges);
      */
      
      /*
      int ne_connection;
      if (pre.existsNode(conn.getNode(0))) {
        ne_connection = conn.getNode(1);
      } else {
        ne_connection = conn.getNode(0);
      }
      
      
      
      
      int ne_connection = connections.get(edge);
      
      Element.Edge new_edge = new_element.getRelatedEdge(graph.getNodeData(ne_connection));
      Element other = graph.getNodeData(ne_connection);
      new_element.resolveNeighbor(other);
      other.resolveNeighbor(new_element);
      
      
      
      // post.addEdge(graph.createEdge(ne_node, ne_connection, new_edge));
      Collection<Integer> postnodes = (Collection<Integer>) post.getNodes().clone();
      for (int node : postnodes) {
        Element element = graph.getNodeData(node);
        int related_index = element.isRelated(new_element);
        if (related_index >= 0) {
          element.setNeighbor(related_index,  new_element.getIndex());
          related_index = new_element.isRelated(element);
          new_element.setNeighbor(related_index,  element.getIndex());
          // post.addEdge(graph.createEdge(ne_node, node, ele_edge));
        }
      }
      */
      post.addNode(new_element.getIndex(), graph);
    }
    
    for (int node : post.getNodes()) {
    	Element elem = graph.getNodeData(node);
    	int num_neighbors = 2*elem.getDim() - 3;
    	for (int i = 0; i < num_neighbors; ++i) {
    		if (elem.getNeighbor(i) == -1) {
    			throw new Exception("Unresolved neighbor!");
    		}
    	}
    }
  }
}
