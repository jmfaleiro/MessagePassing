package test.simple;

import mp.*;

import org.json.simple.*;
import org.json.simple.parser.*;

public class Test {

	public static void main(String[] args) throws ParseException, ShMemFailure{
		
		// Create an empty array and the super simple process. 
		JSONArray arg = new JSONArray();
		SimpleProcess proc = new SimpleProcess();
		
		// Create a client and server. 
		ShMemServer server = new ShMemServer(proc, 0);
		server.start();
		
		
		ShMemClient client = new ShMemClient(arg, 0);
		
		client.fork();
		JSONArray blah = client.merge(arg);
		System.out.println("Pass!");
		
		
	}
}
