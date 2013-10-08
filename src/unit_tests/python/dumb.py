import sys
sys.path.append('../../mp/python')
from ShMem import *
from ShMemObject import *


def main():
    node_num = int(sys.argv[1])    
    address_file = open('test_file.txt', 'r')

    ShMem.init(address_file, node_num)
    ShMem.start()

    address_file.close()
    if node_num == 0:
        my_file = open('test.py', 'r')
        file_contents = my_file.read()
        my_file.close()

        inner_obj = ShMemObject()
        inner_obj.put_simple('file', file_contents)

        ShMem.s_state.put_object('test', inner_obj)        
        ShMem.Release(1)
        ShMem.Acquire(1)
        
        new_file = open('blah.txt', 'w')
        new_file.write(ShMem.s_state.get('test').get('file'))
        new_python_file = open('new_python.py', 'w')
        new_python_file.write(ShMem.s_state.get('python').get('python'))

    else:
        other_file = open('test_file.txt', 'r')
        other_file_contents = other_file.read()
        other_file.close()

        python_file = open('test.py', 'r')
        python_file_contents = python_file.read()
        python_file.close()

        ShMem.Acquire(0)
        temp = ShMem.s_state.get('test')
        temp.put_simple('file', other_file_contents)

        inner_other = ShMemObject()
        inner_other.put_simple('python', python_file_contents)
        ShMem.s_state.put_object('python', inner_other)

        ShMem.Release(0)
        while 1:pass
        

        

if __name__ == '__main__':
    main()
