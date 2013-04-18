package test;


import java.io.*;

import org.json.simple.*;
import org.json.simple.parser.*;

import mp.*;

public class Blackscholes {
	
	// Constants
	private static double inv_sqrt_2xPI = 0.39894228040143270286;

	private static int nThreads;
	public static int numOptions;
	
	// This is the shared state we care about.
	public static OptionData[] data;
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
	public static void runParallel(String input_file) throws InterruptedException, ParseException, ShMemFailure {
		
		ShMemServer[] slaves = new ShMemServer[nThreads];
		ShMem[] clients = new ShMem[nThreads];
		
		
		for (int i = 0; i < nThreads; ++i) {
			/*
			BlackscholesProcess p = new BlackscholesProcess();
			slaves[i] = new ShMemServer(p, i);
			slaves[i].start();
			*/
			ShMem.state.put("num_threads",  nThreads);
			ShMem.state.put("slave_number", i);
			clients[i] = ShMem.fork(i);
		}
		
		for (int i = 0; i < nThreads; ++i) {
			clients[i].join();
		}
		
		ShMemObject result_objs = (ShMemObject)ShMem.state.get("results");
		for (int i = 0; i < numOptions; ++i) {
			
			try {
				Object blah = result_objs.get(Integer.toString(i));
				assert blah != null;
				results[i] = (Double)blah;
			}
			catch(Exception e) {
				System.exit(-1);
			}
		}
		
	}
	
	private static void parse_options(String input_file) {
		
		JSONArray data_add = new JSONArray();
		ShMemObject results_add = new ShMemObject();
		
		//Read input data from file
	    try{
	    	
	    	FileReader reader = new FileReader("test_cases/" + input_file);
	    	@SuppressWarnings("resource")
			BufferedReader file = new BufferedReader(reader);
	    	
	    	int numOptions = Integer.parseInt(file.readLine());
	    	
	    	data = new OptionData[numOptions];
	    	
	    	for(int j = 0; j < numOptions; ++j){
	    		
	    		String line = file.readLine();
	    		String parts[] = line.split(" ");
	    		
	    		if (parts.length != 9){
	    			
	    			Exception toThrow = new Exception("Bad line in input file");
	    			throw toThrow;
	    		}
	    		
	    		JSONObject data_value =  new JSONObject();
	    		
	    		data_value.put("s",  Double.parseDouble(parts[0]));
	    		data_value.put("strike",  Double.parseDouble(parts[1]));
	    		data_value.put("r",  Double.parseDouble(parts[2]));
	    		data_value.put("divq",  Double.parseDouble(parts[3]));
	    		data_value.put("v", Double.parseDouble(parts[4]));
	    		data_value.put("t", Double.parseDouble(parts[5]));
	    		data_value.put("OptionType",  Character.toString(parts[6].charAt(0)));
	    		data_value.put("divs",  Double.parseDouble(parts[7]));
	    		data_value.put("DGrefval", Double.parseDouble(parts[8]));
	    		
	    		data_add.add(data_value);
	    	}
	    	
	    	ShMem.state.put("results",  results_add);
	    	ShMem.state.put("data",  data_add);
	    	
	    	file.close();
    		reader.close();
	    }
	    catch(Exception e){
	    	
	    	System.out.println(e.toString());
	    	System.exit(-1);
	    	
	    }
		
	}
	
	public static void main(String[] args) throws InterruptedException, ShMemFailure, ParseException {
		
	    nThreads = Integer.parseInt(args[0]); 
	    numOptions = Integer.parseInt(args[1]);
	    String input_file = args[2];
	    String output_file = args[3];
	    
	    output_file = output_file + "-parallel.txt";
	   
	    Blackscholes.results = new double[numOptions];
	    parse_options(input_file);
	    runParallel(input_file);
	    
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
