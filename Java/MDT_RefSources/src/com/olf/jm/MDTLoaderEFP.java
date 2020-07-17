package com.olf.jm;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import com.olf.jm.logging.Logging;

/*
 * Version History
 * 1.0 - initial EPI-1323
 */
public class MDTLoaderEFP implements IScript {

	public void execute(IContainerContext context) throws OException
	{
		setUpLog();
		
		Logging.debug("START MDTLoaderEFP param");
		
			
		Logging.info("Importing prices for EFP on " + OCalendar.formatJd(OCalendar.today()));

	    String loadGroup1;
		
	    loadGroup1 = "EFP";
	
	    Table argt = context.getArgumentsTable();
	
	    argt.addCol("args", COL_TYPE_ENUM.COL_STRING);
	
	    argt.addRow();
	    argt.setString(1, 1, loadGroup1);
			
			
		Logging.debug("END MDTLoaderEFP param");
	
	}
	
	


	private void setUpLog() throws OException {
		try {
			try {
				Logging.init(this.getClass(), "MiddleOffice", "MDT_RefSources");
			} catch (Exception e) {
	        	throw new OException("Failed to initialize Logging");
			}			
		} catch (OException ex) {
			throw new RuntimeException ("Error initializing the ConstRepo", ex);
		}
		
	}
	
}
