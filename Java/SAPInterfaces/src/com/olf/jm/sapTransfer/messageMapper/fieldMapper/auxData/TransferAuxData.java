package com.olf.jm.sapTransfer.messageMapper.fieldMapper.auxData;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.enums.EnumAuxDataTables;
import com.olf.jm.SapInterface.businessObjects.enums.EnumCommentColumns;
import com.olf.jm.SapInterface.businessObjects.enums.EnumCommentTypes;
import com.olf.jm.SapInterface.messageMapper.IAuxDataMapper;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumTransferAuxSubTables;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumTransferComment;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;

/**
 * The Class TransferAuxData.
 */
public class TransferAuxData implements IAuxDataMapper {
	
	/** The context the script is running in. */
	private Context context;
	
	/** The source ( input message). */
	private Table source;
	
	
	/**
	 * Instantiates a new transfer aux data.
	 *
	 * @param currentContext the current context
	 * @param currentSource the current source
	 */
	public TransferAuxData(final Context currentContext, final Table currentSource) {
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
		
		Table auxData = tableFactory.createTable(EnumAuxDataTables.TRANSFER_AUX_DATA.getTableName());

		auxData.addColumn(EnumTransferAuxSubTables.COMMENTS.getTableName(), EnumColType.Table);
		
		auxData.addRow();
		
		auxData.setTable(EnumTransferAuxSubTables.COMMENTS.getTableName(), 0, buildCommentsTable());
		
		return auxData;

	}
	
	/**
	 * Builds the comments table.
	 *
	 * @return the table
	 */
	private Table buildCommentsTable() {
		TableFactory tableFactory = context.getTableFactory();
		Table comments = tableFactory.createTable(EnumTransferAuxSubTables.COMMENTS.getTableName());
		
		comments.addColumn(EnumCommentColumns.COMMENT_TEXT.getColumnName(), EnumCommentColumns.COMMENT_TEXT.getColumnType());
		comments.addColumn(EnumCommentColumns.COMMENT_TYPE.getColumnName(), EnumCommentColumns.COMMENT_TYPE.getColumnType());
		
		return comments;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.IAuxDataMapper#setAuxData(com.olf.openrisk.table.Table)
	 */
	@Override
	public final void setAuxData(final Table auxData) {

		Table comments = auxData.getTable(EnumTransferAuxSubTables.COMMENTS.getTableName(), 0);

		String columnNames = source.getColumnNames();
		String sapTransferComment =  "";
		
		if (columnNames.contains(EnumSapTransferRequest.COMMENT_TEXT.getColumnName())) {
			int newRow = comments.addRows(1);		
			comments.setString(EnumCommentColumns.COMMENT_TYPE.getColumnName(), newRow, EnumCommentTypes.SAP_TRANSFER.getType());
			sapTransferComment  = source.getString(EnumSapTransferRequest.COMMENT_TEXT.getColumnName(), 0);
			comments.setString(EnumCommentColumns.COMMENT_TEXT.getColumnName(), newRow, sapTransferComment);
		}
		
		if (columnNames.contains(EnumSapTransferRequest.THIRD_PARTY_INSTRUCTIONS_TEXT.getColumnName())) {
			int newRow = comments.addRows(1);			
			comments.setString(EnumCommentColumns.COMMENT_TYPE.getColumnName(), newRow, EnumCommentTypes.SAP_TRANSFER_3RD_PARTY.getType());
			String comment = source.getString(EnumSapTransferRequest.THIRD_PARTY_INSTRUCTIONS_TEXT.getColumnName(), 0);
			comments.setString(EnumCommentColumns.COMMENT_TEXT.getColumnName(), newRow, comment);		
		}

		if (columnNames.contains(EnumSapTransferRequest.THIRD_PARTY_REFERENCE_TEXT.getColumnName())) {
			int newRow = comments.addRows(1);		
			comments.setString(
					EnumCommentColumns.COMMENT_TYPE.getColumnName(), 
					newRow, 
					EnumCommentTypes.SAP_TRANSFER_3RD_PARTY_REF.getType());
			String comment = source.getString(EnumSapTransferRequest.THIRD_PARTY_REFERENCE_TEXT.getColumnName(), 0);
			comments.setString(EnumCommentColumns.COMMENT_TEXT.getColumnName(), newRow, comment);			
		}
		if(auxData.isValidColumn("to_acc") && auxData.isValidColumn("from_acc")){
			int newRow = comments.addRows(1);
			comments.setString(
					EnumCommentColumns.COMMENT_TYPE.getColumnName(), 
					newRow, 
					EnumTransferComment.FROM_ACCOUNT.getType());
			String comment = "TO " + auxData.getString("to_acc", 0);
			comment = comment + System.lineSeparator() + sapTransferComment;
			comments.setString(EnumCommentColumns.COMMENT_TEXT.getColumnName(), newRow, comment);	
			newRow = comments.addRows(1);
			comments.setString(
					EnumCommentColumns.COMMENT_TYPE.getColumnName(), 
					newRow, 
					EnumTransferComment.TO_ACCOUNT.getType());
			comment = "EX " + auxData.getString("from_acc", 0);
			comment = comment + System.lineSeparator() + sapTransferComment;
			comments.setString(EnumCommentColumns.COMMENT_TEXT.getColumnName(), newRow, comment);
		}
		
		
		
		
			
	}

}
