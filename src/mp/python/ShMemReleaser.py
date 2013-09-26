import socket
import json
import thread
import ConcurrentQueue
import ShMem

# Instance fields:
# m_id			-- The id of this node (int)
# m_send_queue		-- Queue of state to be sent to remote nodes. 
# m_condition		-- Condition sending thread waits on if queue is empty
# m_sock		-- Socket used by the sending thread. 
class ShMemReleaser:
    
    def __init__(self, my_id, queue):
        self.m_send_queue = queue
        self.m_id = my_id
        lock = threading.RLock()
        self.m_condition = threading.condition
        self.m_sock = socket.sock(socket.AF_INSET, socket.SOCK_STREAM)
        thread.start_new_thread(self.run())

    # The thread running this method sends data asynchronously to other
    # processes. 
    def run(self):
        while True:

            # Get the item we want to send. 
            item = self.m_send_queue.dequeue()            
            to_send = item['obj']
            address = ShMem.address_book[item['to']]
            
            # Open a connection to the acquirer and send the data.
            self.m_sock.connect(address)            
            if self.m_sock.sendall(to_send) != None:
                print "Send error!\n"
                sys.exit(0)            
            self.m_sock.close()            
            
    # This method is called by the application to enqueue data to send
    # in the send queue.
    def send(self, receiver, state):
        
        # Serialize what we want to send.
        to_send = {}
        to_send['argument'] = state
        to_send['releaser'] = my_id
        serialized_state = json.dumps(state)
        
        # Keep track of the receiver and put the object in a queue of objects
        # we want to send. 
        send_obj = {'to' : receiver, 'obj' : serialized_state}
        self.m_send_queue.append(send_obj)
