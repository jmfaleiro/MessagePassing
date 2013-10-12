#!/usr/bin/env python
import sys,os,base64
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
	    bytes = f.read()
	    
	    bytes = base64.b64encode(bytes)

	    # CHANGE THIS TO WRITE FILE BYTES TO SHMEM OBJECT
	    result.put_simple(a, bytes)
	else:
	    result.put_object(a,dir2shmem(a))
	#result = result + [dir2shmem(a)]
	#print "returning " + str(result)
    return result

#x = getInfos('./')
#print x
#print os.path.abspath('../')
#constshmem = dir2shmem(os.path.abspath('../'))
output = dir2shmem('./')



tree.put_object('0',output)
print "result " + str(tree.get_plain_diffs())
print

# print constshmem.get_plain_diffs()


print 'done'
print
print

tree.get_plain_diffs()
ShMem.Release(1)

ShMem.Release(2)

# wait for workers to clean files
print "waiting for workers...."

ShMem.Acquire(1)
ShMem.Acquire(2)

print "writing clean files..."

def writefiles(shmem):
        ret = {}
        # Iterate through all the keys in the current ShMemObject
        for key,value in shmem.m_values.iteritems():
	    print key
            if isinstance(value, ShMemObject):
		# directory
                ret[key] = writefiles(value)
            else:
		# file
		# write to file
		key2 = key.replace('./','')
		filename = 'out/' + key2

		# check if is a directory
		if key2.count('/') > 0 :
			# is a directory
			# find the directory path
			key3 = key2[:key2.rfind('/')]
			# make the directory
			dirname = './out/' + key3
			if not os.path.exists(dirname):
	    			os.makedirs(dirname)

			
			print key + ' ' + 'dir'

		print "writing to: " + filename
		g = open(filename,'wb')
    		g.write(base64.b64decode(value))
    		g.close()
	
        return ret

tree = ShMem.s_state
shin = tree.get('0')
writefiles(shin)

print
#print shin.get_plain_diffs()

print "done"

while 1:pass




"""
tweets = ShMemObject()
tweets2 = ShMemObject()
tweets2.put_simple("fool","this is weak")
tweets.put_object("weak",tweets2)
	    
tweets.get_plain_diffs()




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
