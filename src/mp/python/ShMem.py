from ShMemObject import *
from ShMemAcquirer import *
from ShMemReleaser import *
from timestamp_util import *
from Queue import *

# Static fields:
# s_addresses		-- Pairs of (IP, port) on which remote procs live.
# s_state		-- This node's ShMemObject root. 
# s_index		-- The global id of this node (int)
# s_send_queue		-- Contains state to be asynchronously sent. 
# s_receive_queues	-- Dictionary of queues of received state.
# s_last_sync		-- Dictionary of the last sync time with other nodes. 
# s_acquirer		-- Instance of ShMemAcquirer for this node. 
# s_releaser 		-- Instance of ShMemReleaser for this node. 
class ShMem:    

    # Populate the "address book" with the addresses (IP, port) pairs of 
    # nodes in the system. 
    @staticmethod
    def populate_addresses(input_file):
        ret = {}
        counter = 0
        for line in input_file:
            parts = line.split(' ')
            listen_port = int(parts[1])
            ret[counter] = (parts[0], listen_port)
            counter += 1
        return ret

    # Initialize s_state. All nodes inserted into s_state after this call 
    # but before the call to start are not shipped to other nodes. We do this
    # so that all nodes can potentially start with the same initial state. 
    @staticmethod
    def init(input_file, my_id):
        ShMem.s_index = my_id
        
        # Get the address of each node. 
        ShMem.s_addresses = ShMem.populate_addresses(input_file)
        Timestamp.init(len(ShMem.s_addresses), my_id)
        ShMemObject.s_now = Timestamp.CreateZero()
        ShMem.s_state = ShMemObject()

        # Initialize a single send queue which we'll use to communicate with
        # an ShMemReleaser thread. 
        ShMem.s_send_queue = Queue()

        # Initialize a set of receive queues which the ShMemAcquirer thread will
        # use to put recieved state.
        ShMem.s_receive_queues = {}
        for i in range(0, len(ShMem.s_addresses)):
            if i != my_id:
                ShMem.s_receive_queues[i] = Queue()
        
    # Once this function is called, all changes made to s_state are tracked
    # and sent as part of this diffing process. 
    @staticmethod
    def start(test=True):
        ip, port = ShMem.s_addresses[ShMem.s_index]
        total_nodes = len(ShMem.s_addresses)

        if test:
            print 'here'
            ShMem.s_acquirer = ShMemAcquirer(port, 
                                             ShMem.s_index, 
                                             ShMem.s_receive_queues)
            ShMem.s_releaser = ShMemReleaser(ShMem.s_index, ShMem.s_send_queue,
                                             ShMem.s_addresses)

        # Keep a dictionary of the last time we synced with a remote node. 
        ShMem.s_last_sync = {}
        for i in range(0, total_nodes):
            ShMem.s_last_sync[i] = Timestamp.CreateZero()
        
        Timestamp.LocalIncrement(ShMemObject.s_now)
        
    # Merge s_state with the state we've received from a remote node. 
    @staticmethod
    def Acquire(node):
        delta = ShMem.s_receive_queues[node].get(True)
        ShMem.s_state.merge(delta.get('value'))
        Timestamp.Union(ShMemObject.s_now, delta.get('time'))
        Timestamp.Union(ShMem.s_last_sync[node], delta.get('time'))
        Timestamp.LocalIncrement(ShMemObject.s_now)

    # Release our diffs to another node. Put it in the send queue for the
    # ShMemReleaser thread to get to. 
    @staticmethod
    def Release(node):
        last_sync = ShMem.s_last_sync[node]
        to_send = {'time' : Timestamp.Copy(ShMemObject.s_now),
                   'value' : ShMem.s_state.get_diffs(last_sync)}
        Timestamp.Copy(ShMemObject.s_now, last_sync)
        ShMem.s_releaser.send(node, to_send)
        Timestamp.LocalIncrement(ShMemObject.s_now)
        
        
        
        
