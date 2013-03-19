package test;


/*
 * Java Port of PARSEC's Blackscholes to use Deterministic
 * Parallelism in Java. 
 * 
 */

public class OptionData{
	
	public double s;				// spot price
	public double strike;			// strike price
	public double r;				// risk-free interest rate
	public double divq;				// dividend rate 
	public double v;				// volatility
	public double t;				// time to maturity or expiration in years
							// (1yr = 1.0, 6mos = 0.5 ...)
	
	public char OptionType;		// Option type. "P" = PUT, "C" = CALL
	public double divs;				// dividend vals (not used in this test)
	public double DGrefval;			// DerivaGem reference value

}
