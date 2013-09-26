import ShMemObject
import ShMemAcquirer
import ShMemReleaser
import timestamp_util
import ConcurrentQueue

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
    def populate_addresses(input_file):
        ret = {}
        ins = open(input_file, "r")
        counter = 0
        for line in ins:
            parts = line.split(' ')
            listen_port = int(parts[1])
            ret[counter] = (parts[0], listen_port)
            counter += 1

    # Initialize s_state. All nodes inserted into s_state after this call 
    # but before the call to start are not shipped to other nodes. We do this
    # so that all nodes can potentially start with the same initial state. 
    def init(address_book, my_id):
        ShMem.s_index = my_id
        
        # Get the address of each node. 
        ShMem.s_addresses = populate_addresses(address_book)
        Timestamp.init(len(ShMem.s_addresses), my_id)
        ShMemObject.s_now = Timestamp.CreateZero()
        ShMem.s_state = ShMemObject()

        # Initialize a single send queue which we'll use to communicate with
        # an ShMemReleaser thread. 
        ShMem.s_send_queue = ConcurrentQueue()

        # Initialize a set of receive queues which the ShMemAcquirer thread will
        # use to put recieved state.
        ShMem.s_receive_queues = {}
        for i in range(0, len(ShMem.s_addresses)):
            if i != my_id:
                ShMem.s_receive_queues[i] = ConcurrentQueue()
        
    # Once this function is called, all changes made to s_state are tracked
    # and sent as part of this diffing process. 
    def start():
        ip, port = ShMem.s_addresses[ShMem.s_index]
        total_nodes = len(ShMem.s_addresses)
        ShMem.s_acquirer = ShMemAcquirer(port, ShMem.s_index, total_nodes)
        ShMem.s_releaser = ShMemReleaser(ShMem.s_index, ShMem.s_send_queue)
        Timestamp.LocalIncrement(ShMemObject.s_now)
        
    # Merge s_state with the state we've received from a remote node. 
    def Acquire(node):
        delta = ShMem.s_receive_queues[node].dequeue()
        s_state.merge(delta)
        Timestamp.Copy(ShMem.s_now, ShMem.s_last_sync[node])
        ShMem.s_last_sync[node] = copy.deepcopy(ShMemObject.s_now)
        Timestamp.LocalIncrement(ShMemObject.s_now)

    # Release our diffs to another node. Put it in the send queue for the
    # ShMemReleaser thread to get to. 
    def Release(node):
        last_sync = ShMem.s_last_sync[node]
        cur_delta = ShMem.s_state.get_diffs(last_sync)
        Timestamp.Copy(ShMem.s_now, last_sync)
        ShMem.s_releaser.send(node, cur_delta)
        
        
        
        
