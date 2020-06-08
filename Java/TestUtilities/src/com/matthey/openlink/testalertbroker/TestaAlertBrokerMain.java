package com.matthey.openlink.testalertbroker;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;


public class TestaAlertBrokerMain implements IScript {
//	 private TestLoggingSwapMethods data = new TestLoggingSwapMethods();
	public ConstRepository constRep;

	@Override
	public void execute(IContainerContext context) throws OException {
		 
		try {

			// Empty method - Just needed to allow the param script to work
			
		} catch (Exception e) {
			OConsole.print("Ooopsie Error: \n" + e.getLocalizedMessage() + e.getStackTrace());
		}
		
	}

	
	 

	  


	  


	 

	   
	

	
}
