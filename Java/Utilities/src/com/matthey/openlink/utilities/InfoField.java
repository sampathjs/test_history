package com.matthey.openlink.utilities; 

import java.util.Date;


import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.application.Application;
import com.olf.openrisk.trading.EnumToolset;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.Logger;


/**
 * Helper to determine validity of Info fields 
 * @version $Revision: $
 */
public class InfoField {
    public static final int ALL_TOOLSETS = -1;
    
    public enum INFOTYPE {
    	TRANINFO(0),
    	PARAMINFO(1),
    	DOCINFO(42);
    	
    	
    	private final int type;
    	INFOTYPE(int type) {
    		this.type =  type;
    	}
    	
    	public static INFOTYPE getFromInt(int typeValue) {
    		for(INFOTYPE type:INFOTYPE.values()){
    			if (type.type == typeValue)
    				return type;
    		}
    		throw new InfoFieldException(String.format("Invalid argument(%d) for InfoField Type",typeValue));
    	}
    }

    static Date lastUpdated;
    
    private final String name;
    private final int id;
    private final INFOTYPE type;
    private int []toolsets;
    
    public InfoField(String name, int id, INFOTYPE type, Date lastUpdate) {
        this.name = name;
        this.id = id;
        this.type = type;
        InfoField.lastUpdated = lastUpdate;
    }
    
    public InfoField(String name, int[] toolsets, int id, INFOTYPE type, Date lastUpdate) {
        this(name, id, type, lastUpdate);
        this.toolsets = toolsets;
    }

    /**
     * 
     * @return the OLF datatype configured for this InfoField
     */
    public int getType() {
        return type.type;
    }
    public INFOTYPE getInfoType() {
        return type;
    }
    
    /**
     * @param toolset is used to check if this InfoField is active for the supplied argument
     * @return true if it is, otherwise false
     */
    public boolean isValidForToolset(EnumToolset  toolset) {
        for (int category = 0; category < toolsets.length; category++) {
            if (toolsets[category] == ALL_TOOLSETS || toolsets[category] == toolset.getValue())
                return true;
        }
        return false;
    }
    
    /**
     * Lookup the parameter within the active environment to determine details about the named field
     *  
     * @param infoFieldName is the name of the field to check within the active environment
     * @return null if the field is <b>not</b> an InfoField, otherwise an InfoField object 
     */
    public static InfoField get(String infoFieldName) {

        /*String sql = "SELECT ins_or_tran, type_name,toolset.toolset_id,"
                + " tft.type_id, data_type, default_value, required_flag,  max(last_update) last_updated "
                + " FROM tran_info_types tft"
                + " JOIN tran_field_toolsets toolset ON toolset.type_id=tft.type_id "
                + " WHERE type_name ='" + infoFieldName + "' "
                + " GROUP BY ins_or_tran, type_name, toolset.toolset_id, data_type, tft.type_id, default_value, required_flag";
        */
        String sql = "SELECT ins_or_tran, type_name,toolset.toolset_id,"  
        		+ " tft.type_id, data_type, default_value, required_flag,  max(last_update) last_updated "  
        		+ "\nFROM tran_info_types tft"  
        		+ "\nJOIN tran_field_toolsets toolset ON toolset.type_id=tft.type_id " 
        		+ "\nWHERE type_name ='" + infoFieldName +"' " 
        		+ "\nGROUP BY ins_or_tran, type_name, toolset.toolset_id, data_type, tft.type_id, default_value, required_flag"  
        		+ "\nUNION ALL"  
        		+ "\nSELECT 42 as stlinfo, type_name, tft.doc_type," 
        		+ " tft.type_id, tft.data_type, tft.default_value, tft.required_flag,  max(tft.last_update) last_updated " 
        		+ "\nFROM stldoc_info_types tft"  
        		+ "\nWHERE type_name ='" + infoFieldName +"' " 
        		+ "\nGROUP BY  type_name, doc_type, data_type, type_id, default_value, required_flag"; 
       
        
        InfoField field = null;
        Table infoData = null;
        try {
            infoData = DataAccess.getDataFromTable(Application.getInstance().getCurrentSession(), sql);
            
/*            if (ODateTime.strToDateTime(lastUpdated).getDate() <= infoData.getDateTime("last_updated", 1).getDate() 
                    && ODateTime.strToDateTime(lastUpdated).getTime() < infoData.getDateTime("last_updated", 1).getTime()) {
                //TODO update in-memory values
            }
*/          
            if (infoData.getRowCount()>1) {
                
                int toolsets[] = new int[infoData.getRowCount()];
                
                for (int infoFieldItem = 0; infoFieldItem < infoData.getRowCount(); infoFieldItem++) {
                    toolsets[infoFieldItem] = infoData.getInt("toolset_id", infoFieldItem);
                    Logger.log(com.openlink.endur.utilities.logger.LogLevel.DEBUG, LogCategory.General, InfoField.class, String.format("Field >%s< is valid for Toolset:%d",infoFieldName,toolsets[infoFieldItem]));
                }
                field = new InfoField(infoData.getString("type_name", 0), toolsets, infoData.getInt("type_id", 0), INFOTYPE.getFromInt(infoData.getInt("ins_or_tran", 0)), infoData.getDate("last_updated", 0));
                
            } else if (infoData.getRowCount() == 1 ) 
                field = new InfoField(infoData.getString("type_name", 0), new int[]{infoData.getInt("toolset_id", 0)}, infoData.getInt("type_id", 0), INFOTYPE.getFromInt(infoData.getInt("ins_or_tran", 0)), infoData.getDate("last_updated", 0));

            infoData.dispose();
            
        } catch (Exception e) {
            Logger.log(com.openlink.endur.utilities.logger.LogLevel.ERROR, LogCategory.General, InfoField.class, "Field>" + infoFieldName + "< ecountered unexpected failure:"  + e.getLocalizedMessage(), e);
            e.printStackTrace();
            throw new InfoFieldException("Field>" + infoFieldName + "< ecountered unexpected failure:" + e.getLocalizedMessage(), e);
            
        } finally {
            try {
                infoData.dispose();
                
            } catch (Exception e) {
                Logger.log(com.openlink.endur.utilities.logger.LogLevel.ERROR, LogCategory.General, InfoField.class, "Unexpected problem disposing work table:" + e.getLocalizedMessage(), e);
            }
        }
        
        Logger.log(com.openlink.endur.utilities.logger.LogLevel.DEBUG, LogCategory.General, InfoField.class, String.format("Field >%s< is %s InfoField",infoFieldName,null == field ? "NOT valid" : "valid"));
        return field;
    }
    
    
    
    
      
}
