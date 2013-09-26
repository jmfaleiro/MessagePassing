import socket
import json
import thread

class ShMemAcquirer:    

    # Class constructor, all we do is specify the port to which it should bind. 
    def __init__(self, port, index, total_nodes):
        self.m_serversock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.m_port = port
        self.m_recvd_objs = []
        
        # For now, just use a coarse-grained lock on the entire received state
        # when we wish to acquire state. 
        self.m_queue_lock = threading.Lock()
        
        for i in range(0, total_nodes):
            self.m_recvd_objs[i] = []                

        # Create a new thread to run the server. 
        thread.start_new_thread(self.run())
    
    # The actual server logic. 
    def run(self):        

        # Bind to the port and tell it we're ready to start listening to clients
        self.m_serversock.bind((socket.gethostname(), self.m_port))
        self.m_serversock.listen(5)
        
        # Server loop: listen for a client connection, pull out a set of bytes
        # and keep that serialized representation in a dictionary. 
        while 1:
            (clientsocket, address) = self.m_serversock.accept()
            msg = ''            

            # Receive the message from the client. 
            while 1:
                chunk = clientsocket.recv()
            
                # We've received the entire message. 
                if len(chunk) == 0:
                    break
                else:
                    msg += chunk
                
            # Do some deserialization. 
            recvd_obj = json.load(msg)
            serialized_msg = recvd_obj['argument']
            sender = recvd_obj['releaser']                
            msg = json.load(serialized_msg)            
            
            # Put the recieved state into the right receive queue. 
            self.m_queue_lock.acquire()
            self.m_recvd_objs[sender].append(msg)
            self.m_queue_lock.release()
