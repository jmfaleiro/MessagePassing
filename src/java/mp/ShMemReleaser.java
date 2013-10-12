package mp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.net.Socket;

import java.util.*;
import java.util.concurrent.locks.*;

import org.apache.commons.lang3.tuple.*;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;


// Used to release state to other processes in the system.
// The methods in this class are NOT thread-safe.
public class ShMemReleaser {
	
	private final Lock queue_lock_ = new ReentrantLock();
	private final Condition queue_condition_ = queue_lock_.newCondition();
	
	// We need to keep the in-memory representation of the serialized list in 
	// this queue of stuff. 
	private final Queue<Pair<Integer, ObjectNode>> send_queue_ = new LinkedList<Pair<Integer, ObjectNode>>();
	
	private int me_;
	
	private ObjectMapper m_mapper;
	
	private Thread thread_;
	// private SendThread internal_sender_;
	
	// The constructor creates long lived connections to other nodes.
	public ShMemReleaser(int node_id) {
		me_ = node_id;
		m_mapper = new ObjectMapper();
		thread_ = new Thread(new SendThread(queue_lock_, queue_condition_, send_queue_));
		thread_.start();
	}
	
	// Add an element to the send queue. 
	public void Release(int to, ObjectNode state) {
		queue_lock_.lock();
		try {
			send_queue_.add(Pair.of(to, state));
		}
		catch (Exception e) {
			e.printStackTrace(System.out);
			System.exit(-1);
		}
		
		// Wake-up the internal sender thread in case it's sleeping. 
		queue_condition_.signal();
		queue_lock_.unlock();
	}
	
	private class SendThread implements Runnable {
		
		// State required to hold released state in a thread-safe manner. 
		private Lock m_queue_lock;
		private Condition m_queue_condition;
		private Queue<Pair<Integer, ObjectNode>> m_send_queue;
		
		// State required for persistent connections with acquire servers. 
		private Map<Integer, Socket> m_conx;					// Connection to talk to the acquring process.
		private Map<Integer, BufferedReader> m_in = null;	// Reader to parse input from the acquiring proc.
		private Map<Integer, PrintWriter> m_out = null;		// Writer to send data to the acquiring proc. 
		
		public SendThread(Lock queue_lock, 
						  Condition queue_condition, 
						  Queue<Pair<Integer, ObjectNode>> send_queue) {
			m_queue_lock = queue_lock;
			m_queue_condition = queue_condition;
			m_send_queue = send_queue;
		}
		
	
		// Open a persistent connection to the acquirer's server.
		private void InitConnection(int acquirer) {
			
			if (!m_conx.containsKey(acquirer)) {
				Pair<String, Integer> address_port = ShMem.addresses.get(acquirer);
				
				Socket conx = null;
				PrintWriter out = null;
				
				while (true) {
					try {
						conx = new Socket(address_port.getKey(), address_port.getRight());
						out = new PrintWriter(conx.getOutputStream(), true);
						break;
					}
					catch (Exception e) {
						System.out.println("Connection failed!");
					}
				}
				
				m_conx.put(acquirer,  conx);
				m_out.put(acquirer,  out);
			}
			
		}
		
		private void Send(Pair<Integer, ObjectNode> release_state) {
			ObjectNode to_send = m_mapper.createObjectNode();
			to_send.put("releaser", me_);
			to_send.put("argument",  release_state.getRight());
			Pair<String, Integer> address_port = ShMem.addresses.get(release_state.getLeft());
			
			try {
				Socket sock = new Socket(address_port.getKey(), address_port.getRight());
				OutputStream out_stream = sock.getOutputStream();
				out_stream.write(m_mapper.writeValueAsBytes(to_send));
				out_stream.flush();
				
				out_stream.close();
				sock.close();
			}
			catch (IOException e) {
				System.err.println("Release failed!\n");
				System.exit(-1);
			}
		}
		
		public void run() {
			
			// Run the send logic forever.
			while (true) {
				
				// Lock the send_queue_. There may be a concurrent
				// send request. 
				m_queue_lock.lock();
				try {
					
					// If the queue is empty, sleep. 
					while(m_send_queue.size() == 0) {
						m_queue_condition.await();
					}
				}
				
				// We should never have to handle any exception here. 
				catch (Exception e) {
					m_queue_lock.unlock();
					e.printStackTrace();
					System.exit(-1);
				}
				
				// Dequeue the head of the queue. 
				Pair<Integer, ObjectNode> to_send = m_send_queue.poll();
				m_queue_lock.unlock();
				
				// The queue should not have been empty. 
				assert(to_send != null);
				Send(to_send);
			}
		}
		
	}
}
