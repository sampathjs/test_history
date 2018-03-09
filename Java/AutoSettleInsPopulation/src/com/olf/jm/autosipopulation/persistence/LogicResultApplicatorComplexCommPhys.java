package com.olf.jm.autosipopulation.persistence;

import java.util.Collection;
import java.util.List;

import com.olf.jm.autosipopulation.model.DecisionData;
import com.olf.jm.autosipopulation.model.EnumClientDataCol;
import com.olf.jm.autosipopulation.model.EnumRunMode;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.Transaction;

/*
 * History:
 * 2015-09-29	V1.0	jwaechter	- Initial version
 * 2016-06-01	V1.1	jwaechter	- Added logic to differentiate based on offset tran type
 */

/**
 * Specialization of the {@link LogicResultApplicator} that is being used for
 * COMM-PHYS deals.
 * This class is applying the standard logic for all settlement instructions not being
 * associated to a metal currency, but just passes on the the users choice to the post process
 * mode in case it is running as PreProcess script.
 * @author jwaechter
 * @version 1.1
 */
public class LogicResultApplicatorComplexCommPhys extends LogicResultApplicator {
	protected final Table clientData;
	protected final List<Integer> preciousMetalList;

	public LogicResultApplicatorComplexCommPhys(final Transaction tran,
			final Session session, final Collection<LogicResult> results, 
			final EnumRunMode rmode, final Table clientData,
			final List<Integer> preciousMetalList) {
		super(tran, session, results, rmode);
		this.clientData = clientData;
		this.preciousMetalList = preciousMetalList;
		if (!checkClientDataStructure()) {
			setupClientDataStructure ();
		}
	}
	
	@Override
	protected boolean confirmNoExternal(LogicResult result) {
		if (super.rmode == EnumRunMode.PRE) {
			DecisionData dd = result.getDd();
			if (preciousMetalList.contains(dd.getCcyId())) {
				TableRow row = clientData.addRow();
				clientData.setString(EnumClientDataCol.OFFSET_TRAN_TYPE.getColName(), row.getNumber(), dd.getOffsetTranType());
				clientData.setInt(EnumClientDataCol.TRANNUM.getColName(), row.getNumber(), dd.getTranNum());
				clientData.setInt(EnumClientDataCol.LEG_NUM.getColName(), row.getNumber(), dd.getLegNum());
				clientData.setInt(EnumClientDataCol.CCY_ID.getColName(), row.getNumber(), dd.getCcyId());
				clientData.setInt(EnumClientDataCol.INT_EXT.getColName(), row.getNumber(), LogicResultApplicator.InternalExternal.EXTERNAL.getId());
				clientData.setInt(EnumClientDataCol.BUSINESS_UNIT_ID.getColName(), row.getNumber(),
						dd.getExtPartyId());
				clientData.setInt(EnumClientDataCol.SETTLE_ID.getColName(), row.getNumber(), 
						0);
				return super.confirmNoExternal(result);
			} else {
				return super.confirmNoExternal(result);
			}
		} else {

		}
		return false;
	}
	
	@Override
	protected boolean confirmNoInternal(LogicResult result) {
		if (super.rmode == EnumRunMode.PRE) {
			DecisionData dd = result.getDd();
			if (preciousMetalList.contains(dd.getCcyId())) {
				TableRow row = clientData.addRow();
				clientData.setString(EnumClientDataCol.OFFSET_TRAN_TYPE.getColName(), row.getNumber(), dd.getOffsetTranType());
				clientData.setInt(EnumClientDataCol.TRANNUM.getColName(), row.getNumber(), dd.getTranNum());
				clientData.setInt(EnumClientDataCol.LEG_NUM.getColName(), row.getNumber(), dd.getLegNum());
				clientData.setInt(EnumClientDataCol.CCY_ID.getColName(), row.getNumber(), dd.getCcyId());
				clientData.setInt(EnumClientDataCol.INT_EXT.getColName(), row.getNumber(), LogicResultApplicator.InternalExternal.INTERNAL.getId());
				clientData.setInt(EnumClientDataCol.BUSINESS_UNIT_ID.getColName(), row.getNumber(),
						dd.getExtPartyId()); // External!
				clientData.setInt(EnumClientDataCol.SETTLE_ID.getColName(), row.getNumber(), 
						0);
				return super.confirmNoInternal(result);
			} else {
				return super.confirmNoInternal(result);
			}
		} else {

		}
		return false;
	}
	
	@Override
	protected boolean confirmBoth(LogicResult result) {
		if (super.rmode == EnumRunMode.PRE) {
			DecisionData dd = result.getDd();
			if (preciousMetalList.contains(dd.getCcyId())) {
				TableRow row = clientData.addRow();
				clientData.setString(EnumClientDataCol.OFFSET_TRAN_TYPE.getColName(), row.getNumber(), dd.getOffsetTranType());
				clientData.setInt(EnumClientDataCol.TRANNUM.getColName(), row.getNumber(), dd.getTranNum());
				clientData.setInt(EnumClientDataCol.LEG_NUM.getColName(), row.getNumber(), dd.getLegNum());
				clientData.setInt(EnumClientDataCol.CCY_ID.getColName(), row.getNumber(), dd.getCcyId());
				clientData.setInt(EnumClientDataCol.INT_EXT.getColName(), row.getNumber(), LogicResultApplicator.InternalExternal.INTERNAL.getId());
				clientData.setInt(EnumClientDataCol.BUSINESS_UNIT_ID.getColName(), row.getNumber(),
						dd.getExtPartyId()); // External!
				clientData.setInt(EnumClientDataCol.SETTLE_ID.getColName(), row.getNumber(), 
						0);

				row = clientData.addRow();
				clientData.setString(EnumClientDataCol.OFFSET_TRAN_TYPE.getColName(), row.getNumber(), dd.getOffsetTranType());
				clientData.setInt(EnumClientDataCol.TRANNUM.getColName(), row.getNumber(), dd.getTranNum());
				clientData.setInt(EnumClientDataCol.LEG_NUM.getColName(), row.getNumber(), dd.getLegNum());
				clientData.setInt(EnumClientDataCol.CCY_ID.getColName(), row.getNumber(), dd.getCcyId());
				clientData.setInt(EnumClientDataCol.INT_EXT.getColName(), row.getNumber(), LogicResultApplicator.InternalExternal.EXTERNAL.getId());
				clientData.setInt(EnumClientDataCol.BUSINESS_UNIT_ID.getColName(), row.getNumber(),
						dd.getExtPartyId());
				clientData.setInt(EnumClientDataCol.SETTLE_ID.getColName(), row.getNumber(), 
						0);
				
				return super.confirmBoth(result);
			} else {
				return super.confirmBoth(result);
			}
		} else {

		}
		return false;
	}

	@Override
	protected void setExternal(LogicResult result, int extSettleId) {
		if (super.rmode == EnumRunMode.PRE) {
			DecisionData dd = result.getDd();
			if (preciousMetalList.contains(dd.getCcyId())) {
				TableRow row = clientData.addRow();
				clientData.setString(EnumClientDataCol.OFFSET_TRAN_TYPE.getColName(), row.getNumber(), dd.getOffsetTranType());
				clientData.setInt(EnumClientDataCol.TRANNUM.getColName(), row.getNumber(), dd.getTranNum());
				clientData.setInt(EnumClientDataCol.LEG_NUM.getColName(), row.getNumber(), dd.getLegNum());
				clientData.setInt(EnumClientDataCol.CCY_ID.getColName(), row.getNumber(), dd.getCcyId());
				clientData.setInt(EnumClientDataCol.INT_EXT.getColName(), row.getNumber(), LogicResultApplicator.InternalExternal.EXTERNAL.getId());
				clientData.setInt(EnumClientDataCol.BUSINESS_UNIT_ID.getColName(), row.getNumber(),
						dd.getExtPartyId());
				clientData.setInt(EnumClientDataCol.SETTLE_ID.getColName(), row.getNumber(), 
						extSettleId);
			} else {
				super.setExternal(result, extSettleId);
			}
		} else {

		}
	}

	@Override
	protected void setInternal(LogicResult result, int intSettleId) {
		DecisionData dd = result.getDd();
		if (super.rmode == EnumRunMode.PRE) {
			if (preciousMetalList.contains(dd.getCcyId())) {
				TableRow row = clientData.addRow();
				clientData.setString(EnumClientDataCol.OFFSET_TRAN_TYPE.getColName(), row.getNumber(), dd.getOffsetTranType());
				clientData.setInt(EnumClientDataCol.TRANNUM.getColName(), row.getNumber(), dd.getTranNum());
				clientData.setInt(EnumClientDataCol.LEG_NUM.getColName(), row.getNumber(), dd.getLegNum());
				clientData.setInt(EnumClientDataCol.CCY_ID.getColName(), row.getNumber(), dd.getCcyId());
				clientData.setInt(EnumClientDataCol.INT_EXT.getColName(), row.getNumber(), LogicResultApplicator.InternalExternal.INTERNAL.getId());
				clientData.setInt(EnumClientDataCol.BUSINESS_UNIT_ID.getColName(), row.getNumber(),
						dd.getExtPartyId()); // External
				clientData.setInt(EnumClientDataCol.SETTLE_ID.getColName(), row.getNumber(), 
						intSettleId);
			} else {
				super.setInternal(result, intSettleId);
			}
		} else {
			
		}
	}

	protected boolean checkClientDataStructure () {
		for (EnumClientDataCol col : EnumClientDataCol.values()) {
			if (!clientData.isValidColumn(col.getColName()) || 
					clientData.getColumnType(clientData.getColumnId(col.getColName())) != col.getColType()) {
				return false;
			}
		}
		return true;
	}

	protected void setupClientDataStructure() {
		for (EnumClientDataCol col : EnumClientDataCol.values()) {
			if (!clientData.isValidColumn(col.getColName()) || 
					clientData.getColumnType(clientData.getColumnId(col.getColName())) != col.getColType()) {
				clientData.addColumn(col.getColName(), col.getColType());
			}
		}
	}
}
