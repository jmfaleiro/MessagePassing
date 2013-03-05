package archivist;

import java.io.*;

public class Driver {

	public static void main(String[] args) throws IOException{
		
		Aggregator a = new VolumeAggregator(3, 1);
		a.Process();
	}
}
