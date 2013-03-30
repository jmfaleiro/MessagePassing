package test.simple;

import mp.*;

import org.json.simple.*;
import org.json.simple.parser.*;

public class Test {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws ParseException, ShMemFailure{
		
		// Create an empty array and the super simple process. 
		JSONArray arg = new JSONArray();
		arg.add(new JSONObject());
		SimpleProcess proc = new SimpleProcess();
		
		// Create a client and server. 
		ShMemServer server = new ShMemServer(proc, 0);
		server.start();
		ShMem.state.put("key",  "value");
		
		ShMem client = ShMem.fork(0);
		ShMem.state.put("jose",  "faleiro");
		client.join();
		System.out.println(ShMem.state.toJSONString());
	}
}
