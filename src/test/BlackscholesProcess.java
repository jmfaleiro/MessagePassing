package test;

import java.io.BufferedReader;
import java.io.FileReader;

import org.json.simple.*;

import mp.*;

public class BlackscholesProcess implements IProcess {


	@SuppressWarnings("unchecked")
	public void process(){
		
		
		JSONArray data = (JSONArray)ShMem.state.get("data");
		ShMemObject results = (ShMemObject)ShMem.state.get("results");
		long num_threads = (Long)ShMem.state.get("num_threads");
		long start = (Long)ShMem.state.get("slave_number") * (data.size() / num_threads);
		long end = start + (data.size()) / num_threads;
		
		for (long i = start; i < end; ++i) {
			
			int i_usable = ((Long)i).intValue();
			
			JSONObject cur_data = (JSONObject)data.get(i_usable);
			double s = (Double)cur_data.get("s");
			double strike = (Double)cur_data.get("strike");
			double r = (Double)cur_data.get("r");
			double v = (Double)cur_data.get("v");
			double t = (Double)cur_data.get("t");
			long otype = (long)((String)cur_data.get("OptionType")).charAt(0);
			
			double price = Blackscholes.BlkSchlsEqEuroNoDiv(s, strike,
											   				r, v, t, otype,
											   				0);
			try {
				results.put(Long.toString(i),  price);
			}
			catch(Exception e) {
				System.exit(-1);
			}
		}
	}
}
