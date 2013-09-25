import copy
import timestamp_util

class ShMemObject:
    
    # This static variable tells us what time it is
    # "now". 
    s_now = []
        
    def __init__(self):
        self.m_parent = parent
        self.m_parent_key = parent_key        
        self.m_values = {}	# JSON dictionary
        self.m_timestamps = {}

        
    # Propagate the timestamp "time" from the node cur all the way up to
    # the root. 
    def fixTime(cur, time):
        while not (cur.m_parent is None):
            key = cur.m_parent_key
            cur_timestamp = cur.m_parent.m_timestamps[key]
            
            comp = Util.CompareTimestamps(cur_timestamp, time)
            if comp == Comparison.LESS_THAN:
                Util.Union(cur_timestamp, time)
                cur = cur.m_parent
            else:
                break

    # This method is meant to be used when 'value' is an ShMemObject
    def put_object(self, key, value):
        self.put_simple(key, value)
        value.m_parent = self
        value.m_parent_key = key

    # This method should be used when 'value' is a simple type, that is,
    # an array, int, double, string, byte[]. 
    def put_simple(self, key, value):
        self.m_values[key] = value
        self.m_timestamps[key] = copy.deepcopy(ShMemObject.s_now)

    # Getter for a given key
    def get(self, key):
        return self.m_values[key]
    
    # Forcibly insert a key-value pair at the specified timestamp. We use
    # this method only while merging with a remote node's state. 
    def insertAt(self, key, value, timestamp):
        Util.Union(ShMemObject.s_now, timestamp)
        self.m_timestamps[key] = timestamp
        self.m_values[key] = value
        fixTime(self, timestamp)


    def deserializeObjectNode(obj):
        
        # If the value is not a dictionary, then we're done. 
        if not (type(obj) is dict):
            return obj
        
        # The value is a dictionary, we have to recursively deserialize every
        # one of its values
        ret = ShMemObject()
        for obj_key,obj_wrapper in obj:
            real_value = obj_wrapper['value']
            timestamp = obj_wrapper['timestamp']            
            deserialized_value = deserializeObjectNode(real_value)
            ret.insertAt(obj_key, deserialized_value, timestamp)
        return ret

    def do_recursive_insert(self, key, wrapped_value):
        to_insert = deserializeObjectNode(obj['value'])
        self.insertAt(key, to_insert, obj['shmem_timestamp'])
        if type(to_insert) == ShMemObject:
            to_insert.m_parent = self
            to_insert.m_parent_key = key

    def merge(self, obj):
        for other_key,wrapped_value in obj:
            other_timestamp = wrapped_value['shmem_timestamp']
            other_value = wrapped_value['value']
            
            if other_key in self.m_values:
                my_timestamp = self.m_timestamps[other_key]
                my_value = self.m_values[other_key]
                comp = Util.CompareTimestamps(my_timestamp, other_timestamp)
                
                # Both have written to the same node. Try to merge, if we fail
                # then an exception gets automatically thrown. 
                # XXX: We need to change this for the future. 
                if comp == Comparison.NONE:
                    my_value.merge(other_value)
                elif comp == Comparison.LESS_THAN:
                    self.do_recursive_insert(other_key, wrapped_value)
            else:
                self.do_recursive_insert(other_key, wrapped_value)
            
        
    # Get the diffs of this ShMemObject which correspond to a time
    # later than the arugment 'timestamp'. It retuns an object which can 
    # be immediately serialized using Python's JSON library.
    def get_diffs(self, timestamp):
        ret = {}
        
        # Iterate through all the keys in the current ShMemObject
        for key,value in self.m_values:

            # Get the timestamp of the current key-value pair.
            # If the timestamp is greater than 'timestamp' then we need to add
            # the value to the diff tree.             
            cur_timestamp = self.m_timestamps[k]
            comp = Util.CompareTimestamps(timestamp, cur_timestamp)
            if comp == Comparison.LESS_THAN:
                
                # If the value is itself an ShMemObject, then recursively
                # fetch the parts of the object that were changed after
                # 'timestamp'. Otherwise, we just fetch the leaf. 
                if type(value) == ShMemObject:
                    serialized_value = get_diffs(value, timestamp)
                else:
                    serialized_value = value
                    
                wrapper = {}
                wrapper['shmem_timestamp'] = cur_timestamp
                wrapper['value'] = serialized_value
                ret[key] = wrapper
        return ret
            


