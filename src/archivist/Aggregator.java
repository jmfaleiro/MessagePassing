package archivist;

import mp.*;

public class Aggregator {

	private ShMemServer server;
	
	public Aggregator(int node_id, IProcess process) {
		
		server = new ShMemServer(process, node_id);
		server.start();
	}
}
