package DelaunayRefinement.src.java;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import common.util.Time;

import mp.ShMem;
import mp.ShMemObject;

public class DelaunayScheduler {

	
	private final HashMap<Integer, String> m_workers;
	private final Mesh m_mesh;
	private final LinkedList<Integer> m_worklist;
	private final Random m_genrandom;
	
	
	public static void main(String[] args) {
		
		// Initialize ShMem with our node id.
		ShMem.Init(Integer.parseInt(args[1]));
		int num_workers = Integer.parseInt(args[2]);
		Mesh mesh = new Mesh();
		try {
		mesh.read(args[0]);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(-1);
		}
		ShMem.s_state.put("graph",  mesh.graph);
		
		// All initialization complete, we're ready to go!
		ShMem.Start();
		
		DelaunayScheduler scheduler = new DelaunayScheduler(num_workers, mesh);
		scheduler.process();
	}
	
	public DelaunayScheduler(int num_workers, Mesh mesh) {
		
		m_workers = new HashMap<Integer, String>();
		
		// Initialize and populate the arrays corresponding to worker ids
		for (int i = 0; i < num_workers; ++i) {
			m_workers.put(i+1,  Integer.toString(i+1));
		}
		
		// Initialize the mesh and worklist of bad nodes. 
		m_mesh = mesh;
		m_worklist = mesh.getBad();
		m_genrandom = new Random(1234);
		Collections.shuffle(m_worklist); // permute the list of bad nodes. 
	}
	
	private void getBadNodes(int worker_id) {
		ShMemObject obj = 
				(ShMemObject)ShMem.s_state.get(m_workers.get(worker_id));
		
		Iterator<JsonNode> bad_nodes = 
				((ArrayNode)(obj.get("new_bad"))).getElements();
		
		
		
		while (bad_nodes.hasNext()) {
			int cur_bad = bad_nodes.next().getIntValue();
			m_worklist.addLast(cur_bad);
		}
		
		Collections.shuffle(m_worklist, m_genrandom);
	}
	
	private void dispatchWork(int worker_index) {
		ShMemObject arg = (ShMemObject)ShMem.s_state.get(m_workers.get(worker_index));
		int next_bad = m_worklist.removeFirst();
		arg.put("next",  next_bad);
		ShMem.Release(worker_index);
	}
	
	public void process() {
		
		System.gc();
	    System.gc();
	    System.gc();
	    System.gc();
	    System.gc();
		
	    for (int worker_id : m_workers.keySet()) {
			ShMem.Acquire(worker_id);
		}
		int num_workers = m_workers.size();
		
		int[] waiting_on = new int[num_workers];
		boolean[] free_workers = new boolean[num_workers+1];
		
		int first_free = 0, last_waiter = 0;
		
		for (int i = 0; i < num_workers; ++i) {
			waiting_on[i] = -1;
			free_workers[i] = true;
			
			ShMemObject arg = new ShMemObject();
			ShMem.s_state.put(m_workers.get(i+1), arg);
		}
		free_workers[num_workers] = true;
		
		// Start our experiment clock. 
		
		long id = Time.getNewTimeId();
		// Check if the worklist is empty and if all the workers are free. 
		// If yes, we're done. 
		
		int iter = 0;
		while (!(m_worklist.isEmpty() && last_waiter == first_free)) {
			
			// We'll only get out of this loop when there are bad nodes and
			// when we have at least one free worker. 
			while (m_worklist.isEmpty() || first_free == last_waiter + num_workers) {
				ShMem.Acquire(waiting_on[last_waiter % num_workers]);
				getBadNodes(waiting_on[last_waiter % num_workers]);
				free_workers[waiting_on[last_waiter % num_workers]] = true;
				waiting_on[last_waiter % num_workers] = -1;
				last_waiter += 1;
			}
			
			// Find the first free worker. Don't use hashset of integers
			// to keep track of free workers because of GC. 
			int next;
			for (next = 1; next <= num_workers; ++next) {
				if (free_workers[next]) {
					free_workers[next] = false;
					break;
				}
			}
			
			System.out.println(iter);
			iter += 1;
			
			// Release to the next free worker. 
			waiting_on[first_free % num_workers] = next;
			first_free = first_free + 1;
			dispatchWork(next);
		}
		
		long time = Time.elapsedTime(id);
	    System.err.println("runtime: " + time + " ms");
	}
	
}
