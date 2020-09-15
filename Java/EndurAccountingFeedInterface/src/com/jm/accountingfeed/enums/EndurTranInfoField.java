package com.jm.accountingfeed.enums;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.jm.logging.Logging;

/**
 * Custom tran info fields in Endur
 * 
 * See tran_info_types table
 */
public enum EndurTranInfoField
{
    IS_COVERAGE("IsCoverage"),
    LOCATION("Loco"),
    TRADE_PRICE("Trade Price"),
    TRADE_TYPE("JM_Transaction_Id"),
    GENERAL_LEDGER("General Ledger"),
    METAL_LEDGER("Metal Ledger"),
    SAP_ORDER_ID("SAP_Order_ID"),
    PRICING_TYPE("Pricing Type");

	private int id;
	private String typeName;
	
	private EndurTranInfoField(String name) 
	{
		this.typeName = name;
	}

	public static EndurTranInfoField valueOf(int id) 
	{			
		EndurTranInfoField[] arrayOfStlDocInfo_Enum;
		
		int j = (arrayOfStlDocInfo_Enum = values()).length;
		
		for (int i = 0; i < j;) 
		{
			EndurTranInfoField StlDocInfo = arrayOfStlDocInfo_Enum[i];
			
			try 
			{
				if (StlDocInfo.toInt() == id)
				{
					return StlDocInfo;
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
            	String cachedTableName = "tran_info_types" ;              
                Table tblTranInfo = Table.getCachedTable(cachedTableName);              
                
                if (Table.isTableValid(tblTranInfo) != OLF_RETURN_SUCCEED.toInt())
                {                                       
                    Table tblTranInfoNew = Table.tableNew();                    
                    String sqlQuery = "SELECT * FROM tran_info_types";
                    
                    int retVal = DBaseTable.execISql(tblTranInfoNew, sqlQuery); 
                    
                    if (retVal != OLF_RETURN_SUCCEED.toInt()) 
                    {
                        Logging.error("Error Failed to execute:\n" + sqlQuery.toString());
                        String error = DBUserTable.dbRetrieveErrorInfo(retVal, "");
                        throw new RuntimeException(error);
                    }
                    
                    Table.cacheTable(cachedTableName, tblTranInfoNew);
                    tblTranInfo = Table.getCachedTable(cachedTableName);
                }
                
                int row = tblTranInfo.unsortedFindString("type_name", this.typeName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
                
                if (row <= 0) 
                {
                    Logging.info("No enum has been defined for the name " + this.typeName);
                    
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