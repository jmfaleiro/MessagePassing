package archivist.kirigami.java;

import java.io.BufferedReader;
import java.io.FileReader;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import mp.java.*;
import mp.java.ShMemObject.MergeException;

public class Appender {
	
	private final String m_input_file;
	private final int m_batch_size;
	private final int m_num_nodes;
	
	public Appender(String input_file, int batch_size, int num_nodes) { 
		m_input_file = input_file;
		m_batch_size = batch_size;
		m_num_nodes = num_nodes;
	}
	
	
	public void ReleaseAll() {
		for (int i = 1; i < m_num_nodes; ++i) {
			ShMem.Release(i);
		}
	}
	
	public void AcquireAll() {
		for (int i = 1; i < m_num_nodes; ++i) {
			try {
				ShMem.Acquire(i);
			}
			catch (MergeException e) {
				System.out.println("Didn't expect merge exception!");
				System.exit(-1);
			}
		}
	}
	
	public void Process() {
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode epoch_tweets = mapper.createArrayNode();
		
		ShMem.s_state.put("search_term",  "manchester united");
		ShMem.s_state.put("word-aggregate",  new ShMemObject());
		try {
			FileReader reader = new FileReader(m_input_file);
	    	@SuppressWarnings("resource")
			BufferedReader file = new BufferedReader(reader);
	    	
	    	String line = null;
	    	while (true) {
	    		
		    	for (int i = 0; i < m_batch_size; ++i) {
		    		line = file.readLine();
		    		if (line == null) {
		    			break;
		    		}
		    		epoch_tweets.add(mapper.readTree(line));
		    	}
		    	ShMem.s_state.put("tweets",  epoch_tweets);
		    	AcquireAll();
		    	ReleaseAll();
		    	
		    	if (line == null) {
		    		break;
		    	}
	    	}
	    	
	    	AcquireAll();
		}
		catch(Exception e) {
			e.printStackTrace(System.out);
			System.exit(-1);
		}
	}
	
	
	public static void main(String [] args)  {
		ShMem.Init(Integer.parseInt(args[0]));
		ShMem.Start();
		Appender blah = new Appender(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]));
		blah.Process();
	}
}
