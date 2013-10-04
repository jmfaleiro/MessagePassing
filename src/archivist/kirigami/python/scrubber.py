#!/usr/bin/env python
import sys,os
sys.path.append('../../../mp/python/')
from ShMemObject import *
from ShMem import *
from timestamp_util import *

test_file = open('test.txt', 'r')
ShMem.init(test_file, 0)
ShMem.start()
test_file.close()
tweets = ShMemObject()

tree = ShMem.s_state

counter = 0
val = {}
val["weak"] = "code"

tweets.put_simple(str(counter),val)
x = tweets.get(str(0))
print x

def getInfos(currentDir):
    infos = []
    
    dirQ = []

    # os.path.join(dirpath, name)

    for root, dirs, files in os.walk(currentDir): # Walk directory tree, goes once round loop for each "level" in directory tree
	#print root
	#print dirs
	currShMem = tree
	for d in dirs:
	    direc = ShMemObject()
	    # add ShMem object 
	    currShMem.put_object(d,direc)
	    print "dir " + d
	    # add shmem object for this dir to queue
	    dirQ += [direc]

	counter = 0

	#print files
        for f in files:
	    print "files " + f
	    val = {}
	    currShMem.put_simple(f,val)
	    counter = counter + 1

            #infos.append(root + f)
     	    """
            file_bytes = open(root + f, 'rb')
	    file_val = ''
	    for byte in file_bytes:
	      file_val += byte
	    print file_val
	    """
	if len(dirQ) > 0:
	    currShMem = dirQ.pop()

    print dirQ
    return infos

x = getInfos('../')
print x

print 'done'
print
print

print tree.get_plain_diffs()

tweets = ShMemObject()
tweets2 = ShMemObject()
tweets2.put_simple("fool","this is weak")
tweets.put_object("weak",tweets2)
	    
print tweets.get_plain_diffs()



"""
class UrlAggregator:
    
    @staticmethod
    def process():
        next_tweet = 0
        while 1:
            
            # Get state from the acquirer. 
            ShMem.Acquire(0)            
            tweets = ShMem.s_state.get('tweets')


                        
            # Finally, send state back to the master node. 
            ShMem.Release(0)


def main():
    input_file = open(sys.argv[1])
    ShMem.Init(input_file, int(sys.argv[2]))
    input_file.close()
    ShMem.Start()
    UrlAggregator.process()


if __name__ == "__main__":
    main()
"""
