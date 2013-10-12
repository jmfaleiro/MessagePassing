import copy
import sys
from timestamp_util import *


# Instance fields:
# m_parent 		-- ShMemObject that's the parent of this object
# m_parent_key		-- The key whose value is this object
# m_values		-- Dictionary of key-value pairs. 
# m_timestamps		-- Dictionary of key-timestamp pairs. 
class ShMemObject:
        
    def __init__(self):
        self.m_parent = None
        self.m_parent_key = None
        self.m_values = {}	# JSON dictionary
        self.m_timestamps = {}

        
    # Propagate the timestamp "time" from the node cur all the way up to
    # the root. 
    @staticmethod
    def fixTime(cur, time):
        while not (cur.m_parent is None):
            key = cur.m_parent_key
            cur_timestamp = cur.m_parent.m_timestamps[key]
            
            comp = Timestamp.CompareTimestamps(cur_timestamp, time)
            if comp != Comparison.BIGGER_THAN:
                Timestamp.Union(cur_timestamp, time)
                cur = cur.m_parent
            else:
                break

    # This method is meant to be used when 'value' is an ShMemObject
    def put_object(self, key, value):
        value.m_parent = self
        value.m_parent_key = key
        self.put_simple(key, value)

    # This method should be used when 'value' is a simple type, that is,
    # an array, int, double, string, byte[]. 
    def put_simple(self, key, value):
        self.m_values[key] = value
        self.m_timestamps[key] = copy.deepcopy(ShMemObject.s_now)
        ShMemObject.fixTime(self, ShMemObject.s_now)
    
    def getKeys(self):
        return self.m_values.keys()

    # Getter for a given key
    def get(self, key):
        return self.m_values[key]
    
    # Forcibly insert a key-value pair at the specified timestamp. We use
    # this method only while merging with a remote node's state. 
    def insertAt(self, key, value, timestamp):
        self.m_timestamps[key] = timestamp
        self.m_values[key] = value
        if isinstance(value, ShMemObject):
            value.m_parent = self
            value.m_parent_key = key
        ShMemObject.fixTime(self, timestamp)

    @staticmethod
    def deserializeObjectNode(obj):
        
        # If the value is not a dictionary, then we're done. 
        if not isinstance(obj, dict):
            return obj
        
        # The value is a dictionary, we have to recursively deserialize every
        # one of its values
        ret = ShMemObject()
        for obj_key,obj_wrapper in obj.iteritems():
            real_value = obj_wrapper['value']
            timestamp = obj_wrapper['shmem_timestamp']            
            deserialized_value = ShMemObject.deserializeObjectNode(real_value)
            ret.insertAt(obj_key, deserialized_value, timestamp)
        return ret

    def do_recursive_insert(self, key, wrapped_value):
        to_insert = ShMemObject.deserializeObjectNode(wrapped_value['value'])
        self.insertAt(key, to_insert, wrapped_value['shmem_timestamp'])

    def merge(self, obj):
        for other_key,wrapped_value in obj.iteritems():
            other_timestamp = wrapped_value['shmem_timestamp']
            other_value = wrapped_value['value']
            
            if other_key in self.m_values:
                my_timestamp = self.m_timestamps[other_key]
                my_value = self.m_values[other_key]
                comp = Timestamp.CompareTimestamps(my_timestamp, 
                                                   other_timestamp)

                # Check if either one of the values is a leaf node, if so,
                # then this is the time to check for conflicting changes. 
                if not isinstance(my_value, ShMemObject) or not isinstance(other_value, dict):

                    if comp == Comparison.NONE:
                        print 'Merge failure!'
                        my_value.merge(other_value)
                        sys.exit('Merge failure!')
                    
                    if comp == Comparison.LESS_THAN:
                        self.do_recursive_insert(other_key, wrapped_value)
                    
                # Neither of them are leaf nodes. 
                else:
                    if comp != Comparison.BIGGER_THAN:
                        my_value.merge(other_value)
            else:
                self.do_recursive_insert(other_key, wrapped_value)
            
    # Print state as a plain dictionary without timestamp junk.
    def get_plain_diffs(self):
        ret = {}
        # Iterate through all the keys in the current ShMemObject
        for key,value in self.m_values.iteritems():

            # Get the timestamp of the current key-value pair.
            # If the timestamp is greater than 'timestamp' then we need to add
            # the value to the diff tree.             
                
                # If the value is itself an ShMemObject, then recursively
                # fetch the parts of the object that were changed after
                # 'timestamp'. Otherwise, we just fetch the leaf. 
            if isinstance(value, ShMemObject):
                serialized_value = value.get_plain_diffs()
            else:
                serialized_value = value
            ret[key] = serialized_value        
        return ret

        
    # Get the diffs of this ShMemObject which correspond to a time
    # later than the arugment 'timestamp'. It retuns an object which can 
    # be immediately serialized using Python's JSON library.
    def get_diffs(self, timestamp):
        ret = {}

        # Iterate through all the keys in the current ShMemObject
        for key,value in self.m_values.iteritems():

            # Get the timestamp of the current key-value pair.
            # If the timestamp is greater than 'timestamp' then we need to add
            # the value to the diff tree.             
            cur_timestamp = self.m_timestamps[key]
            comp = Timestamp.CompareTimestamps(timestamp, cur_timestamp)
            if comp != Comparison.BIGGER_THAN:
                
                # If the value is itself an ShMemObject, then recursively
                # fetch the parts of the object that were changed after
                # 'timestamp'. Otherwise, we just fetch the leaf. 
                if isinstance(value, ShMemObject):
                    serialized_value = value.get_diffs(timestamp)
                else:
                    serialized_value = value
                    
                wrapper = {}
                wrapper['shmem_timestamp'] = cur_timestamp
                wrapper['value'] = serialized_value
                ret[key] = wrapper
        return ret
            


