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

        my_file = open('test_file.txt', 'r')
        txt_contents = my_file.read()
        my_file.close()
        
        inner_obj = ShMemObject()
        inner_obj.put_simple('python_file', file_contents)
        inner_obj.put_simple('text_file', txt_contents)

        ShMem.s_state.put_object('test', inner_obj)        
        ShMem.Release(1)
        ShMem.Release(2)

        ShMem.Acquire(1)

        print '\n\n\n'
        print ShMem.s_state.get_diffs([0,0,0,0])

        ShMem.Acquire(2)
        
        print ShMemObject.s_now
        print '\n\n\n'
        print ShMem.s_state.get_diffs([0,0,0,0])
        output_py = open('output.py', 'w')
        output_py.write(ShMem.s_state.get('test').get('text_file'))
        output_py.close()
        
        output_txt = open('output.txt', 'w')
        output_txt.write(ShMem.s_state.get('test').get('python_file'))
        output_txt.close()
                        
        
    elif node_num == 1:

        ShMem.Acquire(0)
        temp = ShMem.s_state.get('test')
        python_contents = temp.get('python_file')
        temp.put_simple('text_file', 'text')
        ShMem.Release(0)
        while 1:pass

    elif node_num == 2:
        ShMem.Acquire(0)
        temp = ShMem.s_state.get('test')
        text_contents = temp.get('text_file')
        temp.put_simple('python_file', 'python')
        ShMem.Release(0)
        while 1:pass
        

        

if __name__ == '__main__':
    main()
