/* Released with version 12-Dec-2013_V14_0_3 of APM */

package com.olf.result.APM_PhysVolumes;

import java.util.Vector;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.result.APM_PhysVolumes.APM_PhysVolumesUtilNAGas;


public class APM_PhysInfoNAGas implements IScript 
{
	@Override
	public void execute(IContainerContext arg0) throws OException 
	{
      Table argt = Util.NULL_TABLE, returnt = Util.NULL_TABLE;

      int intOperation;

      argt = arg0.getArgumentsTable();
      returnt = arg0.getReturnTable();

      // Call the virtual functions according to action type
      intOperation = argt.getInt("operation", 1);

      if (intOperation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt()) 
      {
         compute_result(argt, returnt);
      } 
      else if (intOperation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt()) 
      {
         format_result(returnt);
      }
   }

   public void compute_result(Table argt, Table returnt)
         throws OException 
   {
	  Table tblData = Table.tableNew();
	  
      // Process parameters    
      
      Vector<Integer> allowedInsTypes = new Vector<Integer>();               
      
      // Generate a query on all transactions - instrument types list is empty means
      // all are allowed
      int iQueryID = APM_PhysVolumesUtilNAGas.generateQuery(argt, allowedInsTypes);
      
      if (iQueryID > 0)
      {
    	  tblData = APM_PhysVolumesUtilNAGas.retrieveInfo(iQueryID);
      }
      
      returnt.select(tblData, "DISTINCT, *", "deal_num GE 0");
   }  

   public void format_result(Table returnt) throws OException 
   {
	   APM_PhysVolumesUtilNAGas.formatResult(returnt);
   }
}
