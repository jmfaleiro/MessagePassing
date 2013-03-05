package mp;

import java.util.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.*;

import org.json.simple.*;

import java.nio.*;


import java.nio.*;
public class Registration {

	
		private static Class[] toRegister = { byte[].class, JSONObject.class };
		
		public static void registerClasses(Kryo k){
			
			for(int i= 0; i < toRegister.length; ++i)
				k.register(toRegister[i]);
			
		}
}
