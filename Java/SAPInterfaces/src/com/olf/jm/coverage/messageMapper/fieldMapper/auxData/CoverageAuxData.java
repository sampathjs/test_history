package com.olf.jm.coverage.messageMapper.fieldMapper.auxData;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.enums.EnumAuxDataTables;
import com.olf.jm.SapInterface.businessObjects.enums.EnumCommentColumns;
import com.olf.jm.SapInterface.businessObjects.enums.EnumCommentTypes;
import com.olf.jm.SapInterface.businessObjects.enums.EnumCommentsAuxSubTables;
import com.olf.jm.SapInterface.messageMapper.IAuxDataMapper;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;


/**
 * The Class CoverageAuxData. Add the aux data table into the tradebuilder message
 * containing the comment information.
 */
public class CoverageAuxData implements IAuxDataMapper {
	
	/** The context the script is running in. */
	private Context context;
	
	/** The source data from the input message. */
	private Table source;
	
	
	/**
	 * Instantiates a new coverage aux data.
	 *
	 * @param currentContext the current context
	 * @param currentSource the input message data
	 */
	public CoverageAuxData(final Context currentContext, final Table currentSource) {
		context = currentContext;
		
		source = currentSource;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.IAuxDataMapper#buildAuxDataTable()
	 */
	@Override
	public final Table buildAuxDataTable() {
		// Create a default auxData Table
		TableFactory tableFactory = context.getTableFactory();
		
		Table auxData = tableFactory.createTable(EnumAuxDataTables.COVERAGE_AUX_DATA.getTableName());

		auxData.addColumn(EnumCommentsAuxSubTables.COMMENTS.getTableName(), EnumColType.Table);
		
		auxData.addRow();
		
		auxData.setTable(EnumCommentsAuxSubTables.COMMENTS.getTableName(), 0, buildCommentsTable());
		
		return auxData;

	}
	
	/**
	 * Builds the comments table.
	 *
	 * @return the table
	 */
	private Table buildCommentsTable() {
		TableFactory tableFactory = context.getTableFactory();
		Table comments = tableFactory.createTable(EnumCommentsAuxSubTables.COMMENTS.getTableName());
		
		comments.addColumn(EnumCommentColumns.COMMENT_TEXT.getColumnName(), EnumCommentColumns.COMMENT_TEXT.getColumnType());
		comments.addColumn(EnumCommentColumns.COMMENT_TYPE.getColumnName(), EnumCommentColumns.COMMENT_TYPE.getColumnType());
		
		return comments;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.IAuxDataMapper#setAuxData(com.olf.openrisk.table.Table)
	 */
	@Override
	public final void setAuxData(final Table auxData) {
		
		String columnNames = source.getColumnNames();
		
		if (columnNames.contains(EnumSapCoverageRequest.COMMENT_TEXT.getColumnName())) {
			Table comments = auxData.getTable(EnumCommentsAuxSubTables.COMMENTS.getTableName(), 0);

			int newRow = comments.addRows(1);
			
			comments.setString(EnumCommentColumns.COMMENT_TYPE.getColumnName(), newRow, EnumCommentTypes.COVERAGE.getType());
			
			String comment = source.getString(EnumSapCoverageRequest.COMMENT_TEXT.getColumnName(), 0);
			comments.setString(EnumCommentColumns.COMMENT_TEXT.getColumnName(), newRow, comment);
			
		}
	}

}
