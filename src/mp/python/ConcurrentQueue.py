from threading import *

class ConcurrentQueue:
    
    def __init__(self):
        self.m_data = []
        queue_lock = Lock()
        self.m_condition = Condition(queue_lock)
        

    def enqueue(item):
        self.m_condition.acquire()	# Acquire the lock
        self.m_data.append(item)	# Put the item into the queue
        self.m_condition.notify()	# Wake up consumer
        self.m_condition.release()	# Release the lock

    def dequeue():
        self.m_condition.acquire()      # Acquire the lock
        
        while len(self.m_data) == 0:	# Wait until the queue is not empty
            self.m_condition.wait()
        
        ret = self.m_data.pop()		# Dequeue an item
        self.m_condition.release()	# Release the lock
        return ret
