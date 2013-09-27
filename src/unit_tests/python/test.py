import sys
import unittest

sys.path.append('../../mp/python')
from timestamp_util import *

class ShMemTest(unittest.TestCase):    

    def testTimestamps(self):
        Timestamp.init(3, 0)
        start_time = Timestamp.CreateZero()

        # Make sure that start_time is actually zero.
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [0,0,0]),
                         Comparison.EQUAL)
        
        Timestamp.LocalIncrement(start_time)
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [1,0,0]),
                         Comparison.EQUAL)
        
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [1,0,1]),
                         Comparison.LESS_THAN)
        
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [0,0,0]),
                         Comparison.BIGGER_THAN)

        self.assertEqual(Timestamp.CompareTimestamps(start_time, [0,1,0]),
                         Comparison.NONE)
        
        Timestamp.Copy([4,4,4], start_time)
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [4,4,4]),
                         Comparison.EQUAL)
        
        Timestamp.Union(start_time, [3,7,3])
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [4,4,4]),
                         Comparison.BIGGER_THAN)
        self.assertEqual(Timestamp.CompareTimestamps(start_time, [4,7,4]),
                         Comparison.EQUAL)
'''        
    def testStart(self):
        ShMem.start()
        self.assertEqual(ShMem.s_now, [1,0,0,0])

        first_obj = ShMemObject()
        first_obj.put_simple('Yale', 'University')
        ShMem.s_state.put_object('name', first_obj)
        
        # Make sure that gets work properly.
        temp = ShMem.s_state.get('name')
        self.assertEqual(first_obj, temp)        
        self.failUnless(temp.get('Yale') == 'University')
        
        # Increment time. 
        Timestamp.LocalIncrement(ShMem.s_now)
        first_obj.put_simple('CS', 'Watson')
        
        # Make sure that 'name's timestamp has changed to the right value. 
'''        
        
def main():
    unittest.main()

if __name__ == '__main__':
    main()
