package archivist;

import mp.*;

public class Aggregator {

	private ShMemAcquirer server;
	
	public Aggregator(int node_id, IProcess process) {
		
		server = new ShMemAcquirer(process, node_id);
		server.start();
	}
}
