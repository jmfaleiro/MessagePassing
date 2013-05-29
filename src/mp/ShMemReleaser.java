package mp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;

import java.util.*;
import java.util.concurrent.locks.*;

import org.apache.commons.lang3.tuple.*;

import org.json.simple.*;

// Used to release state to other processes in the system.
// The methods in this class are NOT thread-safe.
public class ShMemReleaser {
	
	private final Lock queue_lock_ = new ReentrantLock();
	private final Condition queue_condition_ = queue_lock_.newCondition();
	private final Queue<Pair<Integer, String>> send_queue_ = new LinkedList<Pair<Integer, String>>();
	
	private int me_;
	
	private Thread thread_;
	// private SendThread internal_sender_;
	
	// The constructor creates long lived connections to other nodes.
	public ShMemReleaser(int node_id) {
		me_ = node_id;
		thread_ = new Thread(new SendThread(queue_lock_, queue_condition_, send_queue_));
		thread_.start();
	}
	
	// Add an element to the send queue. 
	public void Release(int to, JSONObject state) {
		queue_lock_.lock();
		send_queue_.add(Pair.of(to,  state.toJSONString()));
		
		// Wake-up the internal sender thread in case it's sleeping. 
		queue_condition_.signal();
		queue_lock_.unlock();
	}
	
	private class SendThread implements Runnable {
		
		// State required to hold released state in a thread-safe manner. 
		private Lock queue_lock_;
		private Condition queue_condition_;
		private Queue<Pair<Integer, String>> send_queue_;

		//private Map<Integer, Socket> conx_;					// Connection to talk to the acquring process.
		//private Map<Integer, BufferedReader> in_ = null;	// Reader to parse input from the acquiring proc.
		//private Map<Integer, PrintWriter> out_ = null;		// Writer to send data to the acquiring proc. 
		
		public SendThread(Lock queue_lock, 
						  Condition queue_condition, 
						  Queue<Pair<Integer, String>> send_queue) {
			queue_lock_ = queue_lock;
			queue_condition_ = queue_condition;
			send_queue_ = send_queue;
			
			InitConnections();
		}
		
		private void InitConnections() {
			
			/*
			conx_ = new HashMap<Integer, Socket>();
			in_ = new HashMap<Integer, BufferedReader>();
			out_ = new HashMap<Integer, PrintWriter>();
			
			// Iterate through all the addresses of the nodes in the system and 
			// open connections to all of them. We're going to use these connections
			// to send state releases. 
			for (Integer node : ShMem.addresses.keySet()) {
				Pair<String, Integer> address_port = ShMem.addresses.get(node); 
				
				// Make sure we open connections properly. 
				while (true) {
					try {
						
						// Open a connection + associated stream reader and writer. 
						Socket cur_conx = new Socket(address_port.getKey(), address_port.getRight());
						BufferedReader cur_in = new BufferedReader(new InputStreamReader(cur_conx.getInputStream()));
						PrintWriter cur_out = new PrintWriter(cur_conx.getOutputStream(), true);
						
						// Insert connection state into our per process map. This keeps track
						// of the long lived state.
						conx_.put(node, cur_conx);
						in_.put(node, cur_in);
						out_.put(node,  cur_out);
					}
					catch (Exception e) {
						continue;
					}
					break;
				}
			}
			*/
		}
		
		private void Send(Pair<Integer, String> release_state) {
			
			Pair<String, Integer> address_port = ShMem.addresses.get(release_state.getLeft());
			Socket conx;
			PrintWriter out;
			
			while (true) {
				
				try {
					conx = new Socket(address_port.getKey(), address_port.getRight());
					out = new PrintWriter(conx.getOutputStream(), true);
					break;
				}
				catch(Exception e) {
					System.out.println("Connection failed!");
				}
			}
			
			
			JSONObject to_send = new JSONObject();
			to_send.put("releaser", me_);
			to_send.put("argument",  release_state.getRight());
			out.println(to_send.toJSONString());
			
			out.close();
			try {
				conx.close();
			}
			catch(IOException e) {
				System.out.println("Couldn't close connection");
			}
		}
		
		public void run() {
			
			// Run the send logic forever.
			while (true) {
				
				// Lock the send_queue_. There may be a concurrent
				// send request. 
				queue_lock_.lock();
				try {
					
					// If the queue is empty, sleep. 
					while(send_queue_.size() == 0) {
						queue_condition_.await();
					}
				}
				
				// We should never have to handle any exception here. 
				catch (Exception e) {
					queue_lock_.unlock();
					e.printStackTrace();
					System.exit(-1);
				}
				
				// Dequeue the head of the queue. 
				Pair<Integer, String> to_send = send_queue_.poll();
				queue_lock_.unlock();
				
				// The queue should not have been empty. 
				assert(to_send != null);
				Send(to_send);
			}
		}
		
	}
}
