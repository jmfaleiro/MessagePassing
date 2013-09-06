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

File: Mesh.java 
*/

package DelaunayRefinement.src.java;


import objects.graph.Edge;
import objects.graph.EdgeGraph;
import objects.graph.Node;

import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Stack;

import org.json.simple.JSONObject;


public class Mesh {
	
  public  final JSONObject graph = new JSONObject();	
  protected static final HashMap<Element.Edge, Node<Element>> edge_map = new HashMap<Element.Edge, Node<Element>>();
  protected final LinkedList<Integer> bad_nodes = new LinkedList<Integer>();

  public Mesh() {
	  graph.put("current_index",  0);
  }
  
  @SuppressWarnings("unchecked")
  public LinkedList<Integer> getBad() {
    return bad_nodes;
  }
  
  public void putNode(int index, Element value) {
	  graph.put(index,  value);
  }
  
  public int getNumNodes() {
	  return (Integer)graph.get("current_index");
  }

  public int getNextIndex() {
	  int current_index = (Integer)graph.get("current_index");
	  graph.put("current_index", current_index+1);
	  return current_index;
  }
  
  
  private static Scanner getScanner(String filename) throws Exception {
    try {
      return new Scanner(new GZIPInputStream(new FileInputStream(filename + ".gz")));
    } catch (FileNotFoundException _) {
      return new Scanner(new FileInputStream(filename));
    }
  }


  private JSONTuple[] readNodes(String filename) throws Exception {
    Scanner scanner = getScanner(filename + ".node");

    int ntups = scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();

    JSONTuple[] JSONTuples = new JSONTuple[ntups];
    for (int i = 0; i < ntups; i++) {
      int index = scanner.nextInt();
      double x = scanner.nextDouble();
      double y = scanner.nextDouble();
      scanner.nextDouble(); // z
      JSONTuples[index] = new JSONTuple(x, y, 0);
    }

    return JSONTuples;
  }


  private HashMap<Element.Edge, Integer> readElements(String filename, JSONTuple[] JSONTuples) throws Exception {
    Scanner scanner = getScanner(filename + ".ele");

    HashMap<Element.Edge, Integer> unresolved_edges = new HashMap<Element.Edge, Integer>();
    
    
    int nels = scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    Element[] elements = new Element[nels];
    for (int i = 0; i < nels; i++) {
      int index = scanner.nextInt();
      int n1 = scanner.nextInt();
      int n2 = scanner.nextInt();
      int n3 = scanner.nextInt();
      elements[index] = new Element(JSONTuples[n1], JSONTuples[n2], JSONTuples[n3], this);
      int graph_index = elements[index].getIndex();
      
      if (elements[index].isBad()) {
    	  bad_nodes.addFirst(graph_index);
      }
      
      tryResolveEdges(elements[index], unresolved_edges);
    }
    
    return unresolved_edges;
  }

  private void tryResolveEdges(Element elem, HashMap<Element.Edge, Integer> to_resolve) throws Exception {
	  
	  // 1 edge if it's a segment, otherwise 3. 
	  int n_edges = 2*elem.getDim() - 3;
	  
	  for (int i = 0; i < n_edges; ++i) {
		  Element.Edge edge = elem.getEdge(i);
		  
		  // We've seen this edge before, link the two elements. 
		  if (to_resolve.containsKey(edge)) {
			  int neighbor_index = to_resolve.get(edge);
			  to_resolve.remove(edge);
			  
			  Element neighbor = (Element)graph.get(neighbor_index);
			  
			  neighbor.resolveNeighbor(elem);
			  elem.resolveNeighbor(neighbor);
		  }
		  
		  // Haven't seen the edge before, add a reference to the element so 
		  // the neighbor can resolve it later. 
		  else {	
			  to_resolve.put(edge,  elem.getIndex());
		  }
	  }
  }

  private void readPoly(HashMap<Element.Edge, Integer> unresolved_edges, String filename, JSONTuple[] JSONTuples) throws Exception {
    Scanner scanner = getScanner(filename + ".poly");

    scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    int nsegs = scanner.nextInt();
    scanner.nextInt();
    Element[] segments = new Element[nsegs];
    
    for (int i = 0; i < nsegs; i++) {
      int index = scanner.nextInt();
      int n1 = scanner.nextInt();
      int n2 = scanner.nextInt();
      scanner.nextInt();
      segments[index] = new Element(JSONTuples[n1], JSONTuples[n2], this);
      
      // Mark it as bad.. 
      if (segments[index].isBad()) {
    	  bad_nodes.addFirst(segments[index].getIndex());
      }
      
      // Try to resolve the edges..
      tryResolveEdges(segments[index], unresolved_edges);
    }
  }
  
  public void removeNode(int node) {
	  Element to_remove = (Element)graph.get(node);
	  to_remove.kill();
  }

  public boolean containsNode(int node) {
	  Element to_check = (Element)graph.get(node);
	  return !to_check.isDead();
  }
  
  public Element getNodeData(int node) {
	  Element ret = (Element)graph.get(node);
	  if (ret == null) {
		  System.out.println("blah");
	  }
	  return ret;
  }

  // .poly contains the perimeter of the mesh; edges basically, which is why it
  // contains pairs of nodes
  public void read(String basename) throws Exception {
    JSONTuple[] JSONTuples = readNodes(basename);
    HashMap<Element.Edge, Integer> unresolved_edges = readElements(basename, JSONTuples);
    readPoly(unresolved_edges, basename, JSONTuples);
    
    if (unresolved_edges.size() != 0) {
    	throw new Exception("There still exist unresolved edges!");
    }
  }


  protected Node<Element> addElement(EdgeGraph<Element, Element.Edge> mesh, Element element) {
    Node<Element> node = mesh.createNode(element);
    mesh.addNode(node);
    for (int i = 0; i < element.numEdges(); i++) {
      Element.Edge edge = element.getEdge(i);
      if (!edge_map.containsKey(edge)) {
        edge_map.put(edge, node);
      } else {
        Edge<Element.Edge> new_edge = mesh.createEdge(node, edge_map.get(edge), edge);
        mesh.addEdge(new_edge);
        edge_map.remove(edge);
      }
    }
    return node;
  }


  public static boolean verify(EdgeGraph<Element, Element.Edge> mesh) {
    // ensure consistency of elements
    for (Node<Element> node : mesh) {
      Element element = mesh.getNodeData(node);
      if (element.getDim() == 2) {
        if (mesh.getOutNeighbors(node).size() != 1) {
          System.out.println("-> Segment " + element + " has " + mesh.getOutNeighbors(node).size() + " relation(s)");
          return false;
        }
      } else if (element.getDim() == 3) {
        if (mesh.getOutNeighbors(node).size() != 3) {
          System.out.println("-> Triangle " + element + " has " + mesh.getOutNeighbors(node).size() + " relation(s)");
          return false;
        }
      } else {
        System.out.println("-> Figures with " + element.getDim() + " edges");
        return false;
      }
    }
    // ensure reachability
    Node<Element> start = mesh.getRandom();
    Stack<Node<Element>> remaining = new Stack<Node<Element>>();
    HashSet<Node<Element>> found = new HashSet<Node<Element>>();
    remaining.push(start);
    while (!remaining.isEmpty()) {
      Node<Element> node = remaining.pop();
      if (!found.contains(node)) {
        found.add(node);
        for (Node<Element> neighbor : mesh.getOutNeighbors(node)) {
          remaining.push(neighbor);
        }
      }
    }
    if (found.size() != mesh.getNumNodes()) {
      System.out.println("Not all elements are reachable");
      return false;
    }
    return true;
  }
}
