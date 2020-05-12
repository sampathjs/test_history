package com.olf.jm.custom_picklists.app;

import com.olf.openjvs.*;


/*
 * History:
 * 2015-11-03	V1.0	jwaechter	-	Initial Version
 */

/**
 *
 * @author ifernandes
 * @version 1.0
 */
public class JMBasePriceRefSrcPickList implements IScript {
	@Override
	public void execute(IContainerContext context) throws OException
	{
	  /*** v17 change - Structure of return table has changed. Added check below. ***/
	  Table retValues = context.getReturnTable();
      boolean isVersionLessThan17 = retValues.getColName(1).equalsIgnoreCase("table_value");
      if (isVersionLessThan17) {
        retValues = retValues.getTable(("table_value"), 1);
      }
		
		
		for(int i = retValues.getNumRows();i>0;i--){
			
			String strValue = retValues.getString("label", i);
			                          
			if( (Str.equal(strValue, "JM London Opening") != 1)
			  && (Str.equal(strValue, "JM HK Opening") != 1)
			  && (Str.equal(strValue, "JM HK Closing") !=1)
			  && (Str.equal(strValue, "JM NY Opening") != 1)
			  && (Str.equal(strValue, "LME AM") != 1)
  			  && (Str.equal(strValue, "LME PM") != 1)
  			  && (Str.equal(strValue, "LBMA AM") != 1)
			  && (Str.equal(strValue, "LBMA Silver") != 1)
			  && (Str.equal(strValue, "LBMA PM") != 1)
					
					){
				retValues.delRow(i);
			}

			
		}
		
		
	}	
}
