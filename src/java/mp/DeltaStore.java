package mp;



import java.util.*;
import java.util.concurrent.locks.*;

import org.codehaus.jackson.node.ObjectNode;

//
// This class is used to service acquire requests. Not visible to
// the actual application. Used by ShMem. It's kept around as a 
// singleton. 
// 
public class DeltaStore {
	
	// XXX: Assume that we always get releases from other processes in 
	// the right order. The use of sockets guarantees this. 
	private Map<Integer, Queue<ObjectNode>> acquired_state_;
	
	// Use the appropriate lock on an acquire call. Sleep using the corresponding
	// condition variable.
	private final Map<Integer, Lock> locks_ = new HashMap<Integer, Lock>();
	private final Map<Integer, Condition> conditions_ = new HashMap<Integer, Condition>();
	
	public DeltaStore() {
		acquired_state_ = new HashMap<Integer, Queue<ObjectNode>>();
		
		// Initialize each of the lists in the map of acquired state. 
		for (Integer proc : ShMem.addresses.keySet()) {
			acquired_state_.put(proc,  new LinkedList<ObjectNode>());
		}
		
		// Initialize the condition and lock maps for synchronizing between
		// ShMemServer writes and ShMemClient reads. 
		for (Integer proc : ShMem.addresses.keySet()) {
			Lock to_add = new ReentrantLock();
			Condition wait_condition = to_add.newCondition();
			locks_.put(proc, to_add);
			conditions_.put(proc,  wait_condition);
		}
	}
	
	// 
	// Called when the ShMemServer gets a newly released value.  
	//
	public void Push(int from, ObjectNode delta) {
		
		// Grab the appropriate queue's lock, we're about to perform a write. 
		locks_.get(from).lock();
		acquired_state_.get(from).add(delta);
		
		// Wakeup threads sleeping on this condition, unlock and return.
		conditions_.get(from).signal();
		locks_.get(from).unlock();
	}
	
	// 
	// Called when we need to acquire from another process. 
	// 
	public ObjectNode Pop(int from) {
		
		// What to return..
		ObjectNode ret = null;
		
		// We need to acquire the queue lock because we're reading from it. 
		locks_.get(from).lock();
		try {
			
			// Make sure that we're reading from a non-empty queue. 
			// If it's empty, go to sleep. 
			while (acquired_state_.get(from).isEmpty()) {
				conditions_.get(from).await();
			}
		}
		catch (Exception e) {
			
			// We should never get here!
			locks_.get(from).unlock();
			e.printStackTrace();
			System.exit(-1);
		}
		
		// Get the head of the queue. 
		ret = acquired_state_.get(from).poll();
		locks_.get(from).unlock();
		
		// Make sure we don't return null!
		assert(ret != null);
		return ret;
	}
}
