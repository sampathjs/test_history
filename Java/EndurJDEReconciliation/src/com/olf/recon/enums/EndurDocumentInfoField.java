package com.olf.recon.enums;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.logging.PluginLog;

public enum EndurDocumentInfoField
{
    OUR_DOC_NUM("Our Doc Num"),
    VAT_INVOICE_DOC_NUM("VAT Invoice Doc Num"),
    CANCELLATION_DOC_NUM("Cancellation Doc Num"),
    CANCELLATION_VAT_NUM("Cancellation VAT Num");

	private int id;
	private String typeName;
	
	private EndurDocumentInfoField(String name) 
	{
		this.typeName = name;
	}

	public static EndurDocumentInfoField valueOf(int id) 
	{			
		EndurDocumentInfoField[] arrayOfStlDocInfo_Enum;
		
		int j = (arrayOfStlDocInfo_Enum = values()).length;
		
		for (int i = 0; i < j;) 
		{
			EndurDocumentInfoField StlDocInfo = arrayOfStlDocInfo_Enum[i];
			
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
            	String cachedTableName = "stldoc_info_types" ;              
                Table tblTranInfo = Table.getCachedTable(cachedTableName);              
                
                if (Table.isTableValid(tblTranInfo) != OLF_RETURN_SUCCEED.jvsValue())
                {                                       
                    Table tblTranInfoNew = Table.tableNew();                    
                    String sqlQuery = "SELECT * FROM stldoc_info_types ";
                    
                    int retVal = DBaseTable.execISql(tblTranInfoNew, sqlQuery); 
                    
                    if (retVal != OLF_RETURN_SUCCEED.jvsValue()) 
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
}