#!/usr/bin/env python
import sys,os,base64
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
        for key in shmem.getKeys():
	    isfile = 'false'
            value = shmem.get(key)
            if isinstance(value, ShMemObject):
		# directory
                ret[key] = findfiles(value)
            else:
		# file
		# check if this worker should clean it (depends on filetype)
		if key.endswith('.png'):
			# write to file
			filename = 'weak-test.png'
			g = open(filename,'wb')
	    		g.write(base64.b64decode(value))
	    		g.close()
			# clean the file
			os.system('../../../../../mat/mat ' + filename)
			
			g = open(filename,'rb')
	    		bytes = g.read()
	      	        bytes = base64.b64encode(bytes)
	    		g.close()
			# update the value for that key in shmem
			shmem.put_simple("key.png", bytes)
			shmem.put_simple(key, bytes)


			# delete the temporary file
			# os.system('rm weak-test.png')

			# then read file back into shmem object
                	ret[key] = value
	        	print key
    
        return ret

findfiles(shin)

ShMem.Release(0)

while 1:pass


