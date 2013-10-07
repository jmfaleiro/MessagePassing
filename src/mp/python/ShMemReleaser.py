import socket
import json
from base64 import b64encode
import thread
from Queue import *
import ShMem

# Instance fields:
# m_id			-- The id of this node (int)
# m_send_queue		-- Queue of state to be sent to remote nodes. 
# m_condition		-- Condition sending thread waits on if queue is empty
# m_sock		-- Socket used by the sending thread. 
class ShMemReleaser:
    
    def __init__(self, my_id, queue, addresses):
        self.m_send_queue = queue
        self.m_id = my_id
        self.m_addresses = addresses
        thread.start_new_thread(ShMemReleaser.run, (self,))

    # The thread running this method sends data asynchronously to other
    # processes. 
    @staticmethod
    def run(me):
        while True:

            # Get the item we want to send. 
            item = me.m_send_queue.get(True)
            to_send = item['obj']
            address = me.m_addresses[item['to']]
            not_sent = True

            while not_sent:
                # Open a connection to the acquirer and send the data.
                my_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                try:
                    my_socket.connect(address)
                except IOError:     
                    # XXX: Should add some back-off here!
                    #print 'here!'
                    my_socket.close()
                    continue

                if my_socket.sendall(to_send) != None:
                    print "Send error!\n"
                    sys.exit(0)            
                my_socket.close()            
                not_sent = False
            
            
    # This method is called by the application to enqueue data to send
    # in the send queue.
    def send(self, receiver, state):
        
        # Serialize what we want to send.
        to_send = {}
        to_send['argument'] = state
        to_send['releaser'] = self.m_id
        
        # Keep track of the receiver and put the object in a queue of objects
        # we want to send. 
        send_obj = {'to' : receiver, 'obj' : json.dumps(to_send)}
        self.m_send_queue.put(send_obj, True)
