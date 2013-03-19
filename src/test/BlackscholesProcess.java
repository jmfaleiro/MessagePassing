package test;

import java.io.BufferedReader;
import java.io.FileReader;

import org.json.simple.*;

import mp.*;

public class BlackscholesProcess implements IProcess {

	private int slave_number;
	private int num_threads;
	
	private OptionData[] data;
	
	public BlackscholesProcess(String input_file, int slave_number, int num_threads) {
		
		this.slave_number = slave_number;
		this.num_threads = num_threads;
		
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
	    		
	    		
	    		data[j] = new OptionData();

	    		data[j] = new OptionData();
	    		data[j].s = Double.parseDouble(parts[0]);
	    		data[j].strike = Double.parseDouble(parts[1]);
	    		data[j].r = Double.parseDouble(parts[2]);
	    		data[j].divq = Double.parseDouble(parts[3]);
	    		data[j].v = Double.parseDouble(parts[4]);
	    		data[j].t = Double.parseDouble(parts[5]);
	    		data[j].OptionType = parts[6].charAt(0);
	    		data[j].divs = Double.parseDouble(parts[7]);
	    		data[j].DGrefval = Double.parseDouble(parts[8]);
	    		
	    	}
	    	
	    	file.close();
    		reader.close();
	    }
	    catch(Exception e){
	    	
	    	System.out.println(e.toString());
	    	return;
	    }
	}

	@SuppressWarnings("unchecked")
	public JSONObject process(JSONArray jobj) {
		
		int start = this.slave_number * (data.length / this.num_threads);
		int end = start + (data.length / this.num_threads);
		
		JSONObject ret = new JSONObject();
		
		for (int i = start; i < end; ++i) {
			
			double price = Blackscholes.BlkSchlsEqEuroNoDiv(data[i].s, data[i].strike,
											   				data[i].r, data[i].v, data[i].t, data[i].OptionType,
											   				0);
			ret.put(i,  price);
		}
		
		return ret;
	}
}
