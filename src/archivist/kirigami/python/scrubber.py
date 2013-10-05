#!/usr/bin/env python
import sys,os
sys.path.append('../../../mp/python/')
from ShMemObject import *
from ShMem import *
from timestamp_util import *

from os import listdir
from os.path import isfile, join

test_file = open('test.txt', 'r')
ShMem.init(test_file, 0)
ShMem.start()
test_file.close()
tweets = ShMemObject()

tree = ShMem.s_state

# takes a directory and returns an Shmem object representing that directory
def dir2shmem(currentDir):
    #files = [ f for f in listdir(currentDir) if isfile(join(currentDir,f)) ]
    #print files

    allindir = os.listdir(currentDir)
    #print allindir
    #result = [currentDir]
    #dirs = list(set(allindir) - set(files))
    result = ShMemObject()

    for a in allindir:
	if currentDir != './' and currentDir != '../':
	    a = currentDir + '/' + a
	else:
	    a = currentDir + a
	if isfile(a):
	    f = open(a,'rb')
	    bytes = ''
	    for byte in f:
		bytes += byte
	    f.close()
	    #g = open(a + 'weak','wb')
	    #g.write(bytes)
	    #g.close()
	    result.put_simple(a,bytes)
	else:
	    result.put_object(a,dir2shmem(a))
	#result = result + [dir2shmem(a)]
	#print "returning " + str(result)
    return result

#x = getInfos('./')
#print x
#print os.path.abspath('../')
#constshmem = dir2shmem(os.path.abspath('../'))
print "result " + str(dir2shmem('./').get_plain_diffs())
print

# print constshmem.get_plain_diffs()


print 'done'
print
print

tree.get_plain_diffs()

tweets = ShMemObject()
tweets2 = ShMemObject()
tweets2.put_simple("fool","this is weak")
tweets.put_object("weak",tweets2)
	    
tweets.get_plain_diffs()



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
