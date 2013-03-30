package test.blackscholes_slaves;

import test.BlackscholesProcess;
import mp.*;

public class BlackscholesSlave1 {

	public static void main(String[] args) {
		IProcess bp = new BlackscholesProcess();
		ShMemServer s = new ShMemServer(bp, 1);
		s.start();
	}
}
