/********************************************************************************

 * Script Name: EndurAccountInfoField
 *
 * 
 * Revision History:
 * Version Date       Author      Description
 * 1.0     14-Apr-20  Jyotsna	  Initial Version - Developed as part of SR 323601 
 ********************************************************************************/
package com.matthey.utilities.enums;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import  com.olf.jm.logging.Logging;

public enum EndurAccountInfoField {
	
	FORM_TYPE("Form");
	private int id;
	private String typeName;
	
	private EndurAccountInfoField(String name) 
	{
		this.typeName = name;
	}

	public static EndurAccountInfoField valueOf(int id) 
	{			
		EndurAccountInfoField[] arrayOfAccInfo_Enum;
		
		int j = (arrayOfAccInfo_Enum = values()).length;
		
		for (int i = 0; i < j;) 
		{
			EndurAccountInfoField AccInfo = arrayOfAccInfo_Enum[i];
			
			try 
			{
				if (AccInfo.toInt() == id)
				{
					return AccInfo;
				}
			} 
			catch (Exception e) 
			{
				Logging.warn(e.getMessage() + " while searching through enums");				
			}
			
			i++;
		}
		
		throw new IllegalArgumentException("The id " + id + " is not valid for this enum or the type has not been configured");
	}

	public int toInt() throws OException 
	{
        if (this.id == 0) 
        {
            try 
            {
            	String cachedTableName = "account_info_type" ;              
                Table tblAccInfo = Table.getCachedTable(cachedTableName);              
                
                if (Table.isTableValid(tblAccInfo) != OLF_RETURN_SUCCEED.toInt())
                {                                       
                    Table tblAccInfoNew = Table.tableNew();                    
                    String sqlQuery = "SELECT * FROM account_info_type ";
                    
                    int retVal = DBaseTable.execISql(tblAccInfoNew, sqlQuery); 
                    
                    if (retVal != OLF_RETURN_SUCCEED.toInt()) 
                    {
                        Logging.error("Error Failed to execute:\n" + sqlQuery.toString());
                        String error = DBUserTable.dbRetrieveErrorInfo(retVal, "");
                        throw new RuntimeException(error);
                    }
                    
                    Table.cacheTable(cachedTableName, tblAccInfoNew);
                    tblAccInfo = Table.getCachedTable(cachedTableName);
                }
                
                int row = tblAccInfo.unsortedFindString("type_name", this.typeName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
                
                if (row <= 0) 
                {
                    Logging.info("No enum has been defined for the name " + this.typeName);
                    
                    this.id = 0;    
                }
                else 
                {              
                    this.id = tblAccInfo.getInt("type_id", row);
                }    
            } 
            catch (OException e) 
            {
                throw new RuntimeException("No enum has been defined for the name " + this.typeName);
            }
        }
        return this.id;
    }

	public String toString() 
	{
		return this.typeName;
	}
	public static EndurAccountInfoField fromString(String val) throws Exception 
	{
		for (EndurAccountInfoField e : EndurAccountInfoField.values()) 
		{
			if (e.typeName.equalsIgnoreCase(val)) 
			{
				return e;
			}
		}
		
		throw new Exception("Invalid name: " + val); 
	}

}
