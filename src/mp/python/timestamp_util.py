# This enum is used to tell the relation between two different
# timestamps. 
class Comparison:
    LESS_THAN = 1
    EQUAL = 2
    BIGGER_THAN = 3
    NONE = 4

# Take the union of the vector timestamps to_union and with_union.
# Put the results in to_union
def Union(to_union, with_union):
    for i in range(0, len(to_union)):
        if with_union[i] > to_union[i]:
            to_union[i] = with_union[i]
            

# Compare two timestamps.
def CompareTimestamps(ts1, ts2):
    bigger_than = False
    less_than = False
    
    for i in range(0, len(ts1)):
        bigger_than |= ts1[i] > ts2[i]
        less_than |= ts1[i] < ts2[i]
    
    if bigger_than and less_than:
        return Comparison.NONE
    elif bigger_than:
        return Comparison.BIGGER_THAN
    elif less_than:
        return Comparison.LESS_THAN
    else:
        return Comparison.NONE
