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
    
    def checkGah(self, diff):
        name_wrapper = diff['name']
        name_timestamp = name_wrapper['shmem_timestamp']
        self.assertEqual(Timestamp.CompareTimestamps(name_timestamp, [2,2,0,0]),
                         Comparison.EQUAL)

        second = name_wrapper['value']

        second_wrapper = second['second']
        second_timestamp = second_wrapper['shmem_timestamp']
        self.assertEqual(Timestamp.CompareTimestamps(second_timestamp, 
                                                     [2,2,0,0]),
                         Comparison.EQUAL)

        gah = second_wrapper['value']		# gah
        gah_wrapper = gah['gah']
        self.assertEqual(
            Timestamp.CompareTimestamps(
                gah_wrapper['shmem_timestamp'], 
                [2,2,0,0]),
            Comparison.EQUAL)
        
        self.assertEqual(gah_wrapper['value'], 'blah')

    def checkWatson(self, diff):
        name_wrapper = diff['name']
        cs_wrapper = name_wrapper['value']['CS']
        cs_timestamp = cs_wrapper['shmem_timestamp']
        self.assertEqual(Timestamp.CompareTimestamps(cs_timestamp,[2,0,0,0]),
                         Comparison.EQUAL)        
        self.assertEqual(cs_wrapper['value'], 'Watson')

    def checkYale(self, diff):
        name_wrapper = diff['name']
        yale_wrapper = name_wrapper['value']['Yale']
        yale_timestamp = yale_wrapper['shmem_timestamp']
        self.assertEqual(Timestamp.CompareTimestamps(yale_timestamp,[1,0,0,0]),
                         Comparison.EQUAL)
        self.assertEqual(yale_wrapper['value'], 'University')


    # Initialize an ShMemObject with a set of keys and timestamps so that we 
    # can test our diff-n-merge code. 
    def standardInit(self):
        ShMem.init('test_file.txt', 0)
        ShMem.start()
        
        first_obj = ShMemObject()
        first_obj.put_simple('Yale', 'University')
        ShMem.s_state.put_object('name', first_obj)        
        
        # Increment time. 
        Timestamp.LocalIncrement(ShMemObject.s_now)        
        first_obj.put_simple('CS', 'Watson')

        # Change s_now arbitratily. 
        ShMemObject.s_now = [2,2,0,0]

        # Make sure that timestamps work properly when we're nesting objects. 
        second_obj = ShMemObject()
        second_obj.put_simple('gah', 'blah')
        first_obj.put_object('second', second_obj)        
        
    def testMerge(self):

        # Generate an obvious leaf-leaf conflict. The merge should fail. 
        self.standardInit()        
        to_merge = {'name' : 
                    {'shmem_timestamp': [0,0,1,0], 
                     'value': 
                     {'Yale' : 
                      {'shmem_timestamp':[0,0,1,0],
                       'value' : 'College'}}}}
        
        with self.assertRaises(AttributeError):
            ShMem.s_state.merge(to_merge)
            
        # Make sure that a non-conflicting change at a non-comparable timestamp
        # propagates the right timestamp to the root. 
        self.standardInit()            
        to_merge = {'jose' : 
                    {'shmem_timestamp' : [0,0,1,0],
                     'value' : 'faleiro'}}
        ShMem.s_state.merge(to_merge)
        faleiro = ShMem.s_state.get('jose')
        self.failUnless(faleiro == 'faleiro')
        self.assertEqual(Timestamp.CompareTimestamps(ShMemObject.s_now,
                                                     [2,2,1,0]),
                         Comparison.EQUAL)
        
        # Leaf-leaf conflict but the timestamp of one subsumes the other. 
        # Shouldn't fail and the new timestamp should propagate to the root. 
        self.standardInit()
        to_merge = {'name' : 
                    {'shmem_timestamp': [2,2,1,0], 
                     'value': 
                     {'Yale' : 
                      {'shmem_timestamp':[2,2,1,0],
                       'value' : 'College'}}}}
        ShMem.s_state.merge(to_merge)
        self.assertEqual(Timestamp.CompareTimestamps(ShMemObject.s_now,
                                                     [2,2,1,0]),
                         Comparison.EQUAL)
        self.assertEqual(ShMem.s_state.get('name').get('Yale'), 'College')
        yale_timestamp = ShMem.s_state.get('name').m_timestamps['Yale']
        self.assertEqual(yale_timestamp, [2,2,1,0])
        name_timestamp = ShMem.s_state.m_timestamps['name']
        self.assertEqual(name_timestamp, [2,2,1,0])        
        
    # Test diffing an ShMemObject.         
    def testDiff(self):
        self.standardInit()
        
        # We're asking for a diff whose timestamp subsumes everything in the 
        # ShMemObject. 
        diff = ShMem.s_state.get_diffs([2,3,0,0])
        self.failUnless(len(diff) == 0)

        diff = ShMem.s_state.get_diffs([2,1,0,0])
        self.checkGah(diff)
        
        diff = ShMem.s_state.get_diffs([1,0,0,0])
        self.checkGah(diff)
        self.checkWatson(diff)
        
        diff = ShMem.s_state.get_diffs([0,0,0,0])
        self.checkGah(diff)
        self.checkWatson(diff)
        self.checkYale(diff)

        
    # Test gets and puts into an ShMemObject. 
    def testPutGet(self):
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
        
        # Make sure that we actually managed to change the time. 
        self.assertEqual(Timestamp.CompareTimestamps(ShMemObject.s_now,
                                                     [2,0,0,0]),
                         Comparison.EQUAL)
        
        first_obj.put_simple('CS', 'Watson')
        
        # The timestamp of the object we previously inserted should not
        # change. 
        yale_timestamp = first_obj.m_timestamps['Yale']
        self.assertEqual(Timestamp.CompareTimestamps(yale_timestamp,
                                                     [1,0,0,0]),
                         Comparison.EQUAL)

        # The newly inserted item must have the right timestamp. 
        cs_timestamp = first_obj.m_timestamps['CS']
        self.assertEqual(Timestamp.CompareTimestamps(cs_timestamp,
                                                     [2,0,0,0]),
                         Comparison.EQUAL)

        # Make sure that 'name's timestamp has changed to the right value. 
        name_timestamp = ShMem.s_state.m_timestamps['name']
        self.assertEqual(Timestamp.CompareTimestamps(name_timestamp, [2,0,0,0]),
                         Comparison.EQUAL)

        # Change s_now arbitratily. 
        ShMemObject.s_now = [2,2,0,0]

        # Make sure that timestamps work properly when we're nesting objects. 
        second_obj = ShMemObject()
        second_obj.put_simple('gah', 'blah')
        first_obj.put_object('second', second_obj)
        self.assertEqual(Timestamp.CompareTimestamps(name_timestamp, [2,2,0,0]),
                         Comparison.EQUAL)        

        
def main():
    unittest.main()

if __name__ == '__main__':
    main()
