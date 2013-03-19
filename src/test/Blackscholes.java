package test;


import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.json.*;
import org.json.simple.*;
import org.json.simple.parser.*;

import mp.*;

public class Blackscholes {
	
	// Constants
	private static double inv_sqrt_2xPI = 0.39894228040143270286;
	private static int NUM_RUNS = 100;
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
            		double time, int otype, float timet )
	{
		double OptionPrice;
		
		// local private working variables for the calculation
		double xStockPrice;
		double xStrikePrice;
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
		
		xStockPrice = sptprice;
		xStrikePrice = strike;
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
	
	public static void runParallel(String input_file) throws InterruptedException, ParseException, ShMemFailure {
		
		ShMemServer[] slaves = new ShMemServer[nThreads];
		ShMemClient[] clients = new ShMemClient[nThreads];
		
		JSONArray arguments = new JSONArray();
		JSONObject empty_object = new JSONObject();
		arguments.add(empty_object);
		
		for (int i = 0; i < nThreads; ++i) {
			BlackscholesProcess p = new BlackscholesProcess(input_file, i, nThreads);
			slaves[i] = new ShMemServer(p, i);
			slaves[i].start();
			
			clients[i] = new ShMemClient(arguments, i);
			clients[i].fork();
		}
		
		for (int i = 0; i < nThreads; ++i) {
			clients[i].merge(arguments);
		}
		
		for (int i = 0; i < numOptions; ++i) {
			
			results[i] = (Double)(((JSONObject)arguments.get(0)).get(i));
		}
	}
	
	public static void main(String[] args) throws InterruptedException, ShMemFailure, ParseException {
		
	    nThreads = Integer.parseInt(args[0]); 
	    numOptions = Integer.parseInt(args[1]);
	    String inputFile = args[2];
	    String output_file = args[3];
	    
	    output_file = output_file + "-parallel.txt";
	   
	    Blackscholes.results = new double[numOptions];
	    runParallel(inputFile);
	    
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
