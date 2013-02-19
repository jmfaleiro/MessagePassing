package test;


import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.json.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


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
	
	private static class BSThread implements Runnable {

		private int id;
		public BSThread(int given_id){
			
			id = given_id;
		}
		
		
		public void run() {
			
			try{
				Blackscholes.driver(id);
			}
			catch(mp.MessageFailure e){
				
				System.out.println(e.toString());
			}
		}
		
		
	};
	
	public static void driver(int id) throws mp.MessageFailure{
		
		double price;
		int start = id * (numOptions / nThreads);
		int end = start + (numOptions / nThreads);
		
		mp.Node n = new mp.Node(nThreads, id);
		
		// We treat id 0 as the leader. All other nodes send their results
		// to this node. 
		
		JSONObject p = new JSONObject();
		for(int i = start; i < end; ++i){
					
			price = BlkSchlsEqEuroNoDiv(data[i].s, data[i].strike,
										data[i].r, data[i].v, data[i].t, data[i].OptionType,
							  			0);
			
			if (id != 0){
				
				p.put(i, price);
			}
			
			else {
				
				results[i] = price;
			}
		}
		// If you're a slave, send your results to the master.
		if (id != 0){
			
			n.sendMessage(0,  p);
		}
		// Wait for the results of each slave and incorporate them in
		// in the results array.
		else {
			
			for(int i = 1; i < nThreads; ++i){
				
				JSONObject r = n.blockingReceive(i);
				JSONObject message = (JSONObject)r.get("message");
				
				int start_i = i * (numOptions/nThreads);
				int end_i = start_i + (numOptions/nThreads);
				
				try{
					for(; start_i < end_i; ++start_i)
						results[start_i] = (java.lang.Double) message.get(start_i);
				}
				catch(Exception e){
					System.out.println("blah");
				}
			}
		}
	}
	
	
	public static void runSerial(){
		
		double price;
		for(int j = 0; j < numOptions; ++j){
			
				
			price = BlkSchlsEqEuroNoDiv(data[j].s, data[j].strike,
				  		data[j].r, data[j].v, data[j].t, data[j].OptionType,
				  		0);
			results[j] = price;
		
		}
	}
	
	
	
	public static void runParallel() throws InterruptedException{
		
		Thread threads[] = new Thread[nThreads];
		
		mp.Leader server = new mp.Leader(nThreads);
		
		for(int i = 0; i < nThreads; ++i){
			
			threads[i] = new Thread(new BSThread(i));
			threads[i].start();
		}
		
		threads[0].join();
	}
	
	public static void main(String[] args) throws InterruptedException{
		
	    nThreads = Integer.parseInt(args[0]); 
	    String inputFile = args[1];
	    String outputFile = args[2];
	    boolean serial = false;
	    
	    if(args[3].startsWith("serial")){
	    	
	    	serial = true;
	    }
	    
	    if(serial){
	    	outputFile = outputFile + "-serial.txt";
	    }
	    else{
	    	outputFile = outputFile + "-parallel.txt";
	    }
	   
	    //Read input data from file
	    
	    try{
	    	
	    	FileReader reader = new FileReader(inputFile);
	    	BufferedReader file = new BufferedReader(reader);
	    	
	    	String[] fields = {"s", "strike", "r", "divq", "v", "t", 
	    						"OptionType", "divs", "DGrefval" };
	    	
	    	
	    	numOptions = Integer.parseInt(file.readLine());
	    	
	    	Blackscholes.data = new OptionData[numOptions];
	    	
	    	
	    	for(int j = 0; j < numOptions; ++j){
	    		
	    		String line = file.readLine();
	    		String parts[] = line.split(" ");
	    		
	    		if (parts.length != 9){
	    			
	    			
	    			Exception toThrow = new Exception("Bad line in input file");
	    			throw toThrow;
	    		}
	    		
	    		
	    		Blackscholes.data[j] = new OptionData();

	    		Blackscholes.data[j] = new OptionData();
	    		Blackscholes.data[j].s = Double.parseDouble(parts[0]);
	    		Blackscholes.data[j].strike = Double.parseDouble(parts[1]);
	    		Blackscholes.data[j].r = Double.parseDouble(parts[2]);
	    		Blackscholes.data[j].divq = Double.parseDouble(parts[3]);
	    		Blackscholes.data[j].v = Double.parseDouble(parts[4]);
	    		Blackscholes.data[j].t = Double.parseDouble(parts[5]);
	    		Blackscholes.data[j].OptionType = parts[6].charAt(0);
	    		Blackscholes.data[j].divs = Double.parseDouble(parts[7]);
	    		Blackscholes.data[j].DGrefval = Double.parseDouble(parts[8]);
	    		
	    	}
	    	file.close();
    		reader.close();
	    }
	    catch(Exception e){
	    	
	    	System.out.println(e.toString());
	    	return;
	    }
	    
	    Blackscholes.results = new double[numOptions];
	    
	    if (serial){
	    	
	    	runSerial();
	    }
	    else{
	    	
	    	runParallel();
	    }
	    
	    
	    try{
	    	FileWriter file = new FileWriter(outputFile);
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
