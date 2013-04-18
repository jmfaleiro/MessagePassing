package test.simple;

import org.json.simple.*;

import mp.*;


// 
// Super simple process that just returns arguments. Use this to ensure
// that JSON serialization and client/server code works properly. 
// 
public class SimpleProcess implements IProcess{

	// Just return the argument. 
	@SuppressWarnings("unchecked")
	public void process() {
		
		try {
			ShMem.state.put("Jose",  "Faleiro");
		}
		catch(Exception e) {
			System.out.println("blah");
		}
	}

}
