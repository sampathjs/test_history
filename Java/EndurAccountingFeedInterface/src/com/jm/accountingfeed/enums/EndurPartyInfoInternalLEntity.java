package com.jm.accountingfeed.enums;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.jm.logging.Logging;

/**
 * Custom party info types (on internal legal entity) in Endur
 * 
 * See party_info_types table
 */
public enum EndurPartyInfoInternalLEntity 
{
    BASE_CURRENCY("Base Currency");
    
    private int id;
    private String typeName;
    
    private EndurPartyInfoInternalLEntity(String name) 
    {
        this.typeName = name;
    }

    public static EndurPartyInfoInternalLEntity valueOf(int id) 
    {           
        EndurPartyInfoInternalLEntity[] enumArray;
        
        int numberOfEnums = (enumArray = values()).length;
        
        for (int i = 0; i < numberOfEnums;) 
        {
            EndurPartyInfoInternalLEntity partyInfo = enumArray[i];
            
            try 
            {
                if (partyInfo.toInt() == id)
                {
                    return partyInfo;
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
                String cachedTableName = "party_info_types_internal_lentity" ;              
                Table tblPartyInfo = Table.getCachedTable(cachedTableName);              
                
                if (Table.isTableValid(tblPartyInfo) != OLF_RETURN_SUCCEED.toInt())
                {                                       
                    Table tblPartyInfoNew = Table.tableNew();                    
                    String sqlQuery = "SELECT * FROM party_info_types where int_ext = 0 and  party_class = 0 ";
                    
                    int retVal = DBaseTable.execISql(tblPartyInfoNew, sqlQuery); 
                    
                    if (retVal != OLF_RETURN_SUCCEED.toInt()) 
                    {
                        Logging.error("Error Failed to execute:\n" + sqlQuery.toString());
                        String error = DBUserTable.dbRetrieveErrorInfo(retVal, "");
                        throw new RuntimeException(error);
                    }
                    
                    Table.cacheTable(cachedTableName, tblPartyInfoNew);
                    tblPartyInfo = Table.getCachedTable(cachedTableName);
                }
                
                int row = tblPartyInfo.unsortedFindString("type_name", this.typeName, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
                
                if (row <= 0) 
                {
                    Logging.info("No enum has been defined for the name " + this.typeName);
                    
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
