package com.matthey.utilities.enums;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;


import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Custom event info types in Endur
 * 
 * See tran_event_info_types table
 */
public enum EndurEventInfoField
{
    TAXED_EVENT_NUM("Taxed Event Num"),
    TAX_RATE_NAME("Tax Rate Name"),
    BASE_AMOUNT("Base Amount"),
    BASE_CURRENCY("Base Currency"),
    FX_RATE("FX Rate"),
    METAL_VALUE_DATE("Metal Value Date");
    
	private int id;
	private String typeName;
	
	private EndurEventInfoField(String name) 
	{
		this.typeName = name;
	}

	public static EndurEventInfoField valueOf(int id) 
	{			
		EndurEventInfoField[] arrayOfStlDocInfo_Enum;
		
		int j = (arrayOfStlDocInfo_Enum = values()).length;
		
		for (int i = 0; i < j;) 
		{
			EndurEventInfoField StlDocInfo = arrayOfStlDocInfo_Enum[i];
			
			try 
			{
				if (StlDocInfo.toInt() == id)
				{
					return StlDocInfo;
				}
			} 
			catch (Exception e) 
			{
				PluginLog.warn(e.getMessage() + " while searching through enums");				
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
            	String cachedTableName = "tran_event_info_types" ;              
                Table tblTranInfo = Table.getCachedTable(cachedTableName);              
                
                if (Table.isTableValid(tblTranInfo) != OLF_RETURN_SUCCEED.toInt())
                {                                       
                    Table tblTranInfoNew = Table.tableNew();                    
                    String sqlQuery = "SELECT * FROM tran_event_info_types ";
                    
                    int retVal = DBaseTable.execISql(tblTranInfoNew, sqlQuery); 
                    
                    if (retVal != OLF_RETURN_SUCCEED.toInt()) 
                    {
                        PluginLog.error("Error Failed to execute:\n" + sqlQuery.toString());
                        String error = DBUserTable.dbRetrieveErrorInfo(retVal, "");
                        throw new RuntimeException(error);
                    }
                    
                    Table.cacheTable(cachedTableName, tblTranInfoNew);
                    tblTranInfo = Table.getCachedTable(cachedTableName);
                }
                
                int row = tblTranInfo.unsortedFindString("type_name", this.typeName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
                
                if (row <= 0) 
                {
                    PluginLog.info("No enum has been defined for the name " + this.typeName);
                    
                    this.id = 0;    
                }
                else 
                {              
                    this.id = tblTranInfo.getInt("type_id", row);
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
	public static EndurEventInfoField fromString(String val) throws Exception 
	{
		for (EndurEventInfoField e : EndurEventInfoField.values()) 
		{
			if (e.typeName.equalsIgnoreCase(val)) 
			{
				return e;
			}
		}
		
		throw new Exception("Invalid name: " + val); 
	}
}