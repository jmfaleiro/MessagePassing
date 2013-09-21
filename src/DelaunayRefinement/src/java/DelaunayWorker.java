package DelaunayRefinement.src.java;

import org.codehaus.jackson.node.ArrayNode;

import mp.ShMem;
import mp.ShMemObject;

public class DelaunayWorker {

	private final int m_scheduler_id;
	private final String m_id;
	private final Mesh m_mesh;
	private final Cavity m_cavity;
	
	public DelaunayWorker(int scheduler_id, String my_id, Mesh mesh) {
		m_scheduler_id = scheduler_id;
		m_id = my_id;
		m_mesh = mesh;
		m_cavity = new Cavity(m_mesh);
	}
	
	// Launch each JVM corresponding to a worker process here. 
	// Read the original mesh from disk at time 0 and set ShMem.state to 
	// the graph in "mesh". 
	public static void main(String[] args) {
		
		// Initialize ShMem with our node id.
		ShMem.Init(Integer.parseInt(args[1]));
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
		
		DelaunayWorker worker = new DelaunayWorker(0, args[1], mesh);
		worker.process();
	}
	
	public void process() {
		
		// Release to the scheduler indicating that we're ready to go. 
		ShMem.Release(m_scheduler_id);
		
		while (true) {
			
			// The acquire shouldn't throw an exception because the 
			// scheduler should have taken care of any inconsistencies. 
			ShMem.Acquire(m_scheduler_id);
			ShMemObject my_container = (ShMemObject)ShMem.s_state.get(m_id);
			
			// Done with mesh refinement!
			if (my_container.get("die") != null) {
				System.exit(0);
			}
			try {
				int to_process = my_container.get("next").getIntValue();
				
				// These guys just do some analysis on the graph. 
				m_cavity.initialize(to_process);
				m_cavity.build();
				
				// This guy actually makes changes to the graph. 
				m_cavity.update();
				
				// remove the old data
		        for (int node : m_cavity.getPre().getNodes()) {
		          m_mesh.removeNode(node);
		        }
		        
		        // Communicate the new bad elements in the bad_elems
		        // ArrayNode. 
		        ArrayNode bad_elems = ShMem.mapper.createArrayNode();
		        for (int bad : m_cavity.getPost().newBad()) {
		        	bad_elems.add(bad);
		        }
		        my_container.put("new_bad",  bad_elems);
		        
		        // Communicate the new nodes we've just created (in case
		        // the scheduler needs to roll back some state). 
		        ArrayNode new_elems = ShMem.mapper.createArrayNode();
		        for (int new_elem : m_cavity.getPost().getNodes()) {
		        	new_elems.add(new_elem);
		        }
		        my_container.put("new_nodes",  new_elems);
			}
			catch (Exception e) {
				
			}
			
			
			ShMem.Release(m_scheduler_id);
		}
	}
}
