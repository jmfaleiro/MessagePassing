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
  private final LinkedList<Integer> nodes;
  // the nodes in the graph before updating
  
  private final LinkedList<Integer> bad_nodes; 


  // the edges that connect the subgraph to the rest of the graph

  public Subgraph() {
    nodes = new LinkedList<Integer>();
    bad_nodes = new LinkedList<Integer>();
  }

  public boolean existsNode(int n) {
    return nodes.contains(n);
  }
  
  public boolean addNode(int n, Mesh mesh) {
	  Element node_data = mesh.getNodeData(n);
	  if (node_data.isBad()) {
		  bad_nodes.add(n);
	  }
	  return nodes.add(n);
  }

  public LinkedList<Integer> getNodes() {
    return nodes;
  }
  public void reset() {
    nodes.clear();
    bad_nodes.clear();
  }


  public LinkedList<Integer> newBad() {
    return bad_nodes;
  }
}
