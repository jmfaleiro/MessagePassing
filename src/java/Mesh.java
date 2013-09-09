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

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.*;


public class Mesh {
	
	
	
  public final static ObjectMapper mapper = new ObjectMapper();	
  private final static int num_indices = 100000;
  public final static String[] indices = new String[num_indices]; 
  public  final ObjectNode graph = mapper.createObjectNode();
  protected static final HashMap<Element.Edge, Node<Element>> edge_map = new HashMap<Element.Edge, Node<Element>>();
  protected final LinkedList<Integer> bad_nodes = new LinkedList<Integer>();

  static {
	  for (int i = 0; i < num_indices; ++i) {
		  indices[i] = Integer.toString(i);
	  }
  }
  
  public Mesh() {
	  graph.put("current_index",  0);
  }
  
  @SuppressWarnings("unchecked")
  public LinkedList<Integer> getBad() {
    return bad_nodes;
  }
  
  public void putNode(int index, JsonNode value) {
	  graph.put(indices[index],  value);
  }
  
  public int getNumNodes() {
	  return graph.get("current_index").getIntValue();
  }

  public int getNextIndex() {
	  int current_index = graph.get("current_index").getIntValue();
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


  private JsonNode[] readNodes(String filename) throws Exception {
    Scanner scanner = getScanner(filename + ".node");

    int ntups = scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();

    JsonNode[] JSONTuples = new JsonNode[ntups];
    for (int i = 0; i < ntups; i++) {
      int index = scanner.nextInt();
      double x = scanner.nextDouble();
      double y = scanner.nextDouble();
      scanner.nextDouble(); // z
      JSONTuples[index] = JSONTuple.CreateTuple(x,  y,  0);
    }

    return JSONTuples;
  }


  private HashMap<EdgeWrapper, Integer> readElements(String filename, JsonNode[] JSONTuples) throws Exception {
    Scanner scanner = getScanner(filename + ".ele");

    HashMap<EdgeWrapper, Integer> unresolved_edges = new HashMap<EdgeWrapper, Integer>();
    
    
    int nels = scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    ObjectNode[] elements = new ObjectNode[nels];
    for (int i = 0; i < nels; i++) {
      int index = scanner.nextInt();
      int n1 = scanner.nextInt();
      int n2 = scanner.nextInt();
      int n3 = scanner.nextInt();
      elements[index] = Element.CreateNewElement(JSONTuples[n1], JSONTuples[n2], JSONTuples[n3], this);
      int graph_index = Element.getIndex(elements[index]);
      
      if (Element.isBad(elements[index])) {
    	  bad_nodes.addFirst(graph_index);
      }
      
      tryResolveEdges(elements[index], unresolved_edges);
    }
    
    return unresolved_edges;
  }

  private void tryResolveEdges(ObjectNode elem, HashMap<EdgeWrapper, Integer> to_resolve) throws Exception {
	  
	  // 1 edge if it's a segment, otherwise 3. 
	  int n_edges =  2*Element.getDim(elem) - 3;
	  
	  for (int i = 0; i < n_edges; ++i) {
		  JsonNode edge = Element.getEdge(elem,  i);
		  EdgeWrapper wrapper = new EdgeWrapper(edge, -1, -1);
		  
		  // We've seen this edge before, link the two elements. 
		  if (to_resolve.containsKey(wrapper)) {
			  int neighbor_index = to_resolve.get(wrapper);
			  to_resolve.remove(wrapper);
			  
			  ObjectNode neighbor = (ObjectNode)(graph.get(indices[neighbor_index]));
			  
			  Element.resolveNeighbor(elem,  neighbor);
			  Element.resolveNeighbor(neighbor,  elem);
		  }
		  
		  // Haven't seen the edge before, add a reference to the element so 
		  // the neighbor can resolve it later. 
		  else {	
			  to_resolve.put(wrapper,  Element.getIndex(elem));
		  }
	  }
  }

  private void readPoly(HashMap<EdgeWrapper, Integer> unresolved_edges, String filename, JsonNode[] JSONTuples) throws Exception {
    Scanner scanner = getScanner(filename + ".poly");

    scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    scanner.nextInt();
    int nsegs = scanner.nextInt();
    scanner.nextInt();
    ObjectNode[] segments = new ObjectNode[nsegs];
    
    for (int i = 0; i < nsegs; i++) {
      int index = scanner.nextInt();
      int n1 = scanner.nextInt();
      int n2 = scanner.nextInt();
      scanner.nextInt();
      segments[index] = Element.CreateNewElement(JSONTuples[n1],  JSONTuples[n2],  this);  
      
      // Mark it as bad.. 
      if (Element.isBad(segments[index])) {
    	  bad_nodes.addFirst(Element.getIndex(segments[index]));
      }
      
      // Try to resolve the edges..
      tryResolveEdges(segments[index], unresolved_edges);
    }
  }
  
  public void removeNode(int node) {
	  ObjectNode to_remove = (ObjectNode)graph.get(indices[node]);
	  Element.kill(to_remove);
  }

  public boolean containsNode(int node) {
	  ObjectNode to_remove = (ObjectNode)graph.get(indices[node]);
	  return !Element.isDead(to_remove);
  }
  
  public ObjectNode getNodeData(int node) {
	  ObjectNode ret = (ObjectNode)graph.get(indices[node]);
	  return ret;
  }

  // .poly contains the perimeter of the mesh; edges basically, which is why it
  // contains pairs of nodes
  public void read(String basename) throws Exception {
	  
    JsonNode[] JSONTuples = readNodes(basename);
    HashMap<EdgeWrapper, Integer> unresolved_edges = readElements(basename, JSONTuples);
    readPoly(unresolved_edges, basename, JSONTuples);
    
    if (unresolved_edges.size() != 0) {
    	throw new Exception("There still exist unresolved edges!");
    }
  }

/*
  protected Node<Element> addElement(EdgeGraph<Element, Element.Edge> mesh, Element element) {
    Node<Element> node = mesh.createNode(element);
    mesh.addNode(node);
    for (int i = 0; i <  element.numEdges(); i++) {
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

*/
  /*

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
  */
  
  
}
