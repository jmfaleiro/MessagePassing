# Unit test for Python ShMem library.
import sys
import unittest

# Import ShMem library specific classes. 
sys.path.append('../../mp/python')
from timestamp_util import *
from ShMem import *
from ShMemObject import *

class ShMemTest(unittest.TestCase):    

    def testTimestamps(self):
        Timestamp.init(3, 0)
        start_time = Timestamp.CreateZero()

        # Make sure that start_time is actually zero.
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [0,0,0]),
                         Comparison.EQUAL)
        
        # Increment the local index and compare with a variety of other 
        # vectors. 
        Timestamp.LocalIncrement(start_time)
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [1,0,0]),
                         Comparison.EQUAL)
        
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [1,0,1]),
                         Comparison.LESS_THAN)
        
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [0,0,0]),
                         Comparison.BIGGER_THAN)

        self.assertEqual(Timestamp.CompareTimestamps(start_time, [0,1,0]),
                         Comparison.NONE)
        
        # Copy some random values and check that we get something expected. 
        Timestamp.Copy([4,4,4], start_time)
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [4,4,4]),
                         Comparison.EQUAL)
        
        # Union with some random value and check that we get something
        # expected. 
        Timestamp.Union(start_time, [3,7,3])
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [4,4,4]),
                         Comparison.BIGGER_THAN)
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [4,7,4]),
                         Comparison.EQUAL)
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [5,5,5]),
                         Comparison.NONE)

    def testStart(self):
        ShMem.init('test_file.txt', 0)
        
        
        ShMem.start()
        self.assertEqual(ShMemObject.s_now, [1,0,0,0])

        first_obj = ShMemObject()
        first_obj.put_simple('Yale', 'University')
        ShMem.s_state.put_object('name', first_obj)
        
        # Make sure that gets work properly.
        temp = ShMem.s_state.get('name')
        self.assertEqual(first_obj, temp)        
        self.failUnless(temp.get('Yale') == 'University')
        
        # Increment time. 
        Timestamp.LocalIncrement(ShMemObject.s_now)
        first_obj.put_simple('CS', 'Watson')
        
        # Make sure that 'name's timestamp has changed to the right value. 

        
def main():
    unittest.main()

if __name__ == '__main__':
    main()
