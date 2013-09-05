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

File: Subgraph.java 
*/

package DelaunayRefinement.src.java;


import objects.graph.Edge;
import objects.graph.EdgeGraph;
import objects.graph.Node;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;


public class Subgraph {
  private final LinkedList<Node<Element>> nodes;
  // the nodes in the graph before updating
  private final LinkedList<Node<Element>> border; // the internal edges in the
                                                  // subgraph
  private final LinkedList<Edge<Element.Edge>> edges;
  private final LinkedList<Node<Element>> bad_nodes; 


  // the edges that connect the subgraph to the rest of the graph

  public Subgraph() {
    nodes = new LinkedList<Node<Element>>();
    border = new LinkedList<Node<Element>>();
    edges = new LinkedList<Edge<Element.Edge>>();
    bad_nodes = new LinkedList<Node<Element>>();
  }


  public boolean existsNode(Node<Element> n) {
    return nodes.contains(n);
  }


  public boolean existsBorder(Node<Element> b) {
    return border.contains(b);
  }


  public boolean existsEdge(Edge<Element.Edge> e) {
    return edges.contains(e);
  }


  public boolean addNode(Node<Element> n) {
	  Element value = n.getData();
	  if (value.isBad()) {
		  bad_nodes.add(n);
	  }
    return nodes.add(n);
  }


  public boolean addBorder(Node<Element> b) {
    return border.add(b);
  }


  public void addEdge(Edge<Element.Edge> e) {
    edges.add(e);
  }


  public LinkedList<Node<Element>> getNodes() {
    return nodes;
  }


  public LinkedList<Node<Element>> getBorder() {
    return border;
  }


  public LinkedList<Edge<Element.Edge>> getEdges() {
    return edges;
  }


  public void reset() {
    nodes.clear();
    border.clear();
    edges.clear();
  }


  public LinkedList<Node<Element>> newBad(EdgeGraph<Element, Element.Edge> mesh) {
    return bad_nodes;
  }
}
