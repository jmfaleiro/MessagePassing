#!/usr/bin/env python
import sys,os
sys.path.append('../../../mp/python/')
from ShMemObject import *
from ShMem import *
from timestamp_util import *

from os import listdir
from os.path import isfile, join

test_file = open('test.txt', 'r')
ShMem.init(test_file, 1)
ShMem.start()
test_file.close()

ShMem.Acquire(0)

tree = ShMem.s_state
shin = tree.get('0')



def findfiles(shmem):
        ret = {}
        # Iterate through all the keys in the current ShMemObject
        for key,value in shmem.m_values.iteritems():
	    isfile = 'false'
            if isinstance(value, ShMemObject):
		# directory
                ret[key] = findfiles(value)
            else:
		# file
		# check if this worker should clean it (depends on filetype)
		if key.endswith('.pdf'):
			# write to file
			filename = 'temp/' + key
			g = open(filename,'wb')
	    		g.write(bytes)
	    		g.close()
			# clean the file
			os.system('../../../../../mat/mat ' + filename)

			# then read file back into shmem object
                	ret[key] = value
	        	print key
    
        return ret

print findfiles(shin)


# find all python files and print out those files
