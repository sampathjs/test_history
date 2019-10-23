package com.matthey.testutil.enums;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Custom tran info fields in Endur
 * See tran_info_types table
 * @author KailsM01
 */
public enum EndurTranInfoField
{
    SAP_ORDER_ID("SAP_Order_ID"),
    SAP_METAL_TRANSFER_REQUEST_NUMBER("SAP-MTRNo"),
    FROM_ACC_LOCO("From A/C Loco"),
    FROM_ACC_FORM("From A/C Form"),
    FROM_ACC("From A/C"),
    FROM_ACC_BU("From A/C BU"),
    TO_ACC_LOCO("To A/C Loco"),
    TO_ACC_FORM("To A/C Form"),
    TO_ACC("To A/C"),
    TO_ACC_BU("To A/C BU"),
    TRADE_PRICE("Trade Price"),
    CHARGES("Charges"),
    CHARGES_IN_USD("Charge (in USD)"),
    IS_COVERAGE("IsCoverage"),
    METAL("Metal"),
    QTY("Qty"),
    UNIT("Unit");

	private int id;
	private String typeName;
	
	/**
	 * @param name
	 */
	private EndurTranInfoField(String name) 
	{
		this.typeName = name;
	}

	/**
	 * @param id
	 * @return
	 */
	public static EndurTranInfoField valueOf(int id) 
	{			
		EndurTranInfoField[] arrayOfStlDocInfo_Enum;
		
		int j = (arrayOfStlDocInfo_Enum = values()).length;
		
		for (int i = 0; i < j; i++) 
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
				PluginLog.warn(e.getMessage() + " while searching through enums");				
			}
		}
		
		throw new IllegalArgumentException("The id " + id + " is not valid for this enum or the type has not been configured");
	}

	/**
	 * @return
	 * @throws OException
	 */
	public int toInt() throws OException 
	{
        if (this.id == 0) 
        {
            try 
            {
            	String cachedTableName = "tran_info_types" ;              
                Table tblTranInfo = Table.getCachedTable(cachedTableName);              
                
                if (Table.isTableValid(tblTranInfo) != OLF_RETURN_SUCCEED.jvsValue())
                {                                       
                    Table tblTranInfoNew = Table.tableNew();                    
                    String sqlQuery = "SELECT * FROM tran_info_types";
                    
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

	/* (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	public String toString() 
	{
		return this.typeName;
	}
}