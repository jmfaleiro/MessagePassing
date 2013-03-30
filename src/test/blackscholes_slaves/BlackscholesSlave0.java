package test.blackscholes_slaves;

import mp.IProcess;
import mp.ShMemServer;
import test.BlackscholesProcess;

public class BlackscholesSlave0 {

	public static void main(String[] args) {
		IProcess bp = new BlackscholesProcess();
		ShMemServer s = new ShMemServer(bp, 0);
		s.start();
	}
}
