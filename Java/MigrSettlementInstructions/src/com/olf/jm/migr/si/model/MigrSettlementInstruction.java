package com.olf.jm.migr.si.model;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial version
 * 2016-04-10	V1.1	jwaechter	- instead of throwing an in exception in case of
 *                                    multiple retrieved rows in method "retrieveSettlementInstruction"
 *                                    the warning is now logged
 * 2016-05-04	V1.2	jwaechter	- added handling of multiple matches in user table as alternatives
 *                                    
 */


/**
 * 
 * @author jwaechter
 * @version 1.2
 */
public class MigrSettlementInstruction {
	private final MigrSettlementInstructionInputCols colConfig;
	private final String oldTranId;
	private String settlementInstructionName=null;
	private String businessUnit;
	private String currency;
	private String isInternal;
	private String deliveryType;
	private final Session session;
	private final int rowMatchNumber;
	private MigrSettlementInstruction nextSi=null;
	
	public MigrSettlementInstruction (final Session session, final MigrSettlementInstructionInputCols colConfig,
			final String oldTranId) {
		this.colConfig = colConfig;
		this.oldTranId = oldTranId;
		this.session = session;
		rowMatchNumber = 0;
	}

	public MigrSettlementInstruction (final Session session, final MigrSettlementInstructionInputCols colConfig,
			final String oldTranId, final int rowMatchNumber) {
		this.colConfig = colConfig;
		this.oldTranId = oldTranId;
		this.session = session;
		this.rowMatchNumber = rowMatchNumber;
	}

	
	public String getSettlementInstructionName () {
		if (settlementInstructionName == null) {
			retrieveSettlementInstruction();
		}
		return settlementInstructionName;
	}

	public String getBusinessUnit () {
		if (businessUnit == null) {
			retrieveSettlementInstruction();
		}
		return businessUnit;
	}
	
	public String getCurrency () {
		if (currency == null) {
			retrieveSettlementInstruction();
		}
		return currency;
	}
	
	public String getDeliveryType () {
		if (deliveryType == null) {
			retrieveSettlementInstruction();
		}
		return deliveryType;
	}
	
	public String getIsInternal () {
		if (isInternal == null) {
			retrieveSettlementInstruction();
		}
		return isInternal;
	}
	
	public boolean hasNextSi () {
		return nextSi != null;
	}
	
	public MigrSettlementInstruction getNextSi () {
		return nextSi;
	}
	
	private void retrieveSettlementInstruction() {
		String sql = colConfig.generateSql(oldTranId);
		Table sqlResult = session.getIOFactory().runSQL(sql);
		if (sqlResult.getRowCount() == 0) {
			throw new Warning ("Could not retrieve settlement instruction data from " + colConfig.getTableName()
					+ " for old tran id " + oldTranId);
		}
		if (sqlResult.getRowCount() > 1) {
			PluginLog.warn("Multiple rows in " + colConfig.getTableName()
					+ " found for for old tran id " + oldTranId);
			if (rowMatchNumber+1 < sqlResult.getRowCount() ) {
				nextSi = new MigrSettlementInstruction(session, colConfig, oldTranId, rowMatchNumber+1);				
			}
		}
		currency = sqlResult.getString("ccy", rowMatchNumber);
		businessUnit = sqlResult.getString("bu", rowMatchNumber);
		isInternal = sqlResult.getString("is_internal", rowMatchNumber);
		deliveryType = sqlResult.getString("dt", rowMatchNumber);
		settlementInstructionName = sqlResult.getString("si", rowMatchNumber);
		sqlResult.dispose();
	}
	
	public MigrSettlementInstructionInputCols getColConfig() {
		return colConfig;
	}

	public String getOldTranId() {
		return oldTranId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((colConfig == null) ? 0 : colConfig.hashCode());
		result = prime * result
				+ ((oldTranId == null) ? 0 : oldTranId.hashCode());
		result = prime * result + rowMatchNumber;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MigrSettlementInstruction other = (MigrSettlementInstruction) obj;
		if (colConfig == null) {
			if (other.colConfig != null)
				return false;
		} else if (!colConfig.equals(other.colConfig))
			return false;
		if (oldTranId == null) {
			if (other.oldTranId != null)
				return false;
		} else if (!oldTranId.equals(other.oldTranId))
			return false;
		if (rowMatchNumber != other.rowMatchNumber)
			return false;
		return true;
	}
}
