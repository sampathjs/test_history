package com.matthey.openlink.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.EnumOlfDebugType;
import com.olf.openrisk.application.EnumUtilDebugType;
import com.olf.openrisk.table.Table;


// TODO: Auto-generated Javadoc
/**
 * The Class TradeFieldSequence.
 * @version $Revision: $
 */
public class TradeFieldSequence implements Comparable<TradeFieldSequence> {


/**
 * The Enum TRANSACTION_TYPE.
 */
public enum TRANSACTION_TYPE {
    
    /** The Trading. */
    Trading("Trading"),
    
    /** The Holding instrument. */
    HoldingInstrument("Holding");
    
    /** The type. */
    private final String type;
    
    /**
     * Instantiates a new transaction type.
     *
     * @param name the name
     */
    private TRANSACTION_TYPE(String name) {
        type = name;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return type;
    }
    
}
    
    /** The fields toolset sequence. */
	private static Map<String, Map<String, List<TradeFieldSequence>>> fieldsToolsetSequence = 
			new HashMap<String, Map<String, List<TradeFieldSequence>>>(0);
    
    /** The name. */
    private final String name;
    
    /** The enabled. */
    private final boolean enabled;
    
    /** The sequence. */
    private final int sequence;
    
    /** The field. */
    private final int field;
    
    
    /**
     * Instantiates a new trade field sequence.
     *
     * @param importName the import name
     * @param active the active
     * @param sequence the sequence
     * @param id the id
     */
    private TradeFieldSequence(String importName, boolean active, int sequence, int id) {
        this.name = importName;
        this.enabled = active;
        this.sequence = sequence;
        this.field = id;
    }
    
    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if is enabled.
     *
     * @return true, if is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the sequence.
     *
     * @return the sequence
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * Gets the field.
     *
     * @return the field
     */
    public int getField() {
        return field;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + field;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + sequence;
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TradeFieldSequence other = (TradeFieldSequence) obj;
        if (enabled != other.enabled)
            return false;
        if (field != other.field)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (sequence != other.sequence)
            return false;
        return true;
    }

    
    /**
     * Gets the sequence.
     *
     * @param type the type
     * @param toolset the toolset
     * @param field the field
     * @return the sequence
     */
    public static int getSequence(TRANSACTION_TYPE type, String toolset, String field) {
        if (fieldsToolsetSequence.isEmpty())
            populateConnexToolsetIO();
        
        if (fieldsToolsetSequence.containsKey(type.toString()))
                return getSequence(fieldsToolsetSequence.get(type.toString()), toolset, field);
        else 
            throw new IllegalArgumentException("Unable to provide sequence for transaction type >" + type.toString() +"<");
    }
    
    /**
     * Gets the sequence.
     *
     * @param connexToolsets the connex toolsets
     * @param toolset the toolset
     * @param field the field
     * @return the sequence
     */
    private static int getSequence(Map<String, List<TradeFieldSequence>> connexToolsets,String toolset, String field) {
        if (fieldsToolsetSequence.isEmpty())
            populateConnexToolsetIO();
        
        if (connexToolsets.containsKey(toolset) ) {
            List<TradeFieldSequence> toolsetFields = connexToolsets.get(toolset);
            for (TradeFieldSequence importField : toolsetFields) {
                if (importField.getName().equals(field)) {
                    if (Application.getInstance().getCurrentSession().getDebug().isDebugTypeActive(EnumUtilDebugType.ImpExp))
                        System.out.println("\n Matched " + field + "(" + importField.getField() +") \t with " + importField.getName() +" seq:" + importField.getSequence()+" import enabled:" + importField.isEnabled());
                    return importField.getSequence();
                }
            }
        }
        return -1;
    }
    
    /**
     * Populate connex toolset io.
     */
    static void populateConnexToolsetIO() {

        if (!fieldsToolsetSequence.isEmpty())
            fieldsToolsetSequence.clear();

        for (TRANSACTION_TYPE transaction : TRANSACTION_TYPE.values()) {
            fieldsToolsetSequence.put(transaction.toString(), populateConnexToolsetIO(transaction.toString()));
        }
    }
    
    /**
     * Populate connex toolset io.
     *
     * @param transactionType the transaction type
     * @return the map
     */
    private static Map<String, List<TradeFieldSequence>> populateConnexToolsetIO(String transactionType) {
        
        Map<String, List<TradeFieldSequence>> connexToolsets = new HashMap<String, List<TradeFieldSequence>>(0);
        
        String sourceTable = String.format("tranf%s_import_processing", transactionType.equals("Holding") ? "_ins" : "");
        String sql = "SELECT t.name toolsetName, tip.seq_num sequence, tip.tranf_field_id tranf, tip.orien_name name, tip.openconnect active" 
                + " FROM %s tip " 
                + " JOIN toolsets t ON tip.toolset_id = t.id_number";
       
        Table connexIOData = null;
        try {
            
            connexIOData = DataAccess.getDataFromTable(Application.getInstance().getCurrentSession(), String.format(sql,sourceTable));
            
          if (connexIOData.getRowCount()<1) {
                throw new  RuntimeException("Unable to establish environment IO fields!");
                
            } 
            int numberOfConnexFields = connexIOData.getRowCount();
            for (int field = 0; field < numberOfConnexFields; field++) {
                
                TradeFieldSequence connexField = new TradeFieldSequence(connexIOData.getString("name", field), 
                                                                      connexIOData.getInt("active", field) == 1 , 
                                                                      connexIOData.getInt("sequence", field), 
                                                                      connexIOData.getInt("tranf", field));
                String toolset = connexIOData.getString("toolsetName", field);
                if (connexToolsets.isEmpty() || !connexToolsets.containsKey(toolset)) {
                    connexToolsets.put(toolset, new ArrayList<TradeFieldSequence>(1));
                }
                
                connexToolsets.get(toolset).add(connexField);
            }

                connexIOData.dispose();
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected failure:" + e.getLocalizedMessage(), e);
            
        } finally {
            try {
                connexIOData.dispose();
                
            } catch (Exception e) {
                if (Application.getInstance().getCurrentSession().getDebug().isDebugTypeActive(EnumUtilDebugType.ImpExp) 
                        || Application.getInstance().getCurrentSession().getDebug().isDebugTypeActive(EnumOlfDebugType.Opencomponent) ) {
                    System.out.println("Unexpected problem disposing work table:" + e.getLocalizedMessage());
                }
                e.printStackTrace();
            }
        }

        return connexToolsets;
    }


    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(TradeFieldSequence o) {
        return Integer.valueOf(this.sequence).compareTo(o.sequence);
    }

    /**
     * Gets the field.
     *
     * @param type the type
     * @param toolset the toolset
     * @param field the field
     * @return the field
     */
    public static TradeFieldSequence getField(TRANSACTION_TYPE type, String toolset, String field) {
        if (fieldsToolsetSequence.isEmpty())
            populateConnexToolsetIO();
        
        if (fieldsToolsetSequence.containsKey(type.toString()))
                return getField(fieldsToolsetSequence.get(type.toString()), toolset, field);
        else 
            throw new IllegalArgumentException("Unable to located transactions of type >" + type.toString() +"<");
    }
    
    /**
     * Gets the field.
     *
     * @param connexToolsets the connex toolsets
     * @param toolset the toolset
     * @param field the field
     * @return the field
     */
    private static TradeFieldSequence getField(Map<String, List<TradeFieldSequence>> connexToolsets, String toolset, String field) {        
        if (connexToolsets.isEmpty())
            populateConnexToolsetIO();
        
        if (Application.getInstance().getCurrentSession().getDebug().isDebugTypeActive(EnumUtilDebugType.ImpExp)) {
            System.out.println("\tSearching for field "  + field + "  Toolset:" +toolset);
        }
        if (connexToolsets.containsKey(toolset)) {
            List<TradeFieldSequence> olfToolset = connexToolsets.get(toolset);
            for (TradeFieldSequence importField : olfToolset) {
                if (field.equalsIgnoreCase(importField.getName())) {
                    if (Application.getInstance().getCurrentSession().getDebug().isDebugTypeActive(EnumUtilDebugType.ImpExp)) {
                        System.out.println("\tFOUND field " + field +"\n" );
                    }

                        return importField;
                }
            }
        }
        return new TradeFieldSequence(field, false, 10000, -1);
        //return null;
    }


}
