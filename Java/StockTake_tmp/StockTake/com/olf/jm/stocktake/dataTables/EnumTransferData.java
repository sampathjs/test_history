package com.olf.jm.stocktake.dataTables;

import com.olf.openrisk.table.EnumColType;

/**
 * The Enum EnumTransferData. Enum representing the columns in the table used
 * to transfer data from the pre to the post.
 */
public enum EnumTransferData implements ITableColumn {
	
    /** Column containing the adjustment id. */
    ADJUSTMENT_ID("adjustment_id", EnumColType.Int), 
    
    /** Column containing the delta amount. */
    DELTA("delta", EnumColType.Double),    
    
    /** Column containing the nomination location. */
    LOCATION("location", EnumColType.String),    
    
    /** Column containing the nomination product. */
    PRODUCT("product", EnumColType.String),
    
    /** Column containing the nomination purity. */
    PURITY("purity", EnumColType.String),  
    
    /** Column containing the nomination form. */
    FORM("form", EnumColType.String),
    
    /** Column containing the nomination brand. */
    BRAND("brand", EnumColType.String),
    
    /** Column containing the nomination batch id. */
    BATCH_ID("batch_id", EnumColType.String),
    
    /** Column containing the nomination container id. */
    CONTAINER_ID("container_id", EnumColType.String),
    
    /** Column containing the nomination initial weight. */
    INITIAL_VOLUME("initial_volume", EnumColType.Double),
    
    /** Column containing the status of the adjustment. */
    STATUS("status", EnumColType.String),
    
    /** Column containing the default settlement instruction. */
    DEFAULT_SI("default_si", EnumColType.Table),
    
    /** Column containing the sequence number. */
    SEQ_NUMBER("seq_num", EnumColType.Int);
    
    /** The column name. */
    private String columnName;
    
    /** The column type. */
    private EnumColType columnType;
    

    /**
     * Instantiates a new enum transfer data.
     *
     * @param name the column name
     * @param type the column type
     */
    EnumTransferData(final String name, final EnumColType type) {
        this.columnName = name;
        this.columnType = type;
    }

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public EnumColType getColumnType() {
		return columnType;
	}

}
