package com.jm.accountingfeed.enums;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Custom party info types (on internal bunit) in Endur
 * 
 * See party_info_types table
 */
public enum EndurPartyInfoInternalBunit 
{
    REGION("Region");
    private int id;
    private String typeName;
    
    private EndurPartyInfoInternalBunit(String name) 
    {
        this.typeName = name;
    }

    public static EndurPartyInfoInternalBunit valueOf(int id) 
    {           
        EndurPartyInfoInternalBunit[] enumArray;
        
        int numberOfEnums = (enumArray = values()).length;
        
        for (int i = 0; i < numberOfEnums;) 
        {
            EndurPartyInfoInternalBunit partyInfo = enumArray[i];
            
            try 
            {
                if (partyInfo.toInt() == id)
                {
                    return partyInfo;
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
                String cachedTableName = "party_info_types" ;              
                Table tblPartyInfo = Table.getCachedTable(cachedTableName);              
                
                if (Table.isTableValid(tblPartyInfo) != OLF_RETURN_SUCCEED.jvsValue())
                {                                       
                    Table tblPartyInfoNew = Table.tableNew();                    
                    String sqlQuery = "SELECT * FROM party_info_types where int_ext = 0 and  party_class = 1 ";
                    
                    int retVal = DBaseTable.execISql(tblPartyInfoNew, sqlQuery); 
                    
                    if (retVal != OLF_RETURN_SUCCEED.jvsValue()) 
                    {
                        PluginLog.error("Error Failed to execute:\n" + sqlQuery.toString());
                        String error = DBUserTable.dbRetrieveErrorInfo(retVal, "");
                        throw new RuntimeException(error);
                    }
                    
                    Table.cacheTable(cachedTableName, tblPartyInfoNew);
                    tblPartyInfo = Table.getCachedTable(cachedTableName);
                }
                
                int row = tblPartyInfo.unsortedFindString("type_name", this.typeName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
                
                if (row <= 0) 
                {
                    PluginLog.info("No enum has been defined for the name " + this.typeName);
                    
                    this.id = 0;    
                }
                else 
                {              
                    this.id = tblPartyInfo.getInt("type_id", row);
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
