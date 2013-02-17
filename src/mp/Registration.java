package mp;

import java.util.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.*;

import org.json.simple.*;


public class Registration {

	
		private static Class[] toRegister = { JSONObject.class,  String.class };
		
		public static void registerClasses(Kryo k){
			
			for(int i= 0; i < toRegister.length; ++i)
				k.register(toRegister[i]);
			
		}
}
