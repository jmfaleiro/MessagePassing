package apps.Blackscholes;


import java.io.*;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import mp.*;

public class Blackscholes {
	
	// Constants
	private static double inv_sqrt_2xPI = 0.39894228040143270286;

	private static int nThreads;
	public static int numOptions;
	
	// This is the shared state we care about.
	
	public static double[] results;
	
	// Some thread local state that gets replicated by the fork.
	// We don't care about their values, they are just 
	// used during the computation itself.
	public static int otype;
	public static double sptprice;
	public static double strike;
	public static double rate;
	public static double volatility;
	public static double otime;
	

	public static double CNDF ( double InputX ) 
	{
	    int sign;

	    double OutputX;
	    double xInput;
	    double xNPrimeofX;
	    double expValues;
	    double xK2;
	    double xK2_2, xK2_3;
	    double xK2_4, xK2_5;
	    double xLocal, xLocal_1;
	    double xLocal_2, xLocal_3;

	    // Check for negative value of InputX
	    if (InputX < 0.0) {
	        InputX = -InputX;
	        sign = 1;
	    } else 
	        sign = 0;

	    xInput = InputX;
	 
	    // Compute NPrimeX term common to both four & six decimal accuracy calcs
	    expValues = Math.exp(-0.5f * InputX * InputX);
	    xNPrimeofX = expValues;
	    xNPrimeofX = xNPrimeofX * inv_sqrt_2xPI;

	    xK2 = 0.2316419 * xInput;
	    xK2 = 1.0 + xK2;
	    xK2 = 1.0 / xK2;
	    xK2_2 = xK2 * xK2;
	    xK2_3 = xK2_2 * xK2;
	    xK2_4 = xK2_3 * xK2;
	    xK2_5 = xK2_4 * xK2;
	    
	    xLocal_1 = xK2 * 0.319381530;
	    xLocal_2 = xK2_2 * (-0.356563782);
	    xLocal_3 = xK2_3 * 1.781477937;
	    xLocal_2 = xLocal_2 + xLocal_3;
	    xLocal_3 = xK2_4 * (-1.821255978);
	    xLocal_2 = xLocal_2 + xLocal_3;
	    xLocal_3 = xK2_5 * 1.330274429;
	    xLocal_2 = xLocal_2 + xLocal_3;

	    xLocal_1 = xLocal_2 + xLocal_1;
	    xLocal   = xLocal_1 * xNPrimeofX;
	    xLocal   = 1.0 - xLocal;

	    OutputX  = xLocal;
	    
	    if (sign != 0) {
	        OutputX = 1.0 - OutputX;
	    }
	    
	    return OutputX;
	} 
	
	public static double BlkSchlsEqEuroNoDiv( double sptprice,
            		double strike, double rate, double volatility,
            		double time, long otype, float timet )
	{
		double OptionPrice;
		
		// local private working variables for the calculation
		
		double xRiskFreeRate;
		double xVolatility;
		double xTime;
		double xSqrtTime;
		
		double logValues;
		double xLogTerm;
		double xD1; 
		double xD2;
		double xPowerTerm;
		double xDen;
		double d1;
		double d2;
		double FutureValueX;
		double NofXd1;
		double NofXd2;
		double NegNofXd1;
		double NegNofXd2;    
		
		
		xRiskFreeRate = rate;
		xVolatility = volatility;
		
		xTime = time;
		xSqrtTime = Math.sqrt(xTime);
		
		logValues = Math.log( sptprice / strike );
		
		xLogTerm = logValues;
		
		
		xPowerTerm = xVolatility * xVolatility;
		xPowerTerm = xPowerTerm * 0.5;
		
		xD1 = xRiskFreeRate + xPowerTerm;
		xD1 = xD1 * xTime;
		xD1 = xD1 + xLogTerm;
		
		xDen = xVolatility * xSqrtTime;
		xD1 = xD1 / xDen;
		xD2 = xD1 -  xDen;
		
		d1 = xD1;
		d2 = xD2;
		
		NofXd1 = CNDF( d1 );
		NofXd2 = CNDF( d2 );
		
		FutureValueX = strike * ( Math.exp( -(rate)*(time) ) );        
		if (otype == 0) {            
			OptionPrice = (sptprice * NofXd1) - (FutureValueX * NofXd2);
		} else { 
			NegNofXd1 = (1.0 - NofXd1);
			NegNofXd2 = (1.0 - NofXd2);
			OptionPrice = (FutureValueX * NegNofXd2) - (sptprice * NegNofXd1);
		}
		
		return OptionPrice;
	}
	
	@SuppressWarnings("unchecked")
	public static void runParallel(String input_file, int node_id, int total_nodes) throws InterruptedException {
		if (node_id == 0) {
			
			parse_options(ShMem.s_state, input_file);
			ShMem.s_state.put("num_threads",  total_nodes-1);
			for (int i = 1; i < total_nodes; ++i) {
				ShMem.Release(i);
			}
			for (int i = 1; i < total_nodes; ++i) {
				try {
					ShMem.Acquire(i);
				}
				catch (ShMemObject.MergeException e) {
					System.out.println("Merge failed!");
					System.exit(-1);
				}
			}
			
			ShMemObject result_objs = (ShMemObject)ShMem.s_state.get("results");
			for (int i = 0; i < numOptions; ++i) {
				if (i == numOptions - 1) {
					System.out.println("done!");
				}
				double value = result_objs.get(Integer.toString(i)).getDoubleValue();
				results[i] = value;
			}
		}
		else {
			try {
				ShMem.Acquire(0);
			}
			catch (ShMemObject.MergeException e) {
				System.out.println("Merge failed!");
				System.exit(-1);
			}
			process(node_id);
			ShMem.Release(0);
		}
	}
	
	private static void process(int node_number) {
		ArrayNode data = (ArrayNode)ShMem.s_state.get("data");
		ShMemObject results = (ShMemObject)ShMem.s_state.get("results");
		long num_threads = ShMem.s_state.get("num_threads").getLongValue();
		long start = ((long)node_number - 1) * (data.size() / num_threads);
		long end = start + (data.size()) / num_threads;
		
		for (long i = start; i < end; ++i) {
			
			int i_usable = ((Long)i).intValue();
			
			JsonNode cur_data = data.get(i_usable);
			double s = (Double)cur_data.get("s").getDoubleValue();
			double strike = (Double)cur_data.get("strike").getDoubleValue();
			double r = (Double)cur_data.get("r").getDoubleValue();
			double v = (Double)cur_data.get("v").getDoubleValue();
			double t = (Double)cur_data.get("t").getDoubleValue();
			long otype = cur_data.get("OptionType").getTextValue().charAt(0);
			
			double price = Blackscholes.BlkSchlsEqEuroNoDiv(s, strike,
											   				r, v, t, otype,
											   				0);
			
			String to_put = Integer.toString(i_usable);
			results.put(to_put,  price);
		}
	}
	
	private static void parse_options(ShMemObject memory, String input_file) {
		
		ArrayNode data_add = ShMem.mapper.createArrayNode();
		ShMemObject results_add = new ShMemObject();
		
		//Read input data from file
	    try{
	    	
	    	FileReader reader = new FileReader("test_cases/" + input_file);
	    	@SuppressWarnings("resource")
			BufferedReader file = new BufferedReader(reader);
	    	
	    	int numOptions = Integer.parseInt(file.readLine());
	    	
	    	for(int j = 0; j < numOptions; ++j){
	    		
	    		String line = file.readLine();
	    		String parts[] = line.split(" ");
	    		
	    		if (parts.length != 9){
	    			
	    			Exception toThrow = new Exception("Bad line in input file");
	    			throw toThrow;
	    		}
	    		
	    		ObjectNode data_value =  ShMem.mapper.createObjectNode();
	    		
	    		data_value.put("s",  Double.parseDouble(parts[0]));
	    		data_value.put("strike",  Double.parseDouble(parts[1]));
	    		data_value.put("r",  Double.parseDouble(parts[2]));
	    		data_value.put("v", Double.parseDouble(parts[4]));
	    		data_value.put("t", Double.parseDouble(parts[5]));
	    		data_value.put("OptionType",  Character.toString(parts[6].charAt(0)));
	    		data_value.put("divs",  Double.parseDouble(parts[7]));
	    		data_value.put("DGrefval", Double.parseDouble(parts[8]));
	    		
	    		data_add.add(data_value);
	    	}
	    	
	    	memory.put("results",  results_add);
	    	memory.put("data",  data_add);
	    	
	    	file.close();
    		reader.close();
	    }
	    catch(Exception e){
	    	
	    	System.out.println(e.toString());
	    	System.exit(-1);
	    	
	    }
		
	}
	
	public static void main(String[] args) throws InterruptedException, ShMemFailure {
		
	    nThreads = Integer.parseInt(args[0]); 
	    numOptions = Integer.parseInt(args[1]);
	    int node_id = Integer.parseInt(args[2]);
	    String input_file = args[3];
	    String output_file = args[4];
	    
	    output_file = output_file + "-parallel.txt";
	   
	    Blackscholes.results = new double[numOptions];
	    
	    ShMem.Init(node_id);
	    ShMem.Start();
	    runParallel(input_file, node_id, nThreads);
	    
	    if (node_id == 0) {
		    try{
		    	FileWriter file = new FileWriter(output_file);
		    	BufferedWriter writer = new BufferedWriter(file);
		    	
		    	
		    	for(int i= 0; i < numOptions; ++i){
		    		
		    		String toWrite = Double.toString(results[i]);
		    		writer.write(toWrite);
		    		writer.write("\n");
		    	}
		    	
		    	writer.close();
		    	file.close();
		    	
		    }
		    catch(Exception e){
		    	
		    	System.out.println(e.toString());
		    	return;
		    }
	    }
	}
}
