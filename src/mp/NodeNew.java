package mp;

import java.io.*;
import java.net.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

public class NodeNew {

		private Socket conx;
		
		private int port = 44555;
		private InputStream in = null;
		private OutputStream out = null;
		private InputStreamReader reader;
		
		private int node_id;
		
		public NodeNew (int total_nodes, int node_id) {
			
			this.node_id = node_id;
			
			try {
				conx = new Socket("localhost", port);
				out = conx.getOutputStream();
				in = conx.getInputStream();
				
				reader = new InputStreamReader(in);
			}
			catch(Exception e) {
				
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		public void sendMessage(int toNode, JSONObject message) throws IOException{
			
			JSONObject toSend = new JSONObject();
			
			toSend.put("send",  "send");
			toSend.put("receiver", toNode);
			toSend.put("sender",  this.node_id);
			toSend.put("message", message);
			
			out.write(toSend.toJSONString().getBytes());
		}
		
		public JSONObject receiveMessage(int fromNode) throws IOException {
			
			
			JSONObject request = new JSONObject();
			request.put("receive",  1);
			request.put("receiver", this.node_id);
			request.put("sender",  fromNode);
			
			out.write(request.toJSONString().getBytes());
			JSONParser parser = new JSONParser();
			JSONObject ret = null;
			
			/*
			byte[] results = new byte[4096];
			in.read(results);
			
			
			
			String json_string = new String(results);
			*/
			try {
				
				ret = (JSONObject)parser.parse(reader);
			}
			catch(Exception e){
				
				System.out.println("blah");
			}
			//Object ret = JSONValue.parse(json_string);
			//System.out.println(ret.toString());
			return ret;
		}
		
}
