/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.items.tables;

import com.olf.embedded.application.Context;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.table.ColumnFormatterAsDateTime;
import com.olf.openrisk.table.ColumnFormatterAsDouble;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumFormatDateTime;
import com.olf.openrisk.table.EnumFormatDouble;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFormatter;
import com.openlink.util.constrepository.ConstRepository;

import java.util.ArrayList;
import java.util.Arrays;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 * 2018-01-29 - V0.2 - scurran - Update table formatting
 */

/**
 * The Class TableColumnHelper.
 *
 * @param <T> the generic type Enum class representing the table columns, must implement the interface TableColumn.
 */
public class TableColumnHelper<T extends  Enum<T>> {
	
	public final static int TOZ_DECIMAL_PLACES = 4;
	public final static int GMS_DECIMAL_PLACES = 2;
	
	private final double toleranceThreshold;
	
	public TableColumnHelper() {
		try {
			ConstRepository constRep = new ConstRepository("Warehouse", "ContainerWeightConverter");

			String value = constRep.getStringValue("zeroToleranceLevel", "0.01");
			
			toleranceThreshold = Double.parseDouble(value);

		} catch (Exception e) {
			Logging.error("Error reading the tolerance threshold." + e.getMessage());
			throw new RuntimeException("Error reading the tolerance threshold." + e.getMessage());
		}
	}
	/**
	 * Format table for output. Loops over all the table columns converting double and date columns to string applying 
	 * column formatters to these columns.
	 *
	 * @param columnEnumeration the column enumeration
	 * @param tableToFormat the table to format
	 * @return the formatted table 
	 */
	public Table formatTableForOutput(Class<T> columnEnumeration, Table tableToFormat ) {
		TableFormatter tableFormatter = tableToFormat.getFormatter();
		
		ColumnFormatterAsDouble columnFormatterDouble2dp = tableFormatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, GMS_DECIMAL_PLACES, GMS_DECIMAL_PLACES);
		
		ColumnFormatterAsDouble columnFormatterDouble3dp = tableFormatter.createColumnFormatterAsDouble(EnumFormatDouble.Notnl, TOZ_DECIMAL_PLACES, TOZ_DECIMAL_PLACES);
				
		ColumnFormatterAsDateTime columnFormatterDate = tableFormatter.createColumnFormatterAsDateTime(EnumFormatDateTime.Date);
			
		for(T column :  columnEnumeration.getEnumConstants()) {
			TableColumn tableColumn = (TableColumn) column;
			if(tableColumn.getFormatType() == EnumFormatType.FMT_2DP) {
				tableFormatter.setColumnFormatter(tableColumn.getColumnName(), columnFormatterDouble2dp);
			} else if(tableColumn.getFormatType() == EnumFormatType.FMT_DATE ) {
				tableFormatter.setColumnFormatter(tableColumn.getColumnName(), columnFormatterDate);
			} else if(tableColumn.getFormatType() == EnumFormatType.FMT_3DP ) {
				tableFormatter.setColumnFormatter(tableColumn.getColumnName(), columnFormatterDouble3dp);
			}
			
			if(tableColumn.applyToleranceCheck()) {
				for(int row = 0; row < tableToFormat.getRowCount(); row++) {
					double valueToCheck = tableToFormat.getDouble(tableColumn.getColumnName(), row);
					
					if(valueToCheck <= toleranceThreshold && valueToCheck >= (toleranceThreshold * -1.0) ) {
						tableToFormat.setDouble(tableColumn.getColumnName(), row, 0.0);
					}
				}
			}
		}
		
		String[] columnNames = this.getColumnNamesToFormat(columnEnumeration);
		EnumColType[] columnTypes = new EnumColType[columnNames.length];
		
		Arrays.fill(columnTypes, EnumColType.String);
		
		tableToFormat.convertColumns(columnNames, columnTypes);

		return tableToFormat;		
	}

	/**
	 * Gets the column names as an array.
	 *
	 * @param columnEnumeration the column enumeration
	 * @return the column names
	 */
	public String[] getColumnNames(Class<T> columnEnumeration ) {
		
		ArrayList<String> columnName = new ArrayList<>();
		
		for(Enum<T> column : columnEnumeration.getEnumConstants() ) {
			columnName.add(((TableColumn)column).getColumnName());
		}
		return columnName.toArray(new String[0]);
		
	}
	
	/**
	 * Gets the column names to format. Return an array of double and date columns
	 *
	 * @param columnEnumeration the column enumeration
	 * @return the column names to format
	 */
	public String[] getColumnNamesToFormat(Class<T> columnEnumeration ) {
		
		ArrayList<String> columnName = new ArrayList<>();
		
		for(Enum<T> column : columnEnumeration.getEnumConstants() ) {
			if(((TableColumn)column).getColumnType() == EnumColType.Double || 
			  ((TableColumn)column).getColumnType() == EnumColType.DateTime ||
			  ((TableColumn)column).getColumnType() == EnumColType.Date) {
			columnName.add(((TableColumn)column).getColumnName());
			}
		}
		return columnName.toArray(new String[0]);
		
	}	
	
	/**
	 * Gets the column types.
	 *
	 * @param columnEnumeration the column enumeration
	 * @return the column types
	 */
	public EnumColType[] getColumnTypes(Class<T> columnEnumeration ) {
		
		ArrayList<EnumColType> columnType = new ArrayList<>();
		
		for(Enum<T> column : columnEnumeration.getEnumConstants() ) {
			columnType.add(((TableColumn)column).getColumnType());
		}
		return columnType.toArray(new EnumColType[0]);
		
	}	
	
	/**
	 * Builds the table. Construct an empty table based on the enum
	 *
	 * @param context the script context
	 * @param columnEnumeration the column enumeration to build the table from
	 * @param tableName the table name
	 * @return the constructed table
	 */
	public Table buildTable(Context context, Class<T> columnEnumeration, String tableName ) {
		
		Table returnValue = context.getTableFactory().createTable(tableName);
		
		returnValue.addColumns(this.getColumnNames(columnEnumeration), this.getColumnTypes(columnEnumeration));
		
		return returnValue;
	}
	
	/**
	 * Builds the table. Construct an empty table based on the enum
	 *
	 * @param context the context
	 * @param columnEnumeration the column enumeration
	 * @return the constructed table
	 */
	public Table buildTable(Context context, Class<T> columnEnumeration ) {
		return buildTable(context, columnEnumeration, "" ); 
	}
} 
