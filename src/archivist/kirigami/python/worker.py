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
print tree.get_plain_diffs()
