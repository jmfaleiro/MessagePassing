#!/usr/bin/env python
# -*- coding: utf-8 -*

'''
    Class for the testing suite :
    - get the list of all test files
    - create a copy of them on start
    - remove the copy on end
'''

import shutil
import os
import glob
import tempfile
import unittest
import subprocess

VERBOSITY = 15

clean = glob.glob('clean*')
clean.sort()
dirty = glob.glob('dirty*')
dirty.sort()

FILE_LIST = zip(clean, dirty)

try:  # PDF render processing
    import cairo
    import gi
    from gi.repository import Poppler
    import pdfrw
except ImportError:
    FILE_LIST.remove(('clean é.pdf', 'dirty é.pdf'))

try:  # python-mutagen : audio file format
    import mutagen
except ImportError:
    FILE_LIST.remove(('clean é.ogg', 'dirty é.ogg'))
    FILE_LIST.remove(('clean é.mp3', 'dirty é.mp3'))
    FILE_LIST.remove(('clean é.flac', 'dirty é.flac'))

try:  # file format exclusively managed by exiftool
    subprocess.Popen('exiftool', stdout=open('/dev/null'))
except OSError:
    pass  # None for now


class MATTest(unittest.TestCase):
    '''
        Parent class of all test-functions
    '''
    def setUp(self):
        '''
            Create working copy of the clean and the dirty file in the TMP dir
        '''
        self.file_list = []
        self.tmpdir = tempfile.mkdtemp()

        for clean, dirty in FILE_LIST:
            shutil.copy2(clean, self.tmpdir + os.sep + clean)
            shutil.copy2(dirty, self.tmpdir + os.sep + dirty)
            self.file_list.append((self.tmpdir + os.sep + clean,
                self.tmpdir + os.sep + dirty))

    def tearDown(self):
        '''
            Remove the tmp folder
        '''
        shutil.rmtree(self.tmpdir)


if __name__ == '__main__':
    import clitest
    import libtest

    SUITE = unittest.TestSuite()
    SUITE.addTests(clitest.get_tests())
    SUITE.addTests(libtest.get_tests())

    unittest.TextTestRunner(verbosity=VERBOSITY).run(SUITE)
