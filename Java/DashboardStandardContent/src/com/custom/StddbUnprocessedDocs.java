package com.custom;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpsDashboard;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;

public class StddbUnprocessedDocs implements IScript
{
   @Override
   public void execute(IContainerContext context) throws OException
   {
      Table argt = context.getArgumentsTable();
	  Table returnt = context.getReturnTable();
	  String allAuthorizedDefinitions = null;
	  boolean onlyStandardData = true;
	  
	  try
	  {
		 // Retrieve the event data for events not assigned to a document
		 StlDoc.retrieveUndesignatedEvents(allAuthorizedDefinitions, returnt, onlyStandardData);
	  }
	  catch(OException e)
	  {
		  OConsole.oprint("Error - " + e.getMessage());
	  }
	  //returnt.viewTable();

	  
      Util.exitSucceed();
   }
}
