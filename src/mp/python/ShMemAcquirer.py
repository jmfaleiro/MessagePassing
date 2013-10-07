import socket
import json
from threading import *
from Queue import *
import thread

# Instance fields: 
# m_serversock		-- Server socket used by the acquiring server thread.
# m_port		-- Port the server thread binds to. 
# m_recvd_objs		-- Dictionary of concurrent queues we've received from. 
class ShMemAcquirer:    

    # Class constructor, all we do is specify the port to which it should bind. 
    def __init__(self, port, index, receive_queues):
        self.m_serversock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.m_port = port
        self.m_recvd_objs = receive_queues
        
        print self.m_port
        # Create a new thread to run the server. 
        thread.start_new_thread(ShMemAcquirer.run, (self,))
    
    # The actual server logic. 
    @staticmethod
    def run(me):        

        # Bind to the port and tell it we're ready to start listening to clients
        me.m_serversock.bind(("localhost", me.m_port))
        me.m_serversock.listen(5)
        
        # Server loop: listen for a client connection, pull out a set of bytes
        # and keep that serialized representation in a dictionary. 
        while 1:
            (clientsocket, address) = me.m_serversock.accept()
            msg = ''            

            # Receive the message from the client. 
            while 1:
                chunk = clientsocket.recv(4096)
            
                # We've received the entire message. 
                if len(chunk) == 0:
                    break
                else:
                    msg += chunk

            # Close the resource. 
            clientsocket.close()
            # Do some deserialization. 
            recvd_obj = json.loads(msg, 'utf-8')
            msg = recvd_obj['argument']
            sender = recvd_obj['releaser']  
 
            # Put the recieved state into the right receive queue. 
            me.m_recvd_objs[sender].put(msg, True)

