# This enum is used to tell the relation between two different
# timestamps. 
class Comparison:
    LESS_THAN = 1
    EQUAL = 2
    BIGGER_THAN = 3
    NONE = 4

class Timestamp:

    @staticmethod
    def init(vector_size, index):
        Timestamp.s_size = vector_size
        Timestamp.s_index = index

    @staticmethod
    def LocalIncrement(vector):
        vector[Timestamp.s_index] += 1

    # Create a zero-vector. 
    @staticmethod
    def CreateZero():
        ret = []
        for i in range(0, Timestamp.s_size):
            ret.append(0)            
        return ret

    # Copy the contents of from_vector into to_vector. 
    @staticmethod
    def Copy(from_vector, to_vector):
        for i in range(0, Timestamp.s_size):
            to_vector[i] = from_vector[i]

    @staticmethod
    def CreateCopy(vector):
        ret = []
        for i in range(0, Timestamp.s_size):
            ret.append(vector[i])
        return ret

    # Take the union of the vector timestamps to_union and with_union.
    # Put the results in to_union
    @staticmethod
    def Union(to_union, with_union):
        for i in range(0, len(to_union)):
            if with_union[i] > to_union[i]:
                to_union[i] = with_union[i]
            

    # Compare two timestamps.
    @staticmethod
    def CompareTimestamps(ts1, ts2):
        bigger_than = False
        less_than = False
        
        for i in range(0, len(ts1)):
            bigger_than = (ts1[i] > ts2[i]) | bigger_than
            less_than = (ts1[i] < ts2[i]) | less_than
    
        if bigger_than and less_than:
            return Comparison.NONE
        elif bigger_than:
            return Comparison.BIGGER_THAN
        elif less_than:
            return Comparison.LESS_THAN
        else:
            return Comparison.EQUAL
