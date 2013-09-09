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
  protected final HashSet<EdgeWrapper> connections;


  // the edge-relations that connect the boundary to the cavity

  public Cavity(Mesh mesh) {
    center = null;
    frontier = new LinkedList<Integer>();
    pre = new Subgraph();
    post = new Subgraph();
    graph = mesh;
    connections = new HashSet<EdgeWrapper>();
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
    return Element.inCircle(element,  center);
  }
/*
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
  */

  public void build() throws Exception {
    while (frontier.size() != 0) {
      int curr = frontier.poll();
      ObjectNode elem = graph.getNodeData(curr);
      int num_neighbors = 2*Element.getDim(elem) - 3;
      for (int i = 0; i < num_neighbors; ++i) {
    	int next = Element.getNeighbor(elem,  i);
	    ObjectNode nextElement = graph.getNodeData(next);
        JsonNode edge = Element.getEdge(elem,  i);
        EdgeWrapper edge_wrapper = new EdgeWrapper(edge, curr, next);
        
        if ((!(dim == 2 && Element.getDim(nextElement) == 2  && next != centerNode)) && isMember(next)) {
          // isMember says next is part of the cavity, and we're not the second
          // segment encroaching on this cavity
          if ((Element.getDim(nextElement) == 2) && (dim != 2)) { // is segment, and we
                                                           // are encroaching
            initialize(next);
            build();
            return;
          } else {
            if (!pre.existsNode(next)) {
              pre.addNode(next, graph);
              pre.addEdge(edge_wrapper);
              frontier.add(next);
            }
          }
        } else { // not a member
          if (!connections.contains(edge_wrapper)) {
            connections.add(edge_wrapper);
            pre.addBorder(next);
          }
        }    	  
      }
    }
  }


  @SuppressWarnings("unchecked")
  public void update() throws Exception {
	  
	  LinkedList<ObjectNode> new_elems = new LinkedList<ObjectNode>();
    if (Element.getDim(centerElement) == 2) { // we built around a segment
      ObjectNode ele1 = Element.CreateNewElement(center, Element.getPoint(centerElement,  0),  graph); 
      post.addNode(Element.getIndex(ele1), graph);
      ObjectNode ele2 = Element.CreateNewElement(center, Element.getPoint(centerElement,  1), graph);
      post.addNode(Element.getIndex(ele2),graph);
    }
    // for (Edge conn : new HashSet<Edge>(connections)) {
    for (EdgeWrapper conn : connections) {
      JsonNode edge = conn.getEdge();
      ObjectNode new_element = Element.CreateNewElement(center, Element.Edge.GetPoint(edge, 0), Element.Edge.GetPoint(edge, 1), graph);
      new_elems.add(new_element);
      int ne_connection;
      if (pre.existsNode(conn.getNode(0))) {
        ne_connection = conn.getNode(1);
      } else {
        ne_connection = conn.getNode(0);
      }
      JsonNode new_edge = Element.getRelatedEdge(new_element,  graph.getNodeData(ne_connection)); 
      ObjectNode other = graph.getNodeData(ne_connection);
      Element.resolveNeighbor(new_element, other);
      Element.resolveNeighbor(other,  new_element);
      // post.addEdge(graph.createEdge(ne_node, ne_connection, new_edge));
      Collection<Integer> postnodes = (Collection<Integer>) post.getNodes().clone();
      for (int node : postnodes) {
        ObjectNode element = graph.getNodeData(node);
        int related_index = Element.isRelated(element,  new_element);
        if (related_index >= 0) {
          Element.setNeighbor(element, related_index, Element.getIndex(new_element));
          related_index = Element.isRelated(new_element,  element);
          Element.setNeighbor(new_element,  related_index,  Element.getIndex(element));
          // post.addEdge(graph.createEdge(ne_node, node, ele_edge));
        }
      }
      post.addNode(Element.getIndex(new_element), graph);
    }
    
    for (int node : post.getNodes()) {
    	ObjectNode elem = graph.getNodeData(node);
    	int num_neighbors = 2*Element.getDim(elem) - 3;
    	for (int i = 0; i < num_neighbors; ++i) {
    		if (Element.getNeighbor(elem,  i) == -1) {
    			throw new Exception("Unresolved neighbor!");
    		}
    	}
    }
  }
}
