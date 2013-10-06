import sys

# Import ShMem library specific classes. 
sys.path.append('../../../mp/python')
from ShMem import *
from ShMemObject import *
import math


class Blackscholes:
    

    
    # Read all data from the input file. 
    @staticmethod
    def parseOptions(state, input_file):
        data_add = []
        results_add = ShMemObject()
        
        input_file = open(input_file, "r")
        for line in input_file:
            parts = line.split(' ')
            if len(parts) < 5:
                continue

            data_value = {}

            data_value['s'] = float(parts[0])
            data_value['strike'] = float(parts[1])
            data_value['r'] = float(parts[2])
            data_value['v'] = float(parts[4])
            data_value['t'] = float(parts[5])
            data_value['OptionType'] = str(parts[6][0])
            data_value['divs'] = float(parts[7])
            data_value['DGrefval'] = float(parts[8])

            data_add.append(data_value)

        state.put_simple('data', data_add)
        state.put_object('results', results_add)
        input_file.close()

    @staticmethod
    def CNDF(InputX):

        # Check for negative value of InputX
        if InputX < 0.0:
            InputX = -InputX
            sign = 1
        else: 
            sign = 0

        xInput = InputX
	 
	# Compute NPrimeX term common to both four & six decimal accuracy calcs
        expValues = math.exp(-0.5 * InputX * InputX)
        xNPrimeofX = expValues
        xNPrimeofX = xNPrimeofX * Blackscholes.inv_sqrt_2xPI

        xK2 = 0.2316419 * xInput
        xK2 = 1.0 + xK2
        xK2 = 1.0 / xK2
        xK2_2 = xK2 * xK2
        xK2_3 = xK2_2 * xK2
        xK2_4 = xK2_3 * xK2
        xK2_5 = xK2_4 * xK2
	    
        xLocal_1 = xK2 * 0.319381530
        xLocal_2 = xK2_2 * (-0.356563782)
        xLocal_3 = xK2_3 * 1.781477937
        xLocal_2 = xLocal_2 + xLocal_3
        xLocal_3 = xK2_4 * (-1.821255978)
        xLocal_2 = xLocal_2 + xLocal_3
        xLocal_3 = xK2_5 * 1.330274429
        xLocal_2 = xLocal_2 + xLocal_3

        xLocal_1 = xLocal_2 + xLocal_1
        xLocal   = xLocal_1 * xNPrimeofX
        xLocal   = 1.0 - xLocal

        OutputX  = xLocal
	    
        if sign != 0:
            OutputX = 1.0 - OutputX	    
        return OutputX;

    @staticmethod
    def BlkSchlsEqEuroNoDiv(sptprice,
                            strike,
                            rate,
                            volatility,
                            time, 
                            otype,
                            timet):
		
        xRiskFreeRate = rate
        xVolatility = volatility
        xTime = time
        xSqrtTime = math.sqrt(xTime)        
        logValues = math.log( sptprice / strike )        
        xLogTerm = logValues        
        xPowerTerm = xVolatility * xVolatility
        xPowerTerm = xPowerTerm * 0.5		
        xD1 = xRiskFreeRate + xPowerTerm
        xD1 = xD1 * xTime
        xD1 = xD1 + xLogTerm        
        xDen = xVolatility * xSqrtTime
        xD1 = xD1 / xDen
        xD2 = xD1 -  xDen        
        d1 = xD1
        d2 = xD2        
        NofXd1 = Blackscholes.CNDF( d1 )
        NofXd2 = Blackscholes.CNDF( d2 )        
        FutureValueX = strike * ( math.exp( -(rate)*(time) ) )
        if otype == 0:
            OptionPrice = (sptprice * NofXd1) - (FutureValueX * NofXd2)
	else:
            NegNofXd1 = (1.0 - NofXd1)
            NegNofXd2 = (1.0 - NofXd2)
            OptionPrice = (FutureValueX * NegNofXd2) - (sptprice * NegNofXd1)
        return OptionPrice
        

    @staticmethod
    def process(node_number):
        data = ShMem.s_state.get('data')
        results = ShMem.s_state.get('results')
        num_threads = ShMem.s_state.get('num_threads')
        start = (node_number - 1) * (len(data) / num_threads)
        end = start + len(data) / num_threads
        
        for i in range(start, end):
            cur_data = data[i]
            s = cur_data['s']
            strike = cur_data['strike']
            r = cur_data['r']
            v = cur_data['v']
            t = cur_data['t']
            otype = ord(cur_data['OptionType'][0])
            
            price = Blackscholes.BlkSchlsEqEuroNoDiv(s, strike, r, v, t, otype,
                                                     0)
            to_put = str(i)
            results.put_simple(str(i), price)
            

    @staticmethod
    def runParallel(input_file, node_id, total_nodes):
        if node_id == 0:
            Blackscholes.parseOptions(ShMem.s_state, input_file)
            ShMem.s_state.put_simple('num_threads', total_nodes-1)

            # Dispatch work to worker processes. 
            for i in range(1, total_nodes):
                ShMem.Release(i)
                
            # Wait for everyone to finish. 
            for i in range(1, total_nodes):
                ShMem.Acquire(i)

            result_objs = ShMem.s_state.get('results')
            print result_objs
            for i in range(i, Blackscholes.s_numOptions):
                print result_objs.get(str(i))
                Blackscholes.s_values.append(result_objs.get(str(i)))
        else:
            # Get the state and range from the master. 
            ShMem.Acquire(0)
            print ShMem.s_state
            Blackscholes.process(node_id)
    
            # Send results to master. 
            ShMem.Release(0)
            while 1:pass

    @staticmethod
    def writeResults():
        output_file = open('output.txt', 'w')
        for result in Blackscholes.s_values:
            output_file.write(str(result) + '\n')
            

def main():
    
    # Read arguments from sys.argv
    Blackscholes.s_node_id = int(sys.argv[1])
    Blackscholes.s_nThreads = int(sys.argv[2])
    Blackscholes.s_numOptions = int(sys.argv[3])
    Blackscholes.s_values = []
    Blackscholes.inv_sqrt_2xPI = 0.39894228040143270286
    input_file = open(sys.argv[4], 'r')
    ShMem.init(input_file, Blackscholes.s_node_id)
    input_file.close()
    ShMem.start()
    Blackscholes.runParallel(sys.argv[5], 
                             Blackscholes.s_node_id,
                             Blackscholes.s_nThreads)
    
if __name__ == '__main__':
    main()
    
