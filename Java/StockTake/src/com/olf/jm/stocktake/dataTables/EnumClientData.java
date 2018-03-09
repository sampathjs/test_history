package com.olf.jm.stocktake.dataTables;

import com.olf.openrisk.table.EnumColType;


/**
 * The Enum EnumClientData. Enum representing the columns on the client data table
 */
public enum EnumClientData implements ITableColumn {
	
    /** The stocktake transfer data. */
    STOCKTAKE_TRANSFER_DATA("stocktake_transfer_data", EnumColType.Table);   
    
    /** The column name. */
    private String columnName;
    
    /** The column type. */
    private EnumColType columnType;
    

    /**
     * Instantiates a new enum client data.
     *
     * @param name the column name
     * @param type the column type
     */
    EnumClientData(final String name, final EnumColType type) {
        this.columnName = name;
        this.columnType = type;
    }

	/* (non-Javadoc)
	 * @see com.olf.jm.stocktake.dataTables.ITableColumn#getColumnName()
	 */
	@Override
	public String getColumnName() {
		return columnName;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.stocktake.dataTables.ITableColumn#getColumnType()
	 */
	@Override
	public EnumColType getColumnType() {
		return columnType;
	}

}
